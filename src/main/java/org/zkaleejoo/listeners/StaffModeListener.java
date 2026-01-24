package org.zkaleejoo.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.zkaleejoo.MaxStaff;

public class StaffModeListener implements Listener {

    private final MaxStaff plugin;

    public StaffModeListener(MaxStaff plugin) {
        this.plugin = plugin;
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