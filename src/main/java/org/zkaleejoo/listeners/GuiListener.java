package org.zkaleejoo.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MaxStaffHolder; 
import org.zkaleejoo.utils.MessageUtils;

public class GuiListener implements Listener {

    private final MaxStaff plugin;

    public GuiListener(MaxStaff plugin) { 
        this.plugin = plugin; 
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        
        InventoryHolder rawHolder = event.getInventory().getHolder();
        if (!(rawHolder instanceof MaxStaffHolder)) {
            return; 
        }

        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        MainConfigManager config = plugin.getMainConfigManager();
        
        if (item.getType() == config.getBorderMaterial()) return;

        MaxStaffHolder holder = (MaxStaffHolder) rawHolder;
        String menuType = holder.getMenuType();
        String targetName = holder.getTargetName();

        switch (menuType) {
            case "INFO":
                handleInfoMenu(player, item, targetName, config);
                break;
            case "PLAYERS":
                handlePlayersMenu(player, item, config);
                break;
            case "SANCTIONS":
                handleSanctionMenu(player, item, targetName, config);
                break;
            case "REASONS":
                String type = (String) holder.getData("type");
                int page = (int) holder.getData("page");
                handleReasonsMenu(player, item, targetName, type, page, config);
                break;
            case "HISTORY":
                handleHistoryMenu(player, item, targetName, config);
                break;
            case "DETAILED_HISTORY":
                handleDetailedHistoryMenu(player, item, targetName, config);
                break;
            case "GAMEMODE":
                handleGameModeMenu(player, item, config);
                break;
            case "ALTS":
                break;
        }
    }

    private void handleInfoMenu(Player player, ItemStack item, String targetName, MainConfigManager config) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        
        if (item.getType() == config.getGuiInfoActionMat()) {
            plugin.getGuiManager().openSanctionMenu(player, targetName);
        } 
        else if (item.getType() == config.getGuiInfoHistoryMat()) {
            if (checkPerm(player, "maxstaff.history")) {
                plugin.getGuiManager().openHistoryMenu(player, targetName);
            }
        }
        else if (item.getType() == Material.COMPASS) { 
            plugin.getGuiManager().openAltsMenu(player, targetName);
        }
        else if (item.getType() == Material.CHEST) { 
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
                player.openInventory(target.getInventory());
            } else {
                player.sendMessage(MessageUtils.getColoredMessage("&cPlayer is offline"));
            }
        }
    }

    private void handlePlayersMenu(Player player, ItemStack item, MainConfigManager config) {
        if (item.getType() == Material.PLAYER_HEAD && item.hasItemMeta()) {
            String itemName = item.getItemMeta().getDisplayName();
            String cleanName = org.bukkit.ChatColor.stripColor(itemName); 
            
            Player target = Bukkit.getPlayer(cleanName);
            if (target != null) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.teleport(target);
                player.sendMessage(MessageUtils.getColoredMessage(config.getMsgTeleport().replace("{player}", target.getName())));
            }
            player.closeInventory();
        }
    }

    private void handleSanctionMenu(Player player, ItemStack item, String targetName, MainConfigManager config) {
        if (item.getType() == config.getNavBackMat()) {
            clickSound(player);
            Player targetPlayer = Bukkit.getPlayer(targetName);
            if (targetPlayer != null) {
                plugin.getGuiManager().openUserInfoMenu(player, targetPlayer);
            }
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

    private void handleReasonsMenu(Player player, ItemStack item, String targetName, String type, int page, MainConfigManager config) {
        if (item.getType() == config.getNavBackMat()) {
            clickSound(player);
            plugin.getGuiManager().openSanctionMenu(player, targetName);
            return;
        }
        
        String nextName = org.bukkit.ChatColor.stripColor(MessageUtils.getColoredMessage(config.getNavNextName()));
        String prevName = org.bukkit.ChatColor.stripColor(MessageUtils.getColoredMessage(config.getNavPrevName()));
        String itemName = org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName());

        if (itemName.contains(nextName)) {
            clickSound(player);
            plugin.getGuiManager().openReasonsMenu(player, targetName, type, page + 1);
        }
        else if (itemName.contains(prevName)) {
            clickSound(player);
            plugin.getGuiManager().openReasonsMenu(player, targetName, type, page - 1);
        }
        else if (item.getType().name().endsWith("_DYE")) {
            if (!checkPerm(player, "maxstaff.punish." + type.toLowerCase())) {
                player.closeInventory();
                return;
            }
            
            ItemMeta meta = item.getItemMeta();
            NamespacedKey reasonKey = new NamespacedKey(plugin, "reason_id");
            NamespacedKey durationKey = new NamespacedKey(plugin, "duration");
            
            String reasonId = meta.getPersistentDataContainer().get(reasonKey, PersistentDataType.STRING);
            String duration = meta.getPersistentDataContainer().get(durationKey, PersistentDataType.STRING);
            
            if (reasonId != null && duration != null) {
                String reasonName = config.getReasonName(type, reasonId);
                
                if (type.equals("BAN")) plugin.getPunishmentManager().banPlayer(player, targetName, reasonName, duration);
                else if (type.equals("MUTE")) plugin.getPunishmentManager().mutePlayer(player, targetName, reasonName, duration);
                else if (type.equals("KICK")) plugin.getPunishmentManager().kickPlayer(player, targetName, reasonName);
                
                player.closeInventory();
            }
        }
    }

    private void handleHistoryMenu(Player player, ItemStack item, String targetName, MainConfigManager config) {
        String type = "";
        if (item.getType() == Material.RED_WOOL) type = "BAN";
        else if (item.getType() == Material.ORANGE_WOOL) type = "MUTE";
        else if (item.getType() == Material.YELLOW_WOOL) type = "WARN";
        else if (item.getType() == Material.LIGHT_GRAY_WOOL) type = "KICK";

        if (!type.isEmpty()) {
            clickSound(player);
            plugin.getGuiManager().openDetailedHistoryMenu(player, targetName, type);
        }
    }

    private void handleDetailedHistoryMenu(Player player, ItemStack item, String targetName, MainConfigManager config) {
        if (item.getType() == config.getNavBackMat()) {
            clickSound(player);
            plugin.getGuiManager().openHistoryMenu(player, targetName);
        }
    }

    private void handleGameModeMenu(Player player, ItemStack item, MainConfigManager config) {
        clickSound(player);
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

    private void clickSound(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    private boolean checkPerm(Player player, String permission) {
        if (!player.hasPermission(permission)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()
            ));
            return false;
        }
        return true;
    }
}