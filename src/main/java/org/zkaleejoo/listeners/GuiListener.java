package org.zkaleejoo.listeners;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.CompatibilityUtil;

public class GuiListener implements Listener {

    private final MaxStaff plugin;

    public GuiListener(MaxStaff plugin) { 
        this.plugin = plugin; 
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        
        if (event.getClickedInventory() != event.getInventory()) return;

        String rawTitle = CompatibilityUtil.getInventoryTitle(event);
        if (rawTitle == null || rawTitle.isEmpty()) return;
        String title = ChatColor.stripColor(rawTitle).trim();
        
        MainConfigManager config = plugin.getMainConfigManager();

        String infoTitleBase = ChatColor.stripColor(MessageUtils.getColoredMessage(config.getGuiInfoTitle().split("\\{")[0])).trim();
        String playersTitle = ChatColor.stripColor(MessageUtils.getColoredMessage(config.getGuiPlayersTitle())).trim();
        String sanctionsTitle = ChatColor.stripColor(MessageUtils.getColoredMessage(config.getGuiSanctionsTitle().replace("{target}", ""))).trim();
        String reasonsBaseTitle = ChatColor.stripColor(MessageUtils.getColoredMessage(config.getGuiReasonsTitle().split("\\{")[0])).trim();
        String historyTitleBase = ChatColor.stripColor(MessageUtils.getColoredMessage(config.getGuiHistoryTitle().split("\\{")[0])).trim();
        String detailedTitleBase = ChatColor.stripColor(MessageUtils.getColoredMessage(config.getGuiDetailedTitle().split("\\[")[0])).trim();

        boolean isMaxStaffGui = title.startsWith(infoTitleBase) || 
                               title.contains(playersTitle) || 
                               title.contains(sanctionsTitle) || 
                               title.contains(reasonsBaseTitle) || 
                               title.startsWith(historyTitleBase) || 
                               title.startsWith(detailedTitleBase);

        if (!isMaxStaffGui) return;

        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (!item.hasItemMeta()) return;
        
        String itemName = item.getItemMeta().hasDisplayName() ? 
                ChatColor.stripColor(item.getItemMeta().getDisplayName()) : "";

        if (item.getType() == config.getBorderMaterial()) return;


        if (title.startsWith(infoTitleBase)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            String targetName = title.replace(infoTitleBase, "").trim();
            
            if (item.getType() == config.getGuiInfoActionMat()) {
                plugin.getGuiManager().openSanctionMenu(player, targetName);
            } 
            else if (item.getType() == config.getGuiInfoHistoryMat()) {
                if (!player.hasPermission("maxstaff.history")) {
                    sendNoPermission(player); 
                    return;
                }
                plugin.getGuiManager().openHistoryMenu(player, targetName);
            }
        }

        else if (title.contains(playersTitle)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            if (item.getType() == Material.PLAYER_HEAD) {
                Player target = plugin.getServer().getPlayer(itemName);
                if (target != null) {
                    player.teleport(target);
                    player.sendMessage(MessageUtils.getColoredMessage(config.getMsgTeleport().replace("{player}", target.getName())));
                }
                player.closeInventory();
            }
        }

        else if (title.contains(sanctionsTitle)) {
            String targetName = title.replace(sanctionsTitle, "").trim();

            if (item.getType() == config.getNavBackMat()) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                Player targetPlayer = plugin.getServer().getPlayer(targetName);
                if (targetPlayer != null) plugin.getGuiManager().openUserInfoMenu(player, targetPlayer);
                return;
            }

            if (item.getType() == Material.IRON_SWORD) {
                if (checkPerm(player, "maxstaff.punish.ban")) plugin.getGuiManager().openReasonsMenu(player, targetName, "BAN", 0);
            } 
            else if (item.getType() == Material.PAPER) {
                if (checkPerm(player, "maxstaff.punish.mute")) plugin.getGuiManager().openReasonsMenu(player, targetName, "MUTE", 0);
            } 
            else if (item.getType() == Material.FEATHER) {
                if (checkPerm(player, "maxstaff.punish.kick")) plugin.getGuiManager().openReasonsMenu(player, targetName, "KICK", 0);
            }
        }

