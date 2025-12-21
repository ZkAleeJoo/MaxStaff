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

    public GuiListener(MaxStaff plugin) {
        this.plugin = plugin;
    }

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

        // 1. MENÚ DE JUGADORES
        if (title.contains(ChatColor.stripColor(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiPlayersTitle())))) {
            event.setCancelled(true);
            if (item.getType() == Material.PLAYER_HEAD) {
                String targetName = itemName;
                Player target = plugin.getServer().getPlayer(targetName);
                if (target != null) {
                    player.teleport(target);
                    player.sendMessage(MessageUtils.getColoredMessage("&aTeletransportado a " + target.getName()));
                }
                player.closeInventory();
            }
        }

        // 2. MENÚ DE SELECCIÓN DE TIPO
        else if (title.startsWith("Punish:")) {
            event.setCancelled(true);
            String targetName = title.split(": ")[1];
            
            if (item.getType() == Material.IRON_SWORD) {
                plugin.getGuiManager().openReasonsMenu(player, targetName, "BAN", 0);
            } else if (item.getType() == Material.PAPER) {
                plugin.getGuiManager().openReasonsMenu(player, targetName, "MUTE", 0);
            } else if (item.getType() == Material.FEATHER) {
                plugin.getPunishmentManager().kickPlayer(player, targetName, "Expulsado vía GUI");
                player.closeInventory();
            }
        }

        // 3. MENÚ DE MOTIVOS Y TIEMPOS UNIFICADO
        else if (title.startsWith("Sancionar -")) {
            event.setCancelled(true);
            String targetName = title.split(" - ")[1].split(" \\(")[0];
            int currentPage = Integer.parseInt(title.split("\\(")[1].split("/")[0]) - 1;

            if (item.getType() == Material.BOOK) {
                plugin.getGuiManager().openSanctionMenu(player, targetName);
                return;
            }

            if (item.getType() == Material.ARROW) {
                String type = getPunishTypeFromLore(item);
                if (itemName.contains("Siguiente")) {
                    plugin.getGuiManager().openReasonsMenu(player, targetName, type, currentPage + 1);
                } else if (itemName.contains("Anterior")) {
                    plugin.getGuiManager().openReasonsMenu(player, targetName, type, currentPage - 1);
                }
                return;
            }

            if (item.getType().name().endsWith("_DYE") && item.getItemMeta().hasLore()) {
                String type = "";
                String reasonId = "";
                String duration = "";
                
                for (String line : item.getItemMeta().getLore()) {
                    String cleanLine = ChatColor.stripColor(line);
                    if (cleanLine.startsWith("Tipo: ")) type = cleanLine.replace("Tipo: ", "");
                    if (cleanLine.startsWith("ID: ")) reasonId = cleanLine.replace("ID: ", "");
                    if (cleanLine.startsWith("TimeValue: ")) duration = cleanLine.replace("TimeValue: ", "");
                }

                String reasonName = plugin.getMainConfigManager().getReasonName(type, reasonId);

                if (type.equalsIgnoreCase("BAN")) {
                    plugin.getPunishmentManager().banPlayer(player, targetName, reasonName, duration);
                } else if (type.equalsIgnoreCase("MUTE")) {
                    plugin.getPunishmentManager().mutePlayer(player, targetName, reasonName, duration);
                }
                player.closeInventory();
            }
        }
    }

    private String getPunishTypeFromLore(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return "BAN";
        for (String line : item.getItemMeta().getLore()) {
            if (ChatColor.stripColor(line).contains("Tipo: MUTE")) return "MUTE";
        }
        return "BAN";
    }
}