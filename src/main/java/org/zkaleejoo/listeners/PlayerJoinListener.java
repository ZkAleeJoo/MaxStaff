package org.zkaleejoo.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.IPUtils;
import java.util.Objects;

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
            player.sendMessage(MessageUtils
                    .getColoredMessage(config.getPrefix() + plugin.getMainConfigManager().getRestoredInventory()));
        }

        plugin.getStaffManager().restoreVanishOnJoin(player);

        plugin.getPunishmentManager().savePlayerIdentity(player.getUniqueId(), player.getName());

        String ip = IPUtils.resolvePlayerIp(player);
        if (ip == null) {
            plugin.getLogger().warning(
                    "Could not resolve IP for player " + player.getName() + " on join; skipping IP cache update.");
        } else {
            plugin.getPunishmentManager().savePlayerIP(player.getUniqueId(), ip);
        }

        for (java.util.UUID uuid : plugin.getStaffManager().getVanishedPlayers()) {
            Player staff = org.bukkit.Bukkit.getPlayer(uuid);
            if (staff != null && staff.isOnline()) {
                if (!player.hasPermission("maxstaff.see.vanish")) {
                    player.hidePlayer(Objects.requireNonNull(plugin), staff);
                    continue;
                }

                player.showPlayer(Objects.requireNonNull(plugin), staff);
            }
        }

        if (player.hasPermission("maxstaff.admin")) {
            String latest = plugin.getLatestVersion();
            if (latest != null && !plugin.getPluginMeta().getVersion().equalsIgnoreCase(latest)) {
                player.sendMessage(" ");
                player.sendMessage(MessageUtils.getColoredMessage(
                        config.getPrefix() + config.getMsgUpdateAvailable().replace("{version}", latest)));
                player.sendMessage(MessageUtils.getColoredMessage(
                        config.getMsgUpdateCurrent().replace("{version}", plugin.getPluginMeta().getVersion())));
                player.sendMessage(MessageUtils.getColoredMessage(config.getMsgUpdateDownload()));
                player.sendMessage(
                        MessageUtils.getColoredMessage("&7https://modrinth.com/plugin/maxstaff"));
                player.sendMessage(" ");
            }
        }
    }
}
