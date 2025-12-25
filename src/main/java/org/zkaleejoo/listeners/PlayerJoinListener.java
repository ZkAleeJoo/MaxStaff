package org.zkaleejoo.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

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

        Player player = event.getPlayer();
    
    if (player.hasPermission("maxstaff.admin")) { 
        String latest = plugin.getLatestVersion();
        if (latest != null && !plugin.getDescription().getVersion().equalsIgnoreCase(latest)) {
            player.sendMessage(" ");
            player.sendMessage(MessageUtils.getColoredMessage("&4&lMaxStaff &8» &eA new version is available! (&b" + latest + "&e)"));
            player.sendMessage(MessageUtils.getColoredMessage("&7Your current version: &c" + plugin.getDescription().getVersion()));
            player.sendMessage(MessageUtils.getColoredMessage("&eDownload it to get improvements and fixes."));
            player.sendMessage(" ");
        }
    }

    }

    
}