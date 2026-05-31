package org.zkaleejoo.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.zkaleejoo.MaxStaff;

public class VanishProtectionListener implements Listener {

    private final MaxStaff plugin;

    public VanishProtectionListener(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player targetPlayer)) {
            return;
        }

        if (!plugin.getStaffManager().isVanished(targetPlayer)) {
            return;
        }

        event.setCancelled(true);
        event.setTarget(null);
    }
}