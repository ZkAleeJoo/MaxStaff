package org.zkaleejoo.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.managers.PunishmentManagerMysql;
import org.zkaleejoo.utils.BanScreen;
import org.zkaleejoo.utils.BanUtils;
import org.zkaleejoo.utils.IPUtils;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.FoliaCompat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GlobalPunishmentListener implements Listener {

    private final MaxStaff plugin;
    private static final long LIVE_BAN_SYNC_TICKS = 20L;
    private static final int SYNC_BATCH_LIMIT = 100;

    public GlobalPunishmentListener(MaxStaff plugin) {
        this.plugin = plugin;
        startLiveBanSyncTask();
        startSyncActionsConsumerTask();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (plugin.getPunishmentManager() instanceof PunishmentManagerMysql mysqlManager) {
            handleMysqlPreLogin(event, mysqlManager);
            return;
        }

        handleBukkitBanListPreLogin(event);
    }

    private void handleMysqlPreLogin(AsyncPlayerPreLoginEvent event, PunishmentManagerMysql mysqlManager) {
        try {
            String banMessage = mysqlManager.getActiveBanMessage(event.getUniqueId(), event.getName());
            if (banMessage != null) {
                disallowWithParsedBanScreen(event, banMessage);
                plugin.getLogger().fine("[MaxStaff IP] PreLogin blocked by NAME/UUID ban for " + event.getName());
                return;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[MaxStaff] Error checking ban for " + event.getName() + ": " + e.getMessage());
        }

        String normalizedIp = normalizePreLoginIp(event);
        if (normalizedIp == null) {
            return;
        }

        try {
            String ipBanMessage = mysqlManager.getActiveIPBanMessage(normalizedIp);
            if (ipBanMessage != null) {
                plugin.getLogger().fine(
                        "[MaxStaff IP] PreLogin blocked by IP ban for " + event.getName() + " (" + normalizedIp + ")");
                disallowWithParsedBanScreen(event, ipBanMessage);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[MaxStaff] Error checking IP ban for " + event.getName() + " (" + normalizedIp
                    + "): " + e.getMessage());
        }
    }

    private void handleBukkitBanListPreLogin(AsyncPlayerPreLoginEvent event) {
        try {
            String banMessage = BanUtils.getPlayerBanMessage(event.getName(), plugin.getMainConfigManager());
            if (banMessage != null && !banMessage.isBlank()) {
                disallowWithParsedBanScreen(event, banMessage);
                plugin.getLogger().fine("[MaxStaff IP] PreLogin blocked by local NAME ban for " + event.getName());
                return;
            }
        } catch (Exception e) {
            plugin.getLogger()
                    .warning("[MaxStaff] Error checking name ban for " + event.getName() + ": " + e.getMessage());
        }

        String normalizedIp = normalizePreLoginIp(event);
        if (normalizedIp == null) {
            return;
        }

        try {
            String ipBanMessage = BanUtils.getIpBanMessage(normalizedIp, plugin.getMainConfigManager());
            if (ipBanMessage != null && !ipBanMessage.isBlank()) {
                disallowWithParsedBanScreen(event, ipBanMessage);
                plugin.getLogger().fine("[MaxStaff IP] PreLogin blocked by local IP ban for " + event.getName() + " ("
                        + normalizedIp + ")");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[MaxStaff] Error checking IP ban for " + event.getName() + " (" + normalizedIp
                    + "): " + e.getMessage());
        }
    }

    private String normalizePreLoginIp(AsyncPlayerPreLoginEvent event) {
        if (event.getAddress() == null) {
            plugin.getLogger()
                    .fine("[MaxStaff IP] PreLogin has no address for " + event.getName() + "; skipping IP-ban check.");
            return null;
        }

        String normalizedIp = IPUtils.normalizeIp(event.getAddress().getHostAddress());
        if (normalizedIp == null) {
            plugin.getLogger().fine("[MaxStaff IP] PreLogin IP normalization failed for " + event.getName());
        }
        return normalizedIp;
    }

    private void disallowWithParsedBanScreen(AsyncPlayerPreLoginEvent event, String message) {
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, MessageUtils.toComponent(message));
    }

    private void startLiveBanSyncTask() {
        if (!(plugin.getPunishmentManager() instanceof PunishmentManagerMysql mysqlManager)) {
            return;
        }

        FoliaCompat.runAsyncTimer(plugin, () -> {
            if (!plugin.isEnabled()) {
                return;
            }

            Map<UUID, String> playersToKick = new LinkedHashMap<>();

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer == null)
                    continue;
                String banMessage = mysqlManager.getActiveBanMessage(onlinePlayer.getUniqueId(),
                        onlinePlayer.getName());
                if (banMessage != null) {
                    playersToKick.put(onlinePlayer.getUniqueId(), banMessage);
                }
            }

            if (playersToKick.isEmpty()) {
                return;
            }

            FoliaCompat.runGlobal(plugin, () -> {
                for (Map.Entry<UUID, String> entry : playersToKick.entrySet()) {
                    Player target = Bukkit.getPlayer(entry.getKey());
                    if (target != null && target.isOnline()) {
                        FoliaCompat.runForEntity(plugin, target, () -> {
                            if (target.isOnline()) {
                                target.kick(MessageUtils.toComponent(entry.getValue()));
                            }
                        });
                    }
                }
            });
        }, LIVE_BAN_SYNC_TICKS, LIVE_BAN_SYNC_TICKS);
    }

    private void startSyncActionsConsumerTask() {
        if (!(plugin.getPunishmentManager() instanceof PunishmentManagerMysql mysqlManager)) {
            return;
        }

        FoliaCompat.runAsyncTimer(plugin, () -> {
            if (!plugin.isEnabled()) {
                return;
            }

            List<SyncAction> actions = pollSyncActions(mysqlManager);
            if (actions.isEmpty()) {
                return;
            }

            FoliaCompat.runGlobal(plugin, () -> {
                for (SyncAction action : actions) {
                    handleSyncAction(action, mysqlManager);
                }
            });
        }, 20L, 20L);
    }

    private List<SyncAction> pollSyncActions(PunishmentManagerMysql mysqlManager) {
        List<SyncAction> actions = new ArrayList<>();
        long now = System.currentTimeMillis();

        try (Connection conn = mysqlManager.getSqlConnection()) {
            try (PreparedStatement cleanupExpired = conn
                    .prepareStatement("DELETE FROM MaxStaff_sync_actions WHERE expires_at <= ?")) {
                cleanupExpired.setLong(1, now);
                cleanupExpired.executeUpdate();
            }

            try (PreparedStatement cleanupOrphanAcks = conn.prepareStatement(
                    "DELETE ack FROM MaxStaff_sync_action_acks ack LEFT JOIN MaxStaff_sync_actions act ON act.id = ack.action_id WHERE act.id IS NULL")) {
                cleanupOrphanAcks.executeUpdate();
            }

            String serverId = mysqlManager.getSyncSourceServer();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT act.id, act.action_type, act.target_uuid, act.target_name, " +
                            "COALESCE(ip_ban.reason, act.reason) AS reason, COALESCE(ip_ban.staff, act.staff) AS staff, "
                            +
                            "ip_ban.expiry AS ban_expiry " +
                            "FROM MaxStaff_sync_actions act " +
                            "LEFT JOIN MaxStaff_sync_action_acks ack ON ack.action_id = act.id AND ack.server_id = ? " +
                            "LEFT JOIN MaxStaff_ip_bans ip_ban ON act.action_type = 'BAN_IP' AND ip_ban.ip = act.target_name "
                            +
                            "WHERE act.source_server <> ? AND act.expires_at > ? AND ack.action_id IS NULL " +
                            "ORDER BY act.created_at ASC LIMIT ?")) {
                ps.setString(1, serverId);
                ps.setString(2, serverId);
                ps.setLong(3, now);
                ps.setInt(4, SYNC_BATCH_LIMIT);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        actions.add(new SyncAction(
                                rs.getLong("id"),
                                rs.getString("action_type"),
                                rs.getString("target_uuid"),
                                rs.getString("target_name"),
                                rs.getString("reason"),
                                rs.getString("staff"),
                                rs.getObject("ban_expiry", Long.class)));
                    }
                }
            }

            if (!actions.isEmpty()) {
                try (PreparedStatement ack = conn.prepareStatement(
                        "INSERT INTO MaxStaff_sync_action_acks (action_id, server_id, processed_at) VALUES (?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE processed_at = VALUES(processed_at)")) {
                    for (SyncAction action : actions) {
                        ack.setLong(1, action.id());
                        ack.setString(2, serverId);
                        ack.setLong(3, now);
                        ack.addBatch();
                    }
                    ack.executeBatch();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[MaxStaff SQL] Failed to poll sync actions: " + e.getMessage());
        }

        return actions;
    }

    private void handleSyncAction(SyncAction action, PunishmentManagerMysql mysqlManager) {
        String type = action.type() == null ? "" : action.type().toUpperCase();
        switch (type) {
            case "KICK" -> {
                Player target = Bukkit.getPlayer(action.targetName());
                if (target != null) {
                    String kickScreen = plugin.getMainConfigManager().getScreenKick()
                            .replace("{staff}", action.staff() == null ? "CONSOLE" : action.staff())
                            .replace("{reason}", action.reason() == null ? "N/A" : action.reason());
                    target.kick(MessageUtils.toComponent(kickScreen));
                }
            }
            case "BAN_IP" -> applyLiveIpBan(action);
            case "UNBAN_IP" -> {
                if (action.targetName() != null) {
                    BanUtils.pardonIp(IPUtils.normalizeIp(action.targetName()));
                }
            }
            case "UNMUTE" -> {
                if (action.targetUuid() != null) {
                    try {
                        mysqlManager.clearMuteCacheFor(UUID.fromString(action.targetUuid()));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            case "UNBAN", "BAN", "MUTE" -> {
            }
            default -> plugin.getLogger()
                    .warning("[MaxStaff SQL] Sync action ignored (no handler): " + type + " id=" + action.id());
        }
    }

    private void applyLiveIpBan(SyncAction action) {
        String targetIp = IPUtils.normalizeIp(action.targetName());
        Long expiry = action.banExpiry();
        if (targetIp == null || expiry == null) {
            return;
        }

        long now = System.currentTimeMillis();
        boolean permanent = expiry == -1L;
        if (!permanent && expiry <= now) {
            return;
        }

        long remainingMillis = permanent ? -1L : expiry - now;
        String banMessage = BanScreen.render(plugin.getMainConfigManager(), action.staff(), action.reason(),
                remainingMillis);
        BanUtils.banIp(targetIp, banMessage, permanent ? null : new Date(expiry), action.staff());

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null)
                continue;
            String onlineIp = IPUtils.normalizeIp(IPUtils.resolvePlayerIp(online));
            if (targetIp.equals(onlineIp)) {
                online.kick(MessageUtils.toComponent(banMessage));
            }
        }
    }

    private record SyncAction(long id, String type, String targetUuid, String targetName, String reason, String staff,
            Long banExpiry) {
    }
}
