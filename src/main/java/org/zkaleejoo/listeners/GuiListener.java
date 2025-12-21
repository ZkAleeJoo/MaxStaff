package org.zkaleejoo.listeners;

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

    public GuiListener(MaxStaff plugin) { this.plugin = plugin; }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = ChatColor.stripColor(event.getView().getTitle());
        ItemStack item = event.getCurrentItem();
        
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        String itemName = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        if (item.getType() == plugin.getMainConfigManager().getBorderMaterial()) {
            event.setCancelled(true);
            return;
        }

        String playersTitle = ChatColor.stripColor(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiPlayersTitle()));
        if (title.contains(playersTitle)) {
            event.setCancelled(true);
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

        String sanctionsTitle = ChatColor.stripColor(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiSanctionsTitle().replace("{target}", "")));
        if (title.contains(sanctionsTitle)) {
            event.setCancelled(true);
            String target = title.replace(sanctionsTitle, "").trim();
            if (item.getType() == Material.IRON_SWORD) plugin.getGuiManager().openReasonsMenu(player, target, "BAN", 0);
            else if (item.getType() == Material.PAPER) plugin.getGuiManager().openReasonsMenu(player, target, "MUTE", 0);
            else if (item.getType() == Material.FEATHER) plugin.getGuiManager().openReasonsMenu(player, target, "KICK", 0);
            return;
        }

        String reasonsBaseTitle = ChatColor.stripColor(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiReasonsTitle().split("\\{")[0]));
        
        if (title.contains(reasonsBaseTitle)) { 
            event.setCancelled(true);
            
            String type = title.contains("[BAN]") ? "BAN" : (title.contains("[MUTE]") ? "MUTE" : "KICK");
            
            String target;
            try {
                target = title.split(" - ")[1].split(" \\(")[0];
            } catch (Exception e) { return; }

            int page;
            try {
                page = Integer.parseInt(title.split("\\(")[1].split("/")[0]) - 1;
            } catch (Exception e) { page = 0; }

            if (item.getType() == plugin.getMainConfigManager().getNavBackMat()) {
                plugin.getGuiManager().openSanctionMenu(player, target);
            }
            else if (item.getType() == plugin.getMainConfigManager().getNavNextMat() && itemName.contains(ChatColor.stripColor(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getNavNextName())))) {
                plugin.getGuiManager().openReasonsMenu(player, target, type, page + 1);
            }
            else if (item.getType() == plugin.getMainConfigManager().getNavPrevMat() && itemName.contains(ChatColor.stripColor(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getNavPrevName())))) {
                plugin.getGuiManager().openReasonsMenu(player, target, type, page - 1);
            }
            else if (item.getType().name().endsWith("_DYE") && item.getItemMeta().hasLore()) {
                String reasonId = "", duration = "";
                for (String line : item.getItemMeta().getLore()) {
                    String clean = ChatColor.stripColor(line);
                    if (clean.startsWith("ID: ")) reasonId = clean.replace("ID: ", "");
                    if (clean.startsWith("TimeValue: ")) duration = clean.replace("TimeValue: ", "");
                }
                
                String reasonName = plugin.getMainConfigManager().getReasonName(type, reasonId);
                
                if (type.equals("BAN")) {
                    plugin.getPunishmentManager().banPlayer(player, target, reasonName, duration);
                } else if (type.equals("MUTE")) {
                    plugin.getPunishmentManager().mutePlayer(player, target, reasonName, duration);
                } else if (type.equals("KICK")) {
                    plugin.getPunishmentManager().kickPlayer(player, target, reasonName);
                }
                player.closeInventory();
            }
        }
    }
}