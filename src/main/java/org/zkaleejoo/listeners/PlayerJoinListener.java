package org.zkaleejoo.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.zkaleejoo.MaxStaff;

public class PlayerJoinListener implements Listener {

    private final MaxStaff plugin;

    public PlayerJoinListener(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player newPlayer = event.getPlayer();

        for (java.util.UUID uuid : plugin.getStaffManager().getVanishedPlayers()) {
            Player staff = org.bukkit.Bukkit.getPlayer(uuid);
            if (staff != null && staff.isOnline()) {
                if (!newPlayer.hasPermission("maxstaff.see.vanish")) {
                    newPlayer.hidePlayer(plugin, staff);
                }
            }
        }
    }
}