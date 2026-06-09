package org.zkaleejoo.listeners;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.zkaleejoo.MaxStaff;

public class StaffModeListener implements Listener {

    private final MaxStaff plugin;

    public StaffModeListener(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onStaffDamagePlayer(EntityDamageByEntityEvent event) {
        Player staff = null;

        if (event.getDamager() instanceof Player) {
            staff = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                staff = (Player) projectile.getShooter();
            }
        }

        if (staff == null || !plugin.getStaffManager().isInStaffMode(staff)) {
            return;
        }

        if (!plugin.getMainConfigManager().isStaffModeAllowHit()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (plugin.getStaffManager().isInStaffMode(player)) {
                event.setCancelled(true);
                player.setFoodLevel(20);
            }
        }
    }
}