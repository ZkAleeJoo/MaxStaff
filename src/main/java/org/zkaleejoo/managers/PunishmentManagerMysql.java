package org.zkaleejoo.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.BanUtils;
import org.zkaleejoo.utils.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.TimeUtils;

public class PunishmentManagerMysql extends AbstractPunishmentManager {

    private static final String SOUND_KICK_CONFIRM = "minecraft:entity.zombie.attack_iron_door";
    private static final String SOUND_BAN_CONFIRM = "minecraft:entity.lightning_bolt.thunder";
    private final DatabaseManager db;
    private final Map<UUID, Long> muteCacheValidation = new ConcurrentHashMap<>();
    private static final long MUTE_CACHE_VALIDATION_MS = 3_000L;
    private static final long MUTE_CACHE_RETENTION_MS = 6L * 60L * 60L * 1_000L;
    private static final long MUTE_CACHE_CLEANUP_INTERVAL_TICKS = 20L * 60L * 10L;
    private static final long SYNC_ACTION_TTL_MS = 30_000L;
    private final String syncSourceServer;
    private BukkitTask muteCacheCleanupTask;

    public PunishmentManagerMysql(MaxStaff plugin) {
        super(plugin);
        this.db = new DatabaseManager(plugin);
        this.syncSourceServer = plugin.getMainConfigManager().getDbServerId();
        loadMuteCache();
        startMuteCacheCleanupTask();
    }