        else if (title.contains(reasonsBaseTitle)) { 
            String type = title.contains("[BAN]") ? "BAN" : (title.contains("[MUTE]") ? "MUTE" : "KICK");
            String target;
            try { target = title.split(" - ")[1].split(" \\(")[0].trim(); } catch (Exception e) { return; }

            int page;
            try { page = Integer.parseInt(title.split("\\(")[1].split("/")[0]) - 1; } catch (Exception e) { page = 0; }

            if (item.getType() == config.getNavBackMat()) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                plugin.getGuiManager().openSanctionMenu(player, target);
            }
            else if (itemName.contains(ChatColor.stripColor(MessageUtils.getColoredMessage(config.getNavNextName())))) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                plugin.getGuiManager().openReasonsMenu(player, target, type, page + 1);
            }
            else if (itemName.contains(ChatColor.stripColor(MessageUtils.getColoredMessage(config.getNavPrevName())))) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                plugin.getGuiManager().openReasonsMenu(player, target, type, page - 1);
            }
            else if (item.getType().name().endsWith("_DYE") && item.getItemMeta().hasLore()) {
                if (!checkPerm(player, "maxstaff.punish." + type.toLowerCase())) {
                    player.closeInventory();
                    return;
                }
                ItemMeta meta = item.getItemMeta();
                NamespacedKey reasonKey = new NamespacedKey(plugin, "reason_id");
                NamespacedKey durationKey = new NamespacedKey(plugin, "duration");
                
                String reasonId = meta.getPersistentDataContainer().get(reasonKey, PersistentDataType.STRING);
                String duration = meta.getPersistentDataContainer().get(durationKey, PersistentDataType.STRING);
                
                if (reasonId == null || duration == null) return;
                String reasonName = config.getReasonName(type, reasonId);
                
                if (type.equals("BAN")) plugin.getPunishmentManager().banPlayer(player, target, reasonName, duration);
                else if (type.equals("MUTE")) plugin.getPunishmentManager().mutePlayer(player, target, reasonName, duration);
                else if (type.equals("KICK")) plugin.getPunishmentManager().kickPlayer(player, target, reasonName);
                
                player.closeInventory();
            }
        }

        else if (title.startsWith(historyTitleBase)) {
            String targetName = title.replace(historyTitleBase, "").trim();
            String type = "";
            if (item.getType() == Material.RED_WOOL) type = "BAN";
            else if (item.getType() == Material.ORANGE_WOOL) type = "MUTE";
            else if (item.getType() == Material.YELLOW_WOOL) type = "WARN";
            else if (item.getType() == Material.LIGHT_GRAY_WOOL) type = "KICK";

            if (!type.isEmpty()) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                plugin.getGuiManager().openDetailedHistoryMenu(player, targetName, type);
            }
        }

        else if (title.startsWith(detailedTitleBase)) {
            if (item.getType() == config.getNavBackMat()) {
                String targetName = title.split(" - ")[1].trim();
                plugin.getGuiManager().openHistoryMenu(player, targetName);
            }
        }

        String gmTitleBase = ChatColor.stripColor(MessageUtils.getColoredMessage(config.getGuiGmTitle())).trim();

        if (title.equals(gmTitleBase)) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            
            org.bukkit.GameMode mode = null;
            String modeName = "";

            if (item.getType() == config.getGuiGmSurvivalMat()) { mode = org.bukkit.GameMode.SURVIVAL; modeName = "Survival"; }
            else if (item.getType() == config.getGuiGmCreativeMat()) { mode = org.bukkit.GameMode.CREATIVE; modeName = "Creative"; }
            else if (item.getType() == config.getGuiGmAdventureMat()) { mode = org.bukkit.GameMode.ADVENTURE; modeName = "Adventure"; }
            else if (item.getType() == config.getGuiGmSpectatorMat()) { mode = org.bukkit.GameMode.SPECTATOR; modeName = "Spectator"; }

            if (mode != null) {
                player.setGameMode(mode);
                if (plugin.getStaffManager().isInStaffMode(player)) {
                    plugin.getStaffManager().updateSavedGameMode(player, mode);
                }
                player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getGuiGmFeedback().replace("{mode}", modeName)));
                player.closeInventory();
            }
        }
    }

    private boolean checkPerm(Player player, String permission) {
        if (!player.hasPermission(permission)) {
            sendNoPermission(player);
            return false;
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        return true;
    }

    private void sendNoPermission(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        player.sendMessage(MessageUtils.getColoredMessage(
            plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()
        ));
    }
}