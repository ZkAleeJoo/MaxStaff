package org.zkaleejoo.listeners;

import org.bukkit.ChatColor;
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

        Player player = (Player) event.getWhoClicked();
        
        String rawTitle = CompatibilityUtil.getInventoryTitle(event);
        String title = ChatColor.stripColor(rawTitle).trim(); 
        
        String infoTitleBase = ChatColor.stripColor(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getGuiInfoTitle().split("\\{")[0])).trim();
        String playersTitle = ChatColor.stripColor(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getGuiPlayersTitle())).trim();
        String sanctionsTitle = ChatColor.stripColor(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getGuiSanctionsTitle().replace("{target}", ""))).trim();
        String reasonsBaseTitle = ChatColor.stripColor(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getGuiReasonsTitle().split("\\{")[0])).trim();

        boolean isOurGui = title.startsWith(infoTitleBase) || 
                           title.contains(playersTitle) || 
                           title.contains(sanctionsTitle) || 
                           title.contains(reasonsBaseTitle);

        if (!isOurGui) return;

        event.setCancelled(true);

        if (event.getClickedInventory() != CompatibilityUtil.getTopInventory(event)) return;

        ItemStack item = event.getCurrentItem();
        if (!item.hasItemMeta()) return;
        
        String itemName = item.getItemMeta().hasDisplayName() ? 
                ChatColor.stripColor(item.getItemMeta().getDisplayName()) : "";

        if (item.getType() == plugin.getMainConfigManager().getBorderMaterial()) {
            return;
        }

        if (title.startsWith(infoTitleBase)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            String targetName = title.replace(infoTitleBase, "").trim();
            
            if (item.getType() == plugin.getMainConfigManager().getGuiInfoActionMat()) {
                plugin.getGuiManager().openSanctionMenu(player, targetName);
            }
            return;
        }

        if (title.contains(playersTitle)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            if (item.getType() == Material.PLAYER_HEAD) {
                Player target = plugin.getServer().getPlayer(itemName);
                if (target != null) {
                    player.teleport(target);
                    String tpMsg = plugin.getMainConfigManager().getMsgTeleport();
                    player.sendMessage(MessageUtils.getColoredMessage(tpMsg.replace("{player}", target.getName())));
                }
                player.closeInventory();
            }
            return;
        }

        if (title.contains(sanctionsTitle)) {
            String targetName = title.replace(sanctionsTitle, "").trim();

            if (item.getType() == plugin.getMainConfigManager().getNavBackMat()) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                Player targetPlayer = plugin.getServer().getPlayer(targetName);
                if (targetPlayer != null) {
                    plugin.getGuiManager().openUserInfoMenu(player, targetPlayer);
                }
                return;
            }

            if (item.getType() == Material.IRON_SWORD) {
                if (!checkPerm(player, "maxstaff.punish.ban")) return;
                plugin.getGuiManager().openReasonsMenu(player, targetName, "BAN", 0);
            } 
            else if (item.getType() == Material.PAPER) {
                if (!checkPerm(player, "maxstaff.punish.mute")) return;
                plugin.getGuiManager().openReasonsMenu(player, targetName, "MUTE", 0);
            } 
            else if (item.getType() == Material.FEATHER) {
                if (!checkPerm(player, "maxstaff.punish.kick")) return;
                plugin.getGuiManager().openReasonsMenu(player, targetName, "KICK", 0);
            }
            return;
        }

        if (title.contains(reasonsBaseTitle)) { 
            String type = title.contains("[BAN]") ? "BAN" : (title.contains("[MUTE]") ? "MUTE" : "KICK");
            String target;
            try {
                target = title.split(" - ")[1].split(" \\(")[0].trim();
            } catch (Exception e) { return; }

            int page;
            try {
                page = Integer.parseInt(title.split("\\(")[1].split("/")[0]) - 1;
            } catch (Exception e) { page = 0; }

            if (item.getType() == plugin.getMainConfigManager().getNavBackMat()) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                plugin.getGuiManager().openUserInfoMenu(player, plugin.getServer().getPlayer(target));
            }
            else if (itemName.contains(ChatColor.stripColor(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getNavNextName())))) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                plugin.getGuiManager().openReasonsMenu(player, target, type, page + 1);
            }
            else if (itemName.contains(ChatColor.stripColor(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getNavPrevName())))) {
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
                
                String reasonName = plugin.getMainConfigManager().getReasonName(type, reasonId);
                
                if (type.equals("BAN")) plugin.getPunishmentManager().banPlayer(player, target, reasonName, duration);
                else if (type.equals("MUTE")) plugin.getPunishmentManager().mutePlayer(player, target, reasonName, duration);
                else if (type.equals("KICK")) plugin.getPunishmentManager().kickPlayer(player, target, reasonName);
                
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