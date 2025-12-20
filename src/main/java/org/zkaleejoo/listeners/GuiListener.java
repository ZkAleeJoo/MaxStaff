package org.zkaleejoo.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;

public class GuiListener implements Listener {

    private final MaxStaff plugin;

    public GuiListener(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        MainConfigManager config = plugin.getMainConfigManager();

        String configPlayersTitle = MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiPlayersTitle());
        
        String configSanctionTitleBase = MessageUtils.getColoredMessage(
            plugin.getMainConfigManager().getGuiSanctionsTitle().split("\\{target}")[0]); 
            
        String configDurationTitleBase = MessageUtils.getColoredMessage(
            plugin.getMainConfigManager().getGuiDurationTitle().split("\\{type}")[0]);

        // --- 1. MENÚ DE JUGADORES---
        if (title.equals(configPlayersTitle)) {
            event.setCancelled(true); 
            
            if (event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                String targetName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                Player target = plugin.getServer().getPlayer(targetName);
                
                if (target != null) {
                    player.teleport(target);
                    player.sendMessage(MessageUtils.getColoredMessage("&aTeletransportado a " + target.getName()));
                } else {
                    player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getMsgOffline()));
                }
                player.closeInventory();
            }
        }

        // --- 2. MENÚ DE SANCIONES---
        else if (title.startsWith(configSanctionTitleBase)) {
            event.setCancelled(true);
            
            String targetName = ChatColor.stripColor(title.substring(title.lastIndexOf(" ") + 1));

            String banName = MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiItemBanName());
            String muteName = MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiItemMuteName());
            String kickName = MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiItemKickName());
            
            String clickedName = event.getCurrentItem().getItemMeta().getDisplayName();

            if (clickedName.equals(banName)) { 
                plugin.getGuiManager().openTimeMenu(player, targetName, "BAN");
            } 
            else if (clickedName.equals(muteName)) { 
                plugin.getGuiManager().openTimeMenu(player, targetName, "MUTE");
            } 
            else if (clickedName.equals(kickName)) { 
                player.closeInventory();
                plugin.getPunishmentManager().kickPlayer(player, targetName, "Kicked from GUI");
            }
        }

        // --- 3. MENÚ DE DURACIÓN---
        else if (title.startsWith(configDurationTitleBase)) {
            event.setCancelled(true);
            
            String cleanTitle = ChatColor.stripColor(title);
            String type = cleanTitle.split(" ")[1].replace(":", ""); 
            String targetName = cleanTitle.split(": ")[1];

            String name1h = MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiTime1hName());
            String name1d = MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiTime1dName());
            String name7d = MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiTime7dName());
            String namePerm = MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiTimePermName());
            String nameBack = MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiBackName());
            
            String clickedName = event.getCurrentItem().getItemMeta().getDisplayName();

            if (clickedName.equals(nameBack)) {
                plugin.getGuiManager().openSanctionMenu(player, targetName);
                return;
            }

            String duration = "";
            if (clickedName.equals(name1h)) duration = "1h";
            else if (clickedName.equals(name1d)) duration = "1d";
            else if (clickedName.equals(name7d)) duration = "7d";
            else if (clickedName.equals(namePerm)) duration = "perm";
            else return;
            if (type.equals("BAN")) {
                plugin.getPunishmentManager().banPlayer(player, targetName, "Banned from GUI", duration);
            } else {
                plugin.getPunishmentManager().mutePlayer(player, targetName, "Muted from GUI", duration);
            }

            player.closeInventory();
        }

        // 1. MENÚ PRINCIPAL DE SANCIONES (Ban/Mute/Kick)
    if (title.startsWith(MessageUtils.getColoredMessage(config.getGuiSanctionsTitle().split("\\{target}")[0]))) {
        event.setCancelled(true);
        String targetName = org.bukkit.ChatColor.stripColor(title.substring(title.lastIndexOf(" ") + 1));
        String clickedName = event.getCurrentItem().getItemMeta().getDisplayName();

        if (clickedName.equals(MessageUtils.getColoredMessage(config.getGuiItemBanName()))) { 
            plugin.getGuiManager().openReasonsMenu(player, targetName, "BAN"); 
        } 
        else if (clickedName.equals(MessageUtils.getColoredMessage(config.getGuiItemMuteName()))) { 
            plugin.getGuiManager().openReasonsMenu(player, targetName, "MUTE"); 
        }
    }

    // 2. MENÚ DE MOTIVOS 
    else if (title.startsWith(MessageUtils.getColoredMessage("&8Motivos "))) {
        event.setCancelled(true);
        String cleanTitle = org.bukkit.ChatColor.stripColor(title);
        String type = cleanTitle.split(" ")[1].replace(":", "");
        String targetName = cleanTitle.split(": ")[1];
        
        String reasonId = org.bukkit.ChatColor.stripColor(event.getCurrentItem().getItemMeta().getLore().get(0)).replace("ID: ", "");
        plugin.getGuiManager().openReasonDurationMenu(player, targetName, type, reasonId);
    }

    else if (title.contains(" - ")) { 
        event.setCancelled(true);
        String cleanTitle = org.bukkit.ChatColor.stripColor(title);
        String type = cleanTitle.split(" - ")[0]; 
        String reasonId = cleanTitle.split(" - ")[1].split(": ")[0];
        String targetName = cleanTitle.split(": ")[1];
        
        String duration = org.bukkit.ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).replace("Duración: ", "");
        String reasonName = config.getReasonName(type, reasonId);

        if (type.equals("BAN")) {
            plugin.getPunishmentManager().banPlayer(player, targetName, reasonName, duration);
        } else {
            plugin.getPunishmentManager().mutePlayer(player, targetName, reasonName, duration);
        }
        player.closeInventory();
    }

    }



    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        
        String title = event.getView().getTitle();
        ItemStack item = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();

        // 1. Cancelar todo clic en cristal decorativo
        if (item.getType() == plugin.getMainConfigManager().getBorderMaterial()) {
            event.setCancelled(true);
            return;
        }

        // 2. Manejar Paginación y Navegación
        if (title.contains("Motivos")) {
            event.setCancelled(true);
            String type = title.contains("BAN") ? "BAN" : "MUTE";
            int currentPage = Integer.parseInt(title.split("Pág. ")[1].replace(")", "")) - 1;
            
            if (item.getItemMeta().getDisplayName().contains("Siguiente")) {
                plugin.getGuiManager().openReasonsMenu(player, "target", type, currentPage + 1);
            } else if (item.getItemMeta().getDisplayName().contains("Anterior")) {
                plugin.getGuiManager().openReasonsMenu(player, "target", type, currentPage - 1);
            } else if (item.getItemMeta().getDisplayName().contains("Volver")) {
                plugin.getGuiManager().openSanctionMenu(player, "target");
            }
        }
    }
}