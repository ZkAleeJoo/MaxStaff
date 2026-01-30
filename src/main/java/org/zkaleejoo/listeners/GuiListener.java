package org.zkaleejoo.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MaxStaffHolder;
import org.zkaleejoo.utils.MessageUtils;

public class GuiListener implements Listener {

    private final MaxStaff plugin;
    private final NamespacedKey actionKey;
    private final NamespacedKey reasonKey;
    private final NamespacedKey durationKey;

    public GuiListener(MaxStaff plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "gui_action");
        this.reasonKey = new NamespacedKey(plugin, "reason_id");
        this.durationKey = new NamespacedKey(plugin, "duration");
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

        String action = getAction(item);
        if (action == null) return; 

        MaxStaffHolder holder = (MaxStaffHolder) rawHolder;
        String targetName = holder.getTargetName();
        MainConfigManager config = plugin.getMainConfigManager();

        switch (action) {
            case "open_history":
                if (checkPerm(player, "maxstaff.history")) {
                    clickSound(player);
                    plugin.getGuiManager().openHistoryMenu(player, targetName);
                }
                break;
                
            case "open_sanction":
                clickSound(player);
                plugin.getGuiManager().openSanctionMenu(player, targetName);
                break;
                
            case "open_alts":
                if (checkPerm(player, "maxstaff.alts")) {
                    clickSound(player);
                    plugin.getGuiManager().openAltsMenu(player, targetName);
                }
                break;
                
            case "open_inv":
                if (checkPerm(player, "maxstaff.invsee")) {
                    Player target = Bukkit.getPlayer(targetName);
                    if (target != null) {
                        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
                        player.openInventory(target.getInventory());
                    } else {
                        player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + "&c El jugador no está conectado."));
                        clickError(player);
                    }
                }
                break;

            case "punish_ban":
                if (checkPerm(player, "maxstaff.punish.ban"))
                    plugin.getGuiManager().openReasonsMenu(player, targetName, "BAN", 0);
                break;
                
            case "punish_mute":
                if (checkPerm(player, "maxstaff.punish.mute"))
                    plugin.getGuiManager().openReasonsMenu(player, targetName, "MUTE", 0);
                break;
                
            case "punish_kick":
                if (checkPerm(player, "maxstaff.punish.kick"))
                    plugin.getGuiManager().openReasonsMenu(player, targetName, "KICK", 0);
                break;

            case "back_info":
                clickSound(player);
                Player t = Bukkit.getPlayer(targetName);
                if (t != null) {
                    plugin.getGuiManager().openUserInfoMenu(player, t);
                } else {
                    player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + "&c Jugador offline, cerrando menú."));
                    player.closeInventory();
                }
                break;
                
            case "back_sanction":
                clickSound(player);
                plugin.getGuiManager().openSanctionMenu(player, targetName);
                break;
                
            case "back_history":
                clickSound(player);
                plugin.getGuiManager().openHistoryMenu(player, targetName);
                break;
                
            case "next_page":
                clickSound(player);
                int currentPage = (int) holder.getData("page");
                String typeNext = (String) holder.getData("type");
                plugin.getGuiManager().openReasonsMenu(player, targetName, typeNext, currentPage + 1);
                break;
                
            case "prev_page":
                clickSound(player);
                int pagePrev = (int) holder.getData("page");
                String typePrev = (String) holder.getData("type");
                plugin.getGuiManager().openReasonsMenu(player, targetName, typePrev, pagePrev - 1);
                break;

            case "apply_punish":
                handlePunishmentApplication(player, item, holder, config);
                break;

            case "history_bans":
                clickSound(player);
                plugin.getGuiManager().openDetailedHistoryMenu(player, targetName, "BAN");
                break;
            case "history_mutes":
                clickSound(player);
                plugin.getGuiManager().openDetailedHistoryMenu(player, targetName, "MUTE");
                break;
            case "history_kicks":
                clickSound(player);
                plugin.getGuiManager().openDetailedHistoryMenu(player, targetName, "KICK");
                break;
            case "history_warns":
                clickSound(player);
                plugin.getGuiManager().openDetailedHistoryMenu(player, targetName, "WARN");
                break;

            case "gm_survival":
                setGameMode(player, GameMode.SURVIVAL, config.getGuiGmSurvivalName());
                break;
            case "gm_creative":
                setGameMode(player, GameMode.CREATIVE, config.getGuiGmCreativeName());
                break;
            case "gm_adventure":
                setGameMode(player, GameMode.ADVENTURE, config.getGuiGmAdventureName());
                break;
            case "gm_spectator":
                setGameMode(player, GameMode.SPECTATOR, config.getGuiGmSpectatorName());
                break;

            case "tp_player":
                handleTeleport(player, item, targetName, config);
                break;
                
            case "info_stats":
            case "info_head":
            case "reason_display":
            case "history_record":
                clickSound(player);
                break;
        }
    }

    private String getAction(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    private void handlePunishmentApplication(Player player, ItemStack item, MaxStaffHolder holder, MainConfigManager config) {
        String type = (String) holder.getData("type");
        
        if (!checkPerm(player, "maxstaff.punish." + type.toLowerCase())) {
            player.closeInventory();
            return;
        }

        String reasonId = item.getItemMeta().getPersistentDataContainer().get(reasonKey, PersistentDataType.STRING);
        String duration = item.getItemMeta().getPersistentDataContainer().get(durationKey, PersistentDataType.STRING);

        if (reasonId != null && duration != null) {
            String reasonName = config.getReasonName(type, reasonId);
            
            switch (type) {
                case "BAN":
                    plugin.getPunishmentManager().banPlayer(player, holder.getTargetName(), reasonName, duration);
                    break;
                case "MUTE":
                    plugin.getPunishmentManager().mutePlayer(player, holder.getTargetName(), reasonName, duration);
                    break;
                case "KICK":
                    plugin.getPunishmentManager().kickPlayer(player, holder.getTargetName(), reasonName);
                    break;
            }
            player.closeInventory();
        } else {
            player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + "&cError interno: Datos del ítem corruptos."));
            clickError(player);
        }
    }

    private void setGameMode(Player player, GameMode mode, String modeDisplayName) {
        if (checkPerm(player, "maxstaff.gamemode")) {
            player.setGameMode(mode);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            
            String cleanModeName = org.bukkit.ChatColor.stripColor(MessageUtils.getColoredMessage(modeDisplayName));
            
            player.sendMessage(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getMsgGamemode().replace("{gamemode}", cleanModeName)
            ));
            player.closeInventory();
        }
    }

    private void handleTeleport(Player player, ItemStack item, String holderTargetName, MainConfigManager config) {
        String targetName = null;
        
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            targetName = org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName());
        }
        
        if (targetName == null || targetName.isEmpty()) {
            targetName = holderTargetName;
        }

        if (targetName != null) {
            Player targetTp = Bukkit.getPlayer(targetName);
            if (targetTp != null) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.teleport(targetTp);
                player.sendMessage(MessageUtils.getColoredMessage(
                    config.getMsgTeleport().replace("{player}", targetTp.getName())
                ));
                player.closeInventory();
            } else {
                player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + "&c Jugador no encontrado o offline."));
                clickError(player);
            }
        }
    }

    private void clickSound(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }
    
    private void clickError(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    private boolean checkPerm(Player player, String permission) {
        if (!player.hasPermission(permission)) {
            clickError(player);
            player.sendMessage(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()
            ));
            return false;
        }
        return true;
    }
}