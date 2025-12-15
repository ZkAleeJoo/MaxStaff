package org.zkaleejoo.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

public class StaffItemsListener implements Listener {

    private final MaxStaff plugin;

    public StaffItemsListener(MaxStaff plugin) {
        this.plugin = plugin;
    }

    // --- PROTECCIÓN: No tirar ítems ---
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.getStaffManager().isInStaffMode(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getCannotDrop()));
        }
    }

    // --- PROTECCIÓN: No poner bloques ---
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (plugin.getStaffManager().isInStaffMode(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getCannotPlace()));
        }
    }

    // --- INTERACCIÓN: Clic al aire/bloque ---
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();

        if (!plugin.getStaffManager().isInStaffMode(player)) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            // Vanish
            if (item.getType() == Material.NETHER_STAR) {
                event.setCancelled(true); 
                plugin.getStaffManager().toggleVanish(player);
            }
            // Reloj
            else if (item.getType() == Material.CLOCK) {
                event.setCancelled(true);
                plugin.getGuiManager().openPlayersMenu(player);
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgPlayers()));
            }
            // Libro
            else if (item.getType() == Material.BOOK) {
                event.setCancelled(true);
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getPlayerClickPls()));
            }
        }
    }

    // --- INTERACCIÓN: Clic a entidad (Jugador) ---
    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();

        if (!plugin.getStaffManager().isInStaffMode(player)) return;
        
        if (!(event.getRightClicked() instanceof Player)) return;

        Player target = (Player) event.getRightClicked();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Cofre
        if (item.getType() == Material.CHEST) {
            event.setCancelled(true);
            String msg = plugin.getMainConfigManager().getMsgInspect().replace("{player}", target.getName());
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + msg));
            player.openInventory(target.getInventory());
        }
        
        // Libro
        else if (item.getType() == Material.BOOK) {
            event.setCancelled(true);
            plugin.getGuiManager().openSanctionMenu(player, target.getName());
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgPunish()));
        }

        // Hielo
        else if (item.getType() == Material.PACKED_ICE) {
            event.setCancelled(true);
            plugin.getFreezeManager().toggleFreeze(player, target);
        }   
    }
}