    private void startMuteCacheCleanupTask() {
        muteCacheCleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupMuteCaches,
                MUTE_CACHE_CLEANUP_INTERVAL_TICKS, MUTE_CACHE_CLEANUP_INTERVAL_TICKS);
    }

    private void cleanupMuteCaches() {
        long now = System.currentTimeMillis();
        long staleBefore = now - MUTE_CACHE_RETENTION_MS;

        muteCacheValidation.entrySet().removeIf(entry -> entry.getValue() < staleBefore);

        muteCache().entrySet().removeIf(entry -> {
            Long expiry = entry.getValue();
            if (expiry == null || expiry == -1L) {
                return false;
            }

            if (expiry > now) {
                return false;
            }

            Long lastValidation = muteCacheValidation.get(entry.getKey());
            return lastValidation == null || lastValidation < staleBefore;
        });
    }

    private void loadMuteCache() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = db.getConnection();
                    PreparedStatement ps = conn.prepareStatement("SELECT uuid, expiry FROM maxstaff_mutes")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    muteCache().put(UUID.fromString(rs.getString("uuid")), rs.getLong("expiry"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void close() {
        if (muteCacheCleanupTask != null) {
            muteCacheCleanupTask.cancel();
            muteCacheCleanupTask = null;
        }
        db.close();
    }

    @Override
    protected UUID getUniqueId(String targetName) {
        Player onlineTarget = Bukkit.getPlayer(targetName);
        if (onlineTarget != null) {
            persistPlayerIdentity(onlineTarget.getUniqueId(), onlineTarget.getName());
            return onlineTarget.getUniqueId();
        }
        UUID cachedUuid = resolveUuidFromNameCache(targetName);
        if (cachedUuid != null) {
            return cachedUuid;
        }

        UUID punishmentUuid = resolveUuidFromPunishmentTables(targetName);
        if (punishmentUuid != null) {
            persistPlayerIdentity(punishmentUuid, targetName);
            return punishmentUuid;
        }

        return null;
    }

    private UUID resolveUuidFromNameCache(String targetName) {
        try (Connection conn = db.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT uuid FROM maxstaff_name_cache WHERE LOWER(last_name) = LOWER(?) ORDER BY updated_at DESC LIMIT 1")) {
            ps.setString(1, targetName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid"));
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            e.printStackTrace();
        }

        return null;
    }

    private UUID resolveUuidFromPunishmentTables(String targetName) {
        String[] queries = {
                "SELECT uuid FROM maxstaff_mutes WHERE LOWER(name) = LOWER(?) LIMIT 1",
                "SELECT uuid FROM maxstaff_bans WHERE LOWER(name) = LOWER(?) ORDER BY created_at DESC LIMIT 1",
                "SELECT uuid FROM maxstaff_history WHERE LOWER(name) = LOWER(?) ORDER BY created_at DESC LIMIT 1"
        };

        for (String query : queries) {
            try (Connection conn = db.getConnection();
                    PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, targetName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return UUID.fromString(rs.getString("uuid"));
                    }
                }
            } catch (SQLException | IllegalArgumentException ignored) {
            }
        }

        return null;
    }

    @Override
    protected String getStorageLogTag() {
        return "MaxStaff SQL";
    }

    @Override
    protected void executeAfterHistoryLog(Runnable action) {
        Bukkit.getScheduler().runTaskLater(plugin, action, 5L);
    }

    @Override
    protected void saveMute(UUID uuid, String reason, String staffName, long expiry) {
        String currentName = resolveLastKnownName(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = db.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO maxstaff_mutes (uuid, name, reason, staff, expiry) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE name=?, reason=?, staff=?, expiry=?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, currentName);
                ps.setString(3, reason);
                ps.setString(4, staffName);
                ps.setLong(5, expiry);
                ps.setString(6, currentName);
                ps.setString(7, reason);
                ps.setString(8, staffName);
                ps.setLong(9, expiry);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected boolean removeMuteByName(String targetName) {
        boolean removed = false;

        try (Connection conn = db.getConnection()) {
            try (PreparedStatement ps = conn
                    .prepareStatement("SELECT uuid FROM maxstaff_mutes WHERE LOWER(name) = LOWER(?)")) {
                ps.setString(1, targetName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            UUID uuid = UUID.fromString(rs.getString("uuid"));
                            muteCache().remove(uuid);
                            muteCacheValidation.remove(uuid);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }

            try (PreparedStatement ps = conn
                    .prepareStatement("DELETE FROM maxstaff_mutes WHERE LOWER(name) = LOWER(?)")) {
                ps.setString(1, targetName);
                removed = ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return removed;
    }

    @Override
    protected boolean removeMute(UUID uuid) {
        boolean existedInCache = muteCache().remove(uuid) != null;
        muteCacheValidation.remove(uuid);
        boolean existedInDatabase = false;

        try (Connection conn = db.getConnection();
                PreparedStatement check = conn
                        .prepareStatement("SELECT 1 FROM maxstaff_mutes WHERE uuid = ? LIMIT 1")) {
            check.setString(1, uuid.toString());
            try (ResultSet rs = check.executeQuery()) {
                existedInDatabase = rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return existedInCache;
        }

        if (!existedInCache && !existedInDatabase) {
            return false;
        }

        try (Connection conn = db.getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM maxstaff_mutes WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return existedInCache;
        }

        return true;
    }

    @Override
    public boolean isMuted(UUID uuid) {
        if (super.isMuted(uuid)) {
            long now = System.currentTimeMillis();
            Long lastValidation = muteCacheValidation.get(uuid);
            if (lastValidation != null && (now - lastValidation) < MUTE_CACHE_VALIDATION_MS) {
                return true;
            }

            Long expiryInCache = muteCache().get(uuid);
            if (expiryInCache == null) {
                return false;
            }

            Long databaseExpiry = getMuteExpiry(uuid);
            muteCacheValidation.put(uuid, now);
            if (databaseExpiry == null) {
                muteCache().remove(uuid);
                return false;
            }

            if (databaseExpiry != expiryInCache) {
                muteCache().put(uuid, databaseExpiry);
            }
            return true;
        }

        Long databaseExpiry = getMuteExpiry(uuid);
        if (databaseExpiry == null) {
            return false;
        }

        muteCache().put(uuid, databaseExpiry);
        muteCacheValidation.put(uuid, System.currentTimeMillis());
        return true;
    }

    private Long getMuteExpiry(UUID uuid) {
        try (Connection conn = db.getConnection();
                PreparedStatement ps = conn
                        .prepareStatement("SELECT expiry FROM maxstaff_mutes WHERE uuid = ? LIMIT 1")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                long expiry = rs.getLong("expiry");
                if (expiry != -1 && System.currentTimeMillis() > expiry) {
                    removeMuteAsync(uuid);
                    return null;
                }
                return expiry;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void kickPlayer(CommandSender staff, String targetName, String reason) {
        kickPlayerInternal(staff, targetName, reason, false);
    }

    @Override
    public void kickPlayerSilent(CommandSender staff, String targetName, String reason) {
        kickPlayerInternal(staff, targetName, reason, true);
    }

    private void kickPlayerInternal(CommandSender staff, String targetName, String reason, boolean silent) {
        if (isProtectedTarget(staff, targetName)) {
            return;
        }
        String actor = getActorName(staff);

        clearFreezeBeforeDisconnectPunishment(targetName, actor);

        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            logHistory(targetName, "KICK", reason, actor, "Ahora");

            String kickScreen = cfg().getScreenKick()
                    .replace("{staff}", actor)
                    .replace("{reason}", reason);
            target.kick(MessageUtils.toComponent(kickScreen));

            logStaffPunishment("KICK", actor, target.getName());

            if (!silent) {
                String bcMsg = cfg().getBcKick()
                        .replace("{target}", target.getName())
                        .replace("{staff}", actor)
                        .replace("{reason}", reason);
                broadcast(bcMsg);
            }

            plugin.getDiscordManager().sendWebhook("kick", target.getName(), actor, reason, "N/A", null);
        } else {
            enqueueSyncAction("KICK", null, targetName, reason, actor, null, SYNC_ACTION_TTL_MS);
            if (!silent) {
                staff.sendMessage(prefixed("&aKick global queue for &f" + targetName
                        + "&a. It will be applied on the network if you are connected."));
            }

            logStaffPunishment("KICK_GLOBAL", actor, targetName);
        }

        playStaffSound(staff, SOUND_KICK_CONFIRM, 1.0f, 0.5f);
    }

    @Override
    public void banPlayer(CommandSender staff, String targetName, String reason, String durationStr) {
        banPlayerInternal(staff, targetName, reason, durationStr, false);
    }

    @Override
    public void banPlayerSilent(CommandSender staff, String targetName, String reason, String durationStr) {
        banPlayerInternal(staff, targetName, reason, durationStr, true);
    }

    private void banPlayerInternal(CommandSender staff, String targetName, String reason, String durationStr,
            boolean silent) {
        if (isProtectedTarget(staff, targetName)) {
            return;
        }

        String actor = getActorName(staff);
        long duration = TimeUtils.parseDuration(durationStr);
        String timeDisplay = TimeUtils.getDurationString(duration, cfg());
        clearFreezeBeforeDisconnectPunishment(targetName, actor);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID uuid = getUniqueId(targetName);

            if (uuid == null) {
                if (!silent) {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> staff.sendMessage(prefixed(getPlayerNotFoundInCacheMessage(targetName))));
                }
                return;
            }

            long expiry = (duration == -1) ? -1 : System.currentTimeMillis() + duration;
            try (Connection conn = db.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO maxstaff_bans (uuid, name, reason, staff, expiry, created_at) VALUES (?, ?, ?, ?, ?, ?) "
                                    + "ON DUPLICATE KEY UPDATE name=?, reason=?, staff=?, expiry=?, created_at=?")) {
                long now = System.currentTimeMillis();
                ps.setString(1, uuid.toString());
                ps.setString(2, targetName);
                ps.setString(3, reason);
                ps.setString(4, actor);
                ps.setLong(5, expiry);
                ps.setLong(6, now);
                ps.setString(7, targetName);
                ps.setString(8, reason);
                ps.setString(9, actor);
                ps.setLong(10, expiry);
                ps.setLong(11, now);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                logHistory(targetName, "BAN", reason, actor, timeDisplay);

                String banScreenTemplate = cfg().getScreenBan().replace("{staff}", actor).replace("{reason}", reason)
                        .replace("{duration}", timeDisplay);
                String finalBanMessage = c(banScreenTemplate);

                Player target = Bukkit.getPlayer(targetName);
                if (target != null) {
                    target.kick(MessageUtils.toComponent(finalBanMessage));
                }

                logStaffPunishment("BAN", actor, targetName);

                if (!silent) {
                    String bcMsg = cfg().getBcBan().replace("{target}", targetName).replace("{staff}", actor)
                            .replace("{duration}", timeDisplay).replace("{reason}", reason);
                    broadcast(bcMsg);
                }

                playStaffSound(staff, SOUND_BAN_CONFIRM, 0.6f, 1.0f);

                enqueueSyncAction("BAN", uuid.toString(), targetName, reason, actor, timeDisplay, SYNC_ACTION_TTL_MS);
                plugin.getDiscordManager().sendWebhook("ban", targetName, actor, reason, timeDisplay, null);
            });
        });
    }

    @SuppressWarnings("null")
    @Override
    public void unbanPlayer(CommandSender staff, String targetName) {
        UUID directUuid = tryParseUuid(targetName);
        Player onlineTarget = Bukkit.getPlayer(targetName);
        UUID onlineUuid = onlineTarget != null ? onlineTarget.getUniqueId() : null;
        String actor = getActorName(staff);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID uuid = directUuid != null ? directUuid : onlineUuid;
            if (uuid == null) {
                uuid = resolveUuidFromNameCache(targetName);
            }
            if (uuid == null) {
                uuid = resolveUuidFromPunishmentTables(targetName);
            }
            if (uuid != null) {
                persistPlayerIdentity(uuid, targetName);
            }

            int rowsAffected = 0;
            try (Connection conn = db.getConnection()) {
                if (uuid != null) {
                    try (PreparedStatement ps = conn
                            .prepareStatement("DELETE FROM maxstaff_bans WHERE uuid = ? OR LOWER(name) = LOWER(?)")) {
                        ps.setString(1, uuid.toString());
                        ps.setString(2, targetName);
                        rowsAffected = ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn
                            .prepareStatement("DELETE FROM maxstaff_bans WHERE LOWER(name) = LOWER(?)")) {
                        ps.setString(1, targetName);
                        rowsAffected = ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            UUID finalUuid = uuid;
            int finalRowsAffected = rowsAffected;
            Bukkit.getScheduler().runTask(plugin, () -> {
                boolean hadLocalBan = BanUtils.isPlayerNameBanned(targetName);
                BanUtils.pardonPlayerName(targetName);
                if (finalUuid != null) {
                    BanUtils.pardonPlayer(finalUuid, directUuid != null ? null : targetName);
                }

                if (finalRowsAffected > 0 || hadLocalBan) {
                    String feedbackTarget = targetName;
                    if (directUuid != null) {
                        feedbackTarget = finalUuid.toString();
                    }

                    staff.sendMessage(prefixed(cfg().getMsgUnbanSuccess().replace("{target}", feedbackTarget)));
                    logStaffPunishment("UNBAN", actor, targetName);
                    enqueueSyncAction("UNBAN", finalUuid != null ? finalUuid.toString() : null, targetName, null, actor, null,
                            SYNC_ACTION_TTL_MS);
                } else {
                    staff.sendMessage(prefixed("&cThe player &f" + targetName + " &cwas not banned."));
                }
            });
        });
    }

    public void cleanupExpiredPunishments(CommandSender requester) {
        long now = System.currentTimeMillis();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int bansRemoved = 0;
            int mutesRemoved = 0;
            int ipBansRemoved = 0;

            try (Connection conn = db.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM maxstaff_bans WHERE expiry <> -1 AND expiry <= ?")) {
                    ps.setLong(1, now);
                    bansRemoved = ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM maxstaff_mutes WHERE expiry <> -1 AND expiry <= ?")) {
                    ps.setLong(1, now);
                    mutesRemoved = ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM maxstaff_ip_bans WHERE expiry <> -1 AND expiry <= ?")) {
                    ps.setLong(1, now);
                    ipBansRemoved = ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[MaxStaff SQL] cleanupExpiredPunishments failed: " + e.getMessage());
            }

            int finalBansRemoved = bansRemoved;
            int finalMutesRemoved = mutesRemoved;
            int finalIpBansRemoved = ipBansRemoved;
            Bukkit.getScheduler().runTask(plugin, () -> {
                requester.sendMessage(prefixed("&aCleanup complete. Removed &f" + finalBansRemoved + " &aexpired bans, &f"
                        + finalMutesRemoved + " &aexpired mutes, &f" + finalIpBansRemoved + " &aexpired IP bans."));
            });
        });
    }

    private UUID tryParseUuid(String input) {
        if (input == null) {
            return null;
        }

        String trimmed = input.trim();
        if (trimmed.length() < 32) {
            return null;
        }

        try {
            return UUID.fromString(trimmed);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public void unmutePlayer(CommandSender staff, String targetName) {
        UUID uuid = getUniqueId(targetName);
        super.unmutePlayer(staff, targetName);
        if (uuid != null) {
            muteCacheValidation.remove(uuid);
            logStaffPunishment("UNMUTE", getActorName(staff), targetName);
            enqueueSyncAction("UNMUTE", uuid.toString(), targetName, null, getActorName(staff), null,
                    SYNC_ACTION_TTL_MS);
        }
    }

    @Override
    public void mutePlayer(CommandSender staff, String targetName, String reason, String durationStr) {
        super.mutePlayer(staff, targetName, reason, durationStr);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID uuid = getUniqueId(targetName);
            if (uuid != null) {
                enqueueSyncAction("MUTE", uuid.toString(), targetName, reason, getActorName(staff), durationStr,
                        SYNC_ACTION_TTL_MS);
            }
        });
    }

    @Override
    public void mutePlayerSilent(CommandSender staff, String targetName, String reason, String durationStr) {
        super.mutePlayerSilent(staff, targetName, reason, durationStr);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID uuid = getUniqueId(targetName);
            if (uuid != null) {
                enqueueSyncAction("MUTE", uuid.toString(), targetName, reason, getActorName(staff), durationStr,
                        SYNC_ACTION_TTL_MS);
            }
        });
    }

    @Override
    public List<String> getBannedPlayerNames() {
        List<String> names = new ArrayList<>();
        try (Connection conn = db.getConnection();
                PreparedStatement ps = conn
                        .prepareStatement("SELECT uuid, name, expiry FROM maxstaff_bans ORDER BY created_at DESC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long expiry = rs.getLong("expiry");
                    if (expiry != -1 && System.currentTimeMillis() > expiry) {
                        removeExpiredBan(UUID.fromString(rs.getString("uuid")));
                        continue;
                    }

                    String name = rs.getString("name");
                    if (name != null && !name.isEmpty()) {
                        names.add(name);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return names;
    }

    @Override
    public List<ActivePunishmentRecord> getActivePunishments() {
        List<ActivePunishmentRecord> records = new ArrayList<>();
        long now = System.currentTimeMillis();

        try (Connection conn = db.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT uuid, name, reason, staff, expiry FROM maxstaff_bans ORDER BY created_at DESC");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long expiry = rs.getLong("expiry");
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    if (expiry != -1L && expiry <= now) {
                        removeExpiredBan(uuid);
                        continue;
                    }

                    records.add(new ActivePunishmentRecord(
                            ActivePunishmentRecord.Type.BAN,
                            rs.getString("name"),
                            rs.getString("staff"),
                            rs.getString("reason"),
                            expiry));
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT ip, reason, staff, expiry FROM maxstaff_ip_bans ORDER BY created_at DESC");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long expiry = rs.getLong("expiry");
                    String ip = rs.getString("ip");
                    if (expiry != -1L && expiry <= now) {
                        removeExpiredIpBan(ip);
                        continue;
                    }

                    records.add(new ActivePunishmentRecord(
                            ActivePunishmentRecord.Type.IP_BAN,
                            ip,
                            rs.getString("staff"),
                            rs.getString("reason"),
                            expiry));
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT uuid, name, reason, staff, expiry FROM maxstaff_mutes ORDER BY name ASC");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long expiry = rs.getLong("expiry");
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    if (expiry != -1L && expiry <= now) {
                        removeMuteAsync(uuid);
                        continue;
                    }

                    String name = rs.getString("name");
                    if (name == null || name.isBlank()) {
                        name = resolveLastKnownName(uuid);
                    }

                    records.add(new ActivePunishmentRecord(
                            ActivePunishmentRecord.Type.MUTE,
                            name == null || name.isBlank() ? uuid.toString() : name,
                            rs.getString("staff"),
                            rs.getString("reason"),
                            expiry));
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            e.printStackTrace();
        }

        return records;
    }

    public String getActiveBanMessage(UUID uuid, String targetName) {
        try (Connection conn = db.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT reason, staff, expiry FROM maxstaff_bans WHERE uuid = ? OR LOWER(name) = LOWER(?) LIMIT 1")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, targetName);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                long expiry = rs.getLong("expiry");
                if (expiry != -1 && System.currentTimeMillis() > expiry) {
                    removeExpiredBan(uuid);
                    return null;
                }

                String duration = (expiry == -1)
                        ? TimeUtils.getDurationString(-1, cfg())
                        : TimeUtils.getDurationString(Math.max(0, expiry - System.currentTimeMillis()), cfg());

                return MessageUtils.getColoredMessage(cfg().getScreenBan()
                        .replace("{staff}", rs.getString("staff"))
                        .replace("{reason}", rs.getString("reason"))
                        .replace("{duration}", duration));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void removeExpiredBan(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = db.getConnection();
                    PreparedStatement ps = conn.prepareStatement("DELETE FROM maxstaff_bans WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void removeMuteAsync(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = db.getConnection();
                    PreparedStatement ps = conn.prepareStatement("DELETE FROM maxstaff_mutes WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void logHistory(String targetName, String type, String reason, String staff, String duration) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID uuid = getUniqueId(targetName);

            if (uuid == null) {
                plugin.getLogger().warning("Skipping history log for unknown cached target: " + targetName);
                return;
            }

            long createdAt = System.currentTimeMillis();
            String cleanDuration = (duration == null || duration.isEmpty()) ? "N/A" : duration;

            try (Connection conn = db.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO maxstaff_history (uuid, name, type, reason, staff, duration, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, targetName);
                ps.setString(3, type.toUpperCase());
                ps.setString(4, reason);
                ps.setString(5, staff);
                ps.setString(6, cleanDuration);
                ps.setLong(7, createdAt);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public int getHistoryCount(String targetName, String type) {
        UUID uuid = getUniqueId(targetName);
        if (uuid == null) {
            return 0;
        }
        try (Connection conn = db.getConnection();
                PreparedStatement ps = conn
                        .prepareStatement("SELECT COUNT(*) FROM maxstaff_history WHERE uuid = ? AND type = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type.toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public List<String> getHistoryDetails(String targetName, String type) {
        UUID uuid = getUniqueId(targetName);
        if (uuid == null) {
            return new ArrayList<>();
        }
        List<String> details = new ArrayList<>();
        try (Connection conn = db.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT created_at, staff, reason, duration FROM maxstaff_history WHERE uuid = ? AND type = ? ORDER BY created_at DESC, id DESC")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type.toUpperCase());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String record = formatCreatedAt(rs.getLong("created_at")) + "|"
                        + rs.getString("staff") + "|"
                        + rs.getString("reason") + "|"
                        + rs.getString("duration");
                details.add(record);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return details;
    }

    private String formatCreatedAt(long createdAt) {
        if (createdAt <= 0) {
            return "N/A";
        }
        return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(createdAt));
    }

    private void playStaffSound(CommandSender staff, String soundName, float volume, float pitch) {
        if (staff instanceof Player player && soundName != null && !soundName.isEmpty()) {
            player.playSound(player, soundName, volume, pitch);
        }
    }

    @Override
    public void resetHistory(String targetName, String type) {
        UUID uuid = getUniqueId(targetName);
        if (uuid == null) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = db.getConnection()) {
                if (type.equalsIgnoreCase("all")) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM maxstaff_history WHERE uuid = ?")) {
                        ps.setString(1, uuid.toString());
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn
                            .prepareStatement("DELETE FROM maxstaff_history WHERE uuid = ? AND type = ?")) {
                        ps.setString(1, uuid.toString());
                        ps.setString(2, type.toUpperCase());
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public boolean takeHistory(String targetName, String type, int amount) {
        UUID uuid = getUniqueId(targetName);
        if (uuid == null) {
            return false;
        }
        int current = getHistoryCount(targetName, type);
        if (current <= 0) {
            return false;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = db.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM maxstaff_history WHERE uuid = ? AND type = ? ORDER BY id DESC LIMIT ?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, type.toUpperCase());
                ps.setInt(3, amount);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return true;
    }

    @Override
    protected void persistPlayerIP(UUID uuid, String ip) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = db.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO maxstaff_ip_cache (uuid, ip) VALUES (?, ?) ON DUPLICATE KEY UPDATE ip=?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, ip);
                ps.setString(3, ip);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected String getCachedIP(UUID uuid) {
        try (Connection conn = db.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT ip FROM maxstaff_ip_cache WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("ip");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getPlayerIP(String targetName) {
        if (isLiteralIp(targetName)) {
            return normalizeIp(targetName);
        }

        String ip = super.getPlayerIP(targetName);
        if (ip != null && !ip.isEmpty()) {
            return normalizeIp(ip);
        }

        return normalizeIp(resolveIpFromNameFallback(targetName));
    }

    private String resolveIpFromNameFallback(String targetName) {
        String byNameCache = getLatestIpByKnownName(targetName);
        if (byNameCache != null && !byNameCache.isEmpty()) {
            return byNameCache;
        }

        return getIpFromIpBansAssociation(targetName);
    }

    private String getLatestIpByKnownName(String targetName) {
        String sql = "SELECT ic.ip " +
                "FROM maxstaff_name_cache nc " +
                "JOIN maxstaff_ip_cache ic ON ic.uuid = nc.uuid " +
                "WHERE LOWER(nc.last_name) = LOWER(?) " +
                "ORDER BY nc.updated_at DESC LIMIT 1";

        try (Connection conn = db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, targetName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ip");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getIpFromIpBansAssociation(String targetName) {
        String sql = "SELECT ib.ip " +
                "FROM maxstaff_ip_bans ib " +
                "JOIN maxstaff_ip_cache ic ON ic.ip = ib.ip " +
                "JOIN maxstaff_name_cache nc ON nc.uuid = ic.uuid " +
                "WHERE LOWER(nc.last_name) = LOWER(?) " +
                "ORDER BY ib.created_at DESC LIMIT 1";

        try (Connection conn = db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, targetName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ip");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<UUID> getAllAccountsByIP(String ip) {
        List<UUID> alts = new ArrayList<>();
        try (Connection conn = db.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM maxstaff_ip_cache WHERE ip = ?")) {
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                alts.add(UUID.fromString(rs.getString("uuid")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alts;
    }

    @Override
    public void banIPPlayer(CommandSender staff, String target, String reason, String durationStr) {
        banIPPlayerInternal(staff, target, reason, durationStr, false);
    }

    @Override
    public void banIPPlayerSilent(CommandSender staff, String target, String reason, String durationStr) {
        banIPPlayerInternal(staff, target, reason, durationStr, true);
    }

    private void banIPPlayerInternal(CommandSender staff, String target, String reason, String durationStr,
            boolean silent) {
        boolean literalIp = isLiteralIp(target);
        if (!literalIp && isProtectedTarget(staff, target)) {
            return;
        }
        String actor = getActorName(staff);

        String ip = literalIp ? normalizeIp(target) : getPlayerIP(target);
        if (ip == null || ip.isEmpty()) {
            if (!silent) {
                staff.sendMessage(
                        prefixed(cfg().getMsgNoIPFound() + " &7(Target: &f" + target + "&7) &cUse literal IP."));
            }
            return;
        }

        long duration = TimeUtils.parseDuration(durationStr);
        String timeDisplay = TimeUtils.getDurationString(duration, cfg());
        long expiry = (duration == -1) ? -1 : System.currentTimeMillis() + duration;

        logHistory(target, "BAN-IP", reason, actor, timeDisplay);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = db.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO maxstaff_ip_bans (ip, reason, staff, expiry, created_at) VALUES (?, ?, ?, ?, ?) "
                                    +
                                    "ON DUPLICATE KEY UPDATE reason=?, staff=?, expiry=?, created_at=?")) {
                long now = System.currentTimeMillis();
                ps.setString(1, ip);
                ps.setString(2, reason);
                ps.setString(3, actor);
                ps.setLong(4, expiry);
                ps.setLong(5, now);
                ps.setString(6, reason);
                ps.setString(7, actor);
                ps.setLong(8, expiry);
                ps.setLong(9, now);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        String banMessage = c(cfg().getScreenBan()
                .replace("{staff}", actor)
                .replace("{reason}", reason)
                .replace("{duration}", timeDisplay));

        BanUtils.banIp(ip, banMessage, (expiry == -1 ? null : new java.util.Date(expiry)), actor);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null) continue;
            String onlineIp = resolveIp(online);
            if (ip.equals(onlineIp)) {
                online.kick(MessageUtils.toComponent(banMessage));
            }
        }

        if (!silent) {
            String bcMsg = cfg().getBcBanIP()
                    .replace("{target}", target)
                    .replace("{staff}", actor)
                    .replace("{duration}", timeDisplay);
            broadcast(bcMsg);
        }

        logStaffPunishment("BAN_IP", actor, ip);
        enqueueSyncAction("BAN_IP", null, ip, reason, actor, timeDisplay, SYNC_ACTION_TTL_MS);
        plugin.getDiscordManager().sendWebhook("ban", target + " (IP)", actor, reason, timeDisplay, null);
    }

    @Override
    public void unbanIPPlayer(CommandSender staff, String target) {
        String ip = resolveUnbanIpTarget(target);
        if (ip == null) {
            staff.sendMessage(prefixed(cfg().getMsgInvalidIP().replace("{target}", target)
                    + " &7Could not resolve IP from cache/DB, use literal IP."));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int rowsAffected = 0;
            try (Connection conn = db.getConnection();
                    PreparedStatement ps = conn.prepareStatement("DELETE FROM maxstaff_ip_bans WHERE ip = ?")) {
                ps.setString(1, ip);
                rowsAffected = ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            int finalRowsAffected = rowsAffected;
            String actor = getActorName(staff);
            Bukkit.getScheduler().runTask(plugin, () -> {
                boolean hadLocalBan = BanUtils.isIpBanned(ip);
                BanUtils.pardonIp(ip);

                if (finalRowsAffected > 0 || hadLocalBan) {
                    logStaffPunishment("UNBAN_IP", actor, ip);
                    enqueueSyncAction("UNBAN_IP", null, ip, null, actor, null, SYNC_ACTION_TTL_MS);
                    staff.sendMessage(prefixed(cfg().getMsgUnbanIPSuccess().replace("{ip}", ip)));
                } else {
                    staff.sendMessage(prefixed("&cThe IP &f" + ip + " &cwas not banned."));
                }
            });
        });
    }

    private String resolveUnbanIpTarget(String target) {
        if (isLiteralIp(target)) {
            return normalizeIp(target);
        }

        String ip = getPlayerIP(target);
        if (ip != null && !ip.isEmpty()) {
            return normalizeIp(ip);
        }

        return normalizeIp(getIpFromIpBansAssociation(target));
    }

    public String getActiveIPBanMessage(String ip) {
        try (Connection conn = db.getConnection();
                PreparedStatement ps = conn
                        .prepareStatement("SELECT reason, staff, expiry FROM maxstaff_ip_bans WHERE ip = ? LIMIT 1")) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                long expiry = rs.getLong("expiry");
                if (expiry != -1 && System.currentTimeMillis() > expiry) {
                    removeExpiredIpBan(ip);
                    return null;
                }

                String duration = (expiry == -1)
                        ? TimeUtils.getDurationString(-1, cfg())
                        : TimeUtils.getDurationString(Math.max(0, expiry - System.currentTimeMillis()), cfg());

                return c(cfg().getScreenBan()
                        .replace("{staff}", rs.getString("staff"))
                        .replace("{reason}", rs.getString("reason"))
                        .replace("{duration}", duration));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void removeExpiredIpBan(String ip) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = db.getConnection();
                    PreparedStatement ps = conn.prepareStatement("DELETE FROM maxstaff_ip_bans WHERE ip = ?")) {
                ps.setString(1, ip);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public String getSyncSourceServer() {
        return syncSourceServer;
    }

    public List<String> getActiveIpBans() {
        List<String> activeIpBans = new ArrayList<>();
        try (Connection conn = db.getConnection();
                PreparedStatement ps = conn
                        .prepareStatement("SELECT ip, expiry FROM maxstaff_ip_bans ORDER BY created_at DESC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long expiry = rs.getLong("expiry");
                    String ip = rs.getString("ip");
                    if (expiry != -1 && System.currentTimeMillis() > expiry) {
                        removeExpiredIpBan(ip);
                        continue;
                    }

                    if (ip != null && !ip.isEmpty()) {
                        activeIpBans.add(ip);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return activeIpBans;
    }

    public Connection getSqlConnection() throws SQLException {
        return db.getConnection();
    }

    public void clearMuteCacheFor(UUID uuid) {
        if (uuid == null) {
            return;
        }
        muteCache().remove(uuid);
        muteCacheValidation.remove(uuid);
    }

    private void enqueueSyncAction(String actionType, String targetUuid, String targetName, String reason, String staff,
            String duration, long ttlMs) {
        long now = System.currentTimeMillis();
        long expiresAt = now + ttlMs;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = db.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO maxstaff_sync_actions (action_type, target_uuid, target_name, reason, staff, duration, created_at, expires_at, source_server) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, actionType);
                ps.setString(2, targetUuid);
                ps.setString(3, targetName);
                ps.setString(4, reason);
                ps.setString(5, staff);
                ps.setString(6, duration);
                ps.setLong(7, now);
                ps.setLong(8, expiresAt);
                ps.setString(9, syncSourceServer);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void persistPlayerIdentity(UUID uuid, String name) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = db.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO maxstaff_name_cache (uuid, last_name, updated_at) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE last_name = VALUES(last_name), updated_at = VALUES(updated_at)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public List<String> getMutedPlayerNames() {
        List<String> names = new ArrayList<>();
        try (Connection conn = db.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT uuid, name, expiry FROM maxstaff_mutes")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long expiry = rs.getLong("expiry");
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    if (expiry != -1 && System.currentTimeMillis() > expiry) {
                        removeMuteAsync(uuid);
                        continue;
                    }

                    String name = rs.getString("name");
                    if (name == null || name.isEmpty()) {
                        name = resolveLastKnownName(uuid);
                    }

                    if (name != null && !name.isEmpty()) {
                        names.add(name);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return names;
    }

    private String resolveLastKnownName(UUID uuid) {
        Player onlineTarget = Bukkit.getPlayer(uuid);
        if (onlineTarget != null) {
            return onlineTarget.getName();
        }

        try (Connection conn = db.getConnection();
                PreparedStatement ps = conn
                        .prepareStatement("SELECT last_name FROM maxstaff_name_cache WHERE uuid = ? LIMIT 1")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("last_name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
