package org.zkaleejoo.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.config.MainConfigManager;

public class PlayerJoinListener implements Listener {

    private final MaxStaff plugin;

    public PlayerJoinListener(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        MainConfigManager config = plugin.getMainConfigManager();

        if (plugin.getStaffManager().hasPersistentStaffData(player.getUniqueId())) {
            plugin.getStaffManager().disableStaffMode(player);
            player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + plugin.getMainConfigManager().getRestoredInventory()));
        }

        String ip = player.getAddress().getAddress().getHostAddress();
        plugin.getPunishmentManager().savePlayerIP(player.getUniqueId(), ip);

        for (java.util.UUID uuid : plugin.getStaffManager().getVanishedPlayers()) {
            Player staff = org.bukkit.Bukkit.getPlayer(uuid);
            if (staff != null && staff.isOnline()) {
                if (!player.hasPermission("maxstaff.see.vanish")) {
                    player.hidePlayer(plugin, staff);
                }
            }
        }

        if (player.hasPermission("maxstaff.admin")) { 
            String latest = plugin.getLatestVersion();
            if (latest != null && !plugin.getDescription().getVersion().equalsIgnoreCase(latest)) {
                player.sendMessage(" ");
                player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getMsgUpdateAvailable().replace("{version}", latest)));
                player.sendMessage(MessageUtils.getColoredMessage(config.getMsgUpdateCurrent().replace("{version}", plugin.getDescription().getVersion())));
                player.sendMessage(MessageUtils.getColoredMessage(config.getMsgUpdateDownload()));
                player.sendMessage(MessageUtils.getColoredMessage("&7https://modrinth.com/plugin/maxstaff"));
                player.sendMessage(" ");
            }
        }
    }
}