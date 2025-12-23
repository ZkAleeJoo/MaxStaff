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
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;

import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.entity.Player;

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

        MainConfigManager config = plugin.getMainConfigManager();

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            if (item.getType() == config.getMatVanish()) {
                event.setCancelled(true); 
                plugin.getStaffManager().toggleVanish(player);
            }
            else if (item.getType() == config.getMatPlayers()) {
                event.setCancelled(true);
                plugin.getGuiManager().openPlayersMenu(player);
                player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getMsgPlayers()));
            }
            else if (item.getType() == config.getMatPunish()) {
            event.setCancelled(true);

            org.bukkit.util.RayTraceResult ray = player.getWorld().rayTraceEntities(
                player.getEyeLocation(), 
                player.getEyeLocation().getDirection(), 
                5, 
                entity -> entity instanceof org.bukkit.entity.Player && !entity.equals(player)
            );

            if (ray != null && ray.getHitEntity() != null) {
                return;
            }

            player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getPlayerClickPls()));
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
        MainConfigManager config = plugin.getMainConfigManager();

        if (item.getType() == config.getMatInspect()) {
            event.setCancelled(true);
            String msg = config.getMsgInspect().replace("{player}", target.getName());
            player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + msg));
            player.openInventory(target.getInventory());
        }
        
        else if (item.getType() == config.getMatPunish()) {
            event.setCancelled(true);
            plugin.getGuiManager().openUserInfoMenu(player, target);
            player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + "&eCargando información del jugador..."));
        }

        else if (item.getType() == config.getMatFreeze()) {
            event.setCancelled(true);
            plugin.getFreezeManager().toggleFreeze(player, target);
        }   
    }

    // --- No recoger ítems ---
    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        if (plugin.getStaffManager().isInStaffMode(player)) {
            event.setCancelled(true); 
        
        }
    }
}