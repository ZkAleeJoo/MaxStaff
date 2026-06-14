package org.zkaleejoo.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.managers.StaffManager;

public class PlayerQuitListener implements Listener {

    private final MaxStaff plugin;

    public PlayerQuitListener(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        StaffManager staffManager = plugin.getStaffManager();

        plugin.getReportManager().clearCooldown(player.getUniqueId());
        runInventoryQuitSequence(
                staffManager.isInStaffMode(player),
                () -> staffManager.disableStaffMode(player),
                () -> plugin.getInventorySnapshotManager().saveSnapshot(player),
                () -> staffManager.handlePlayerQuit(player));
        plugin.disableStaffChat(player.getUniqueId());
    }

    static void runInventoryQuitSequence(boolean inStaffMode,
            Runnable restoreStaffInventory,
            Runnable saveInventorySnapshot,
            Runnable clearSessionState) {
        if (inStaffMode) {
            restoreStaffInventory.run();
        }

        saveInventorySnapshot.run();
        clearSessionState.run();
    }
}
