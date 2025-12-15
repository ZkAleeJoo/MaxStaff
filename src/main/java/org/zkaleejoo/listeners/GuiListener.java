package org.zkaleejoo.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.zkaleejoo.MaxStaff;
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

        if (title.equals(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getTitleOnlinePlayers()))) {
            event.setCancelled(true); 
            
            if (event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                String targetName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                Player target = Bukkit.getPlayer(targetName);
                
                if (target != null) {
                    player.teleport(target);
                    player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getTpPlayer() + " " + target.getName()));
                } else {
                    player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getPlayerNoOnline().replace("{player}", targetName)));
                }
                player.closeInventory();
            }
        }

        else if (title.startsWith(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getSanctionMenuTitle()))) {
            event.setCancelled(true);
            

            String targetName = ChatColor.stripColor(title.split(": ")[1]);

            Material type = event.getCurrentItem().getType();
            
            if (type == Material.IRON_SWORD) { 
                plugin.getGuiManager().openTimeMenu(player, targetName, "BAN");
            } 
            else if (type == Material.PAPER) { 
                plugin.getGuiManager().openTimeMenu(player, targetName, "MUTE");
            } 
            else if (type == Material.FEATHER) { 
                player.closeInventory();
                Bukkit.dispatchCommand(player, "kick " + targetName + " " + plugin.getMainConfigManager().getKickedMessage());
            }
        }

        else if (title.startsWith(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getTitleDuration()))) {
            event.setCancelled(true);
            
            String cleanTitle = ChatColor.stripColor(title); 
            String type = cleanTitle.split(" ")[1].replace(":", ""); 
            String targetName = cleanTitle.split(": ")[1]; 

            ItemStack item = event.getCurrentItem();
            if (item.getType() == Material.ARROW) {
                plugin.getGuiManager().openSanctionMenu(player, targetName);
                return;
            }

        String duration = "";
            Material mat = item.getType();
            if (mat == Material.LIME_DYE) duration = "1h";
            else if (mat == Material.YELLOW_DYE) duration = "1d";
            else if (mat == Material.ORANGE_DYE) duration = "7d";
            else if (mat == Material.RED_DYE) duration = "perm";
            
            if (type.equals("BAN")) {
                            plugin.getPunishmentManager().banPlayer(player, targetName, plugin.getMainConfigManager().getSactionsMenu(), duration);
                        } else {
                            plugin.getPunishmentManager().mutePlayer(player, targetName, plugin.getMainConfigManager().getSactionsMenu(), duration);
                        }

            player.closeInventory();
    
        }
    }
}