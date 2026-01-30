package org.zkaleejoo.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MaxStaffHolder;
import org.zkaleejoo.utils.MessageUtils;

import java.util.Optional;

public class GuiListener implements Listener {

    private final MaxStaff plugin;
    private final NamespacedKey reasonKey;
    private final NamespacedKey durationKey;

    public GuiListener(MaxStaff plugin) {
        this.plugin = plugin;
        this.reasonKey = new NamespacedKey(plugin, "reason_id");
        this.durationKey = new NamespacedKey(plugin, "duration");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        if (!(event.getInventory().getHolder() instanceof MaxStaffHolder holder)) return;

        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        MainConfigManager config = plugin.getMainConfigManager();
        if (item.getType() == config.getBorderMaterial()) return;

        handleMenuAction(player, item, holder, config);
    }

    private void handleMenuAction(Player player, ItemStack item, MaxStaffHolder holder, MainConfigManager config) {
        String menuType = holder.getMenuType();
        String targetName = holder.getTargetName();

        switch (menuType) {
            case "INFO" -> handleInfoMenu(player, item, targetName, config);
            case "PLAYERS" -> handlePlayersMenu(player, item, config);
            case "SANCTIONS" -> handleSanctionMenu(player, item, targetName, config);
            case "REASONS" -> handleReasonsMenu(player, item, holder, config);
            case "HISTORY" -> handleHistoryMenu(player, item, targetName, config);
            case "GAMEMODE" -> handleGameModeMenu(player, item, config);
            case "DETAILED_HISTORY" -> handleDetailedHistoryMenu(player, item, targetName, config);
        }
    }

    private void handleInfoMenu(Player player, ItemStack item, String targetName, MainConfigManager config) {
        Material mat = item.getType();
        
        if (mat == config.getGuiInfoActionMat()) {
            plugin.getGuiManager().openSanctionMenu(player, targetName);
        } else if (mat == config.getGuiInfoHistoryMat()) {
            if (checkPerm(player, "maxstaff.history")) plugin.getGuiManager().openHistoryMenu(player, targetName);
        } else if (mat == Material.COMPASS) {
            plugin.getGuiManager().openAltsMenu(player, targetName);
        } else if (mat == Material.CHEST) {
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                clickSound(player, Sound.BLOCK_CHEST_OPEN);
                player.openInventory(target.getInventory());
                return; 
            } else {
                player.sendMessage(MessageUtils.getColoredMessage("&cPlayer is offline"));
            }
        }
        clickSound(player, Sound.UI_BUTTON_CLICK);
    }

    private void handlePlayersMenu(Player player, ItemStack item, MainConfigManager config) {
        if (item.getType() == Material.PLAYER_HEAD && item.hasItemMeta()) {
            String cleanName = org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName());
            Player target = Bukkit.getPlayer(cleanName);
            
            if (target != null) {
                clickSound(player, Sound.ENTITY_ENDERMAN_TELEPORT);
                player.teleport(target);
                player.sendMessage(MessageUtils.getColoredMessage(config.getMsgTeleport().replace("{player}", target.getName())));
            }
            player.closeInventory();
        }
    }

    private void handleSanctionMenu(Player player, ItemStack item, String targetName, MainConfigManager config) {
        Material mat = item.getType();
        clickSound(player, Sound.UI_BUTTON_CLICK);

        if (mat == config.getNavBackMat()) {
            Optional.ofNullable(Bukkit.getPlayer(targetName))
                    .ifPresent(target -> plugin.getGuiManager().openUserInfoMenu(player, target));
        } else if (mat == Material.IRON_SWORD) {
            if (checkPerm(player, "maxstaff.punish.ban")) plugin.getGuiManager().openReasonsMenu(player, targetName, "BAN", 0);
        } else if (mat == Material.PAPER) {
            if (checkPerm(player, "maxstaff.punish.mute")) plugin.getGuiManager().openReasonsMenu(player, targetName, "MUTE", 0);
        } else if (mat == Material.FEATHER) {
            if (checkPerm(player, "maxstaff.punish.kick")) plugin.getGuiManager().openReasonsMenu(player, targetName, "KICK", 0);
        }
    }

    private void handleReasonsMenu(Player player, ItemStack item, MaxStaffHolder holder, MainConfigManager config) {
        Material mat = item.getType();
        String targetName = holder.getTargetName();
        String type = (String) holder.getData("type");
        int page = (int) holder.getData("page");

        if (mat == config.getNavBackMat()) {
            clickSound(player, Sound.UI_BUTTON_CLICK);
            plugin.getGuiManager().openSanctionMenu(player, targetName);
            return;
        }

        if (mat.name().endsWith("_DYE")) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            String reasonId = meta.getPersistentDataContainer().get(reasonKey, PersistentDataType.STRING);
            String duration = meta.getPersistentDataContainer().get(durationKey, PersistentDataType.STRING);

            if (reasonId != null && duration != null) {
                if (!checkPerm(player, "maxstaff.punish." + type.toLowerCase())) {
                    player.closeInventory();
                    return;
                }

                String reasonName = config.getReasonName(type, reasonId);
                executePunishment(player, targetName, type, reasonName, duration);
                player.closeInventory();
                return;
            }
        }
        String itemName = org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName());
        if (itemName.contains(org.bukkit.ChatColor.stripColor(MessageUtils.getColoredMessage(config.getNavNextName())))) {
            plugin.getGuiManager().openReasonsMenu(player, targetName, type, page + 1);
            clickSound(player, Sound.UI_BUTTON_CLICK);
        } else if (itemName.contains(org.bukkit.ChatColor.stripColor(MessageUtils.getColoredMessage(config.getNavPrevName())))) {
            plugin.getGuiManager().openReasonsMenu(player, targetName, type, page - 1);
            clickSound(player, Sound.UI_BUTTON_CLICK);
        }
    }

    private void handleHistoryMenu(Player player, ItemStack item, String targetName, MainConfigManager config) {
        String type = switch (item.getType()) {
            case RED_WOOL -> "BAN";
            case ORANGE_WOOL -> "MUTE";
            case YELLOW_WOOL -> "WARN";
            case LIGHT_GRAY_WOOL -> "KICK";
            default -> "";
        };

        if (!type.isEmpty()) {
            clickSound(player, Sound.UI_BUTTON_CLICK);
            plugin.getGuiManager().openDetailedHistoryMenu(player, targetName, type);
        }
    }

    private void handleGameModeMenu(Player player, ItemStack item, MainConfigManager config) {
        org.bukkit.GameMode mode = null;
        String modeName = "";

        if (item.getType() == config.getGuiGmSurvivalMat()) { mode = org.bukkit.GameMode.SURVIVAL; modeName = "Survival"; }
        else if (item.getType() == config.getGuiGmCreativeMat()) { mode = org.bukkit.GameMode.CREATIVE; modeName = "Creative"; }
        else if (item.getType() == config.getGuiGmAdventureMat()) { mode = org.bukkit.GameMode.ADVENTURE; modeName = "Adventure"; }
        else if (item.getType() == config.getGuiGmSpectatorMat()) { mode = org.bukkit.GameMode.SPECTATOR; modeName = "Spectator"; }

        if (mode != null) {
            clickSound(player, Sound.UI_BUTTON_CLICK);
            player.setGameMode(mode);
            if (plugin.getStaffManager().isInStaffMode(player)) {
                plugin.getStaffManager().updateSavedGameMode(player, mode);
            }
            player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getGuiGmFeedback().replace("{mode}", modeName)));
            player.closeInventory();
        }
    }

    private void handleDetailedHistoryMenu(Player player, ItemStack item, String targetName, MainConfigManager config) {
        if (item.getType() == config.getNavBackMat()) {
            clickSound(player, Sound.UI_BUTTON_CLICK);
            plugin.getGuiManager().openHistoryMenu(player, targetName);
        }
    }

    private void executePunishment(Player staff, String target, String type, String reason, String duration) {
        switch (type) {
            case "BAN" -> plugin.getPunishmentManager().banPlayer(staff, target, reason, duration);
            case "MUTE" -> plugin.getPunishmentManager().mutePlayer(staff, target, reason, duration);
            case "KICK" -> plugin.getPunishmentManager().kickPlayer(staff, target, reason);
        }
    }

    private void clickSound(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    private boolean checkPerm(Player player, String permission) {
        if (!player.hasPermission(permission)) {
            clickSound(player, Sound.ENTITY_VILLAGER_NO);
            player.sendMessage(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()
            ));
            return false;
        }
        return true;
    }
}