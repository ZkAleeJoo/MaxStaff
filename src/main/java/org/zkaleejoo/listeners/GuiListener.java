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
        String itemName = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        if (item.getType() == plugin.getMainConfigManager().getBorderMaterial()) {
            event.setCancelled(true);
            return;
        }

        if (title.contains(ChatColor.stripColor(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiPlayersTitle())))) {
            event.setCancelled(true);
            if (item.getType() == Material.PLAYER_HEAD) {
                Player target = plugin.getServer().getPlayer(itemName);
                if (target != null) {
                    player.teleport(target);
                    player.sendMessage(MessageUtils.getColoredMessage("&aTeletransportado a " + target.getName()));
                }
                player.closeInventory();
            }
        }

        else if (title.startsWith("Punish:")) {
            event.setCancelled(true);
            String target = title.split(": ")[1];
            if (item.getType() == Material.IRON_SWORD) plugin.getGuiManager().openReasonsMenu(player, target, "BAN", 0);
            else if (item.getType() == Material.PAPER) plugin.getGuiManager().openReasonsMenu(player, target, "MUTE", 0);
            else if (item.getType() == Material.FEATHER) {
                plugin.getPunishmentManager().kickPlayer(player, target, "Expulsado vía GUI");
                player.closeInventory();
            }
        }

        else if (title.startsWith("Sancionar")) {
            event.setCancelled(true);
            String type = title.contains("[BAN]") ? "BAN" : "MUTE";
            String target = title.split(" - ")[1].split(" \\(")[0];
            int page = Integer.parseInt(title.split("\\(")[1].split("/")[0]) - 1;

            if (item.getType() == plugin.getMainConfigManager().getNavBackMat()) {
                plugin.getGuiManager().openSanctionMenu(player, target);
            }
            else if (item.getType() == plugin.getMainConfigManager().getNavNextMat() && itemName.contains("Siguiente")) {
                plugin.getGuiManager().openReasonsMenu(player, target, type, page + 1);
            }
            else if (item.getType() == plugin.getMainConfigManager().getNavPrevMat() && itemName.contains("Anterior")) {
                plugin.getGuiManager().openReasonsMenu(player, target, type, page - 1);
            }
            else if (item.getType().name().endsWith("_DYE") && item.getItemMeta().hasLore()) {
                String reasonId = "", duration = "";
                for (String line : item.getItemMeta().getLore()) {
                    String clean = ChatColor.stripColor(line);
                    if (clean.startsWith("ID: ")) reasonId = clean.replace("ID: ", "");
                    if (clean.startsWith("TimeValue: ")) duration = clean.replace("TimeValue: ", "");
                }
                String name = plugin.getMainConfigManager().getReasonName(type, reasonId);
                if (type.equals("BAN")) plugin.getPunishmentManager().banPlayer(player, target, name, duration);
                else plugin.getPunishmentManager().mutePlayer(player, target, name, duration);
                player.closeInventory();
            }
        }
    }
}