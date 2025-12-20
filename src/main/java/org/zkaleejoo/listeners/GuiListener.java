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

        // Cancelar si es el material de borde
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

        // 2. MENÚ DE SELECCIÓN DE TIPO (BAN/MUTE/KICK)
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

        // 3. MENÚ DE MOTIVOS
        else if (title.startsWith("Sancionar -")) {
            event.setCancelled(true);
            String targetName = title.split(" - ")[1].split(" \\(")[0];
            String type = title.contains("(BAN)") ? "BAN" : "MUTE";

            if (itemName.contains("Volver")) {
                plugin.getGuiManager().openSanctionMenu(player, targetName);
            } else if (itemName.contains("Siguiente")) {
            } else if (item.getItemMeta().hasLore()) {
                String reasonId = ChatColor.stripColor(item.getItemMeta().getLore().get(0)).replace("ID: ", "");
                plugin.getGuiManager().openReasonDurationMenu(player, targetName, type, reasonId);
            }
        }

        // 4. MENÚ DE DURACIÓN FINAL
        else if (title.startsWith("Tiempo:")) {
            event.setCancelled(true);
            String parts = title.replace("Tiempo: ", "");
            String type = parts.split(" - ")[0];
            String reasonId = parts.split(" - ")[1].split(" : ")[0];
            String targetName = parts.split(" : ")[1];

            if (itemName.contains("Volver")) {
                plugin.getGuiManager().openReasonsMenu(player, targetName, type, 0);
                return;
            }

            if (item.getType() == Material.CLOCK) {
                String duration = itemName.replace("Duración: ", "").trim();
                String reasonName = plugin.getMainConfigManager().getReasonName(type, reasonId);

                if (type.equals("BAN")) {
                    plugin.getPunishmentManager().banPlayer(player, targetName, reasonName, duration);
                } else {
                    plugin.getPunishmentManager().mutePlayer(player, targetName, reasonName, duration);
                }
                player.closeInventory();
            }
        }
    }
}