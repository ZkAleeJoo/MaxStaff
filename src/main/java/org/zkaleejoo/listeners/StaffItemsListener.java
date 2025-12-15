package org.zkaleejoo.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

public class StaffItemsListener implements Listener {

    private final MaxStaff plugin;

    public StaffItemsListener(MaxStaff plugin) {
        this.plugin = plugin;
    }

    // EVENTO 1: Clic al aire o bloque
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getStaffManager().isInStaffMode(player)) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            // --- ESTRELLA (VANISH) ---
            if (item.getType() == Material.NETHER_STAR) {
                event.setCancelled(true); // Evitar que coloque bloques o interactúe
                plugin.getStaffManager().toggleVanish(player);
            }

            // --- RELOJ (JUGADORES) ---
            else if (item.getType() == Material.CLOCK) {
                event.setCancelled(true);
                player.sendMessage(MessageUtils.getColoredMessage("&ePróximamente: &fAbriendo menú de jugadores..."));
                // AQUÍ PONDREMOS EL MENÚ EN EL SIGUIENTE PASO
            }

            // --- LIBRO (SANCIONES - GENERAL) ---
            else if (item.getType() == Material.BOOK) {
                event.setCancelled(true);
                player.sendMessage(MessageUtils.getColoredMessage("&ePróximamente: &fAbriendo menú de sanciones..."));
            }
        }
    }

    // EVENTO 2: Clic a una entidad
    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getStaffManager().isInStaffMode(player)) return;

        if (!(event.getRightClicked() instanceof Player)) return;

        Player target = (Player) event.getRightClicked();
        ItemStack item = player.getInventory().getItemInMainHand();

        // --- COFRE (INSPECCIONAR) ---
        if (item.getType() == Material.CHEST) {
            event.setCancelled(true);
            player.sendMessage(MessageUtils.getColoredMessage("&6Inspeccionando a: &f" + target.getName()));
            player.openInventory(target.getInventory());
        }
        
        // --- LIBRO (SANCIONAR DIRECTO) ---
        else if (item.getType() == Material.BOOK) {
            event.setCancelled(true);
            player.sendMessage(MessageUtils.getColoredMessage("&cSancionando a: &f" + target.getName()));
        }
    }
}