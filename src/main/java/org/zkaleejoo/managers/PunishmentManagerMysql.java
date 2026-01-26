package org.zkaleejoo.managers;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.DatabaseManager;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.TimeUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PunishmentManagerMysql implements IPunishmentManager {

    private final MaxStaff plugin;
    private final DatabaseManager db;
    private final Map<UUID, Long> muteCache = new ConcurrentHashMap<>();

    public PunishmentManagerMysql(MaxStaff plugin) {
        this.plugin = plugin;
        this.db = new DatabaseManager(plugin);
        loadMuteCache();
    }

    private void loadMuteCache() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = db.getConnection(); 
                 PreparedStatement ps = conn.prepareStatement("SELECT uuid, expiry FROM maxstaff_mutes")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    muteCache.put(UUID.fromString(rs.getString("uuid")), rs.getLong("expiry"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void close() {
        db.close();
    }

    private UUID getUniqueId(String targetName) {
        Player onlineTarget = Bukkit.getPlayer(targetName);
        if (onlineTarget != null) {
            return onlineTarget.getUniqueId();
        }
        return Bukkit.getOfflinePlayer(targetName).getUniqueId();
    }

    @Override
    public void logHistory(String targetName, String type, String reason, String staff, String duration) {
        UUID uuid = getUniqueId(targetName);
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        String cleanDuration = (duration == null || duration.isEmpty()) ? "N/A" : duration;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = db.getConnection(); 
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO maxstaff_history (uuid, name, type, reason, staff, duration, date) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, targetName);
                ps.setString(3, type.toUpperCase());
                ps.setString(4, reason);
                ps.setString(5, staff);
                ps.setString(6, cleanDuration);
                ps.setString(7, date);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public int getHistoryCount(String targetName, String type) {
        UUID uuid = getUniqueId(targetName);
        try (Connection conn = db.getConnection(); 
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM maxstaff_history WHERE uuid = ? AND type = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type.toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public List<String> getHistoryDetails(String targetName, String type) {
        UUID uuid = getUniqueId(targetName);
        List<String> details = new ArrayList<>();
        try (Connection conn = db.getConnection(); 
             PreparedStatement ps = conn.prepareStatement("SELECT date, staff, reason, duration FROM maxstaff_history WHERE uuid = ? AND type = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type.toUpperCase());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String record = rs.getString("date") + "|" + 
                                rs.getString("staff") + "|" + 
                                rs.getString("reason") + "|" + 
                                rs.getString("duration");
                details.add(record);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return details;
    }

    // --- KICK ---
    @Override
    public void kickPlayer(CommandSender staff, String targetName, String reason) {
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            logHistory(targetName, "KICK", reason, staff.getName(), "Ahora");
            
            String kickScreen = plugin.getMainConfigManager().getScreenKick()
                    .replace("{staff}", staff.getName())
                    .replace("{reason}", reason);
            target.kickPlayer(MessageUtils.getColoredMessage(kickScreen));
        
            Bukkit.getConsoleSender().sendMessage(org.bukkit.ChatColor.GRAY + "[MaxStaff SQL] The Staff " + staff.getName() + " has kicked " + targetName);
            
            String bcMsg = plugin.getMainConfigManager().getBcKick()
                .replace("{target}", target.getName())
                .replace("{staff}", staff.getName())
                .replace("{reason}", reason);
            broadcast(bcMsg);

            if (staff instanceof Player) {
                ((Player) staff).playSound(((Player) staff).getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 0.5f);
            }
            plugin.getDiscordManager().sendWebhook("kick", targetName, staff.getName(), reason, "N/A", null);
        } else {
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgOffline()));
        }
    }

    // --- BAN ---
    @Override
    public void banPlayer(CommandSender staff, String targetName, String reason, String durationStr) {
        long duration = TimeUtils.parseDuration(durationStr);
        String timeDisplay = TimeUtils.getDurationString(duration, plugin.getMainConfigManager());
        
        logHistory(targetName, "BAN", reason, staff.getName(), timeDisplay);

        Date expiry = (duration == -1) ? null : new Date(System.currentTimeMillis() + duration);
        String banScreenTemplate = plugin.getMainConfigManager().getScreenBan()
                .replace("{staff}", staff.getName())
                .replace("{reason}", reason)
                .replace("{duration}", timeDisplay);
        
        String finalBanMessage = MessageUtils.getColoredMessage(banScreenTemplate);
        
        Bukkit.getBanList(BanList.Type.NAME).addBan(targetName, finalBanMessage, expiry, staff.getName());

        Bukkit.getConsoleSender().sendMessage(org.bukkit.ChatColor.GRAY + "[MaxStaff SQL] The Staff " + staff.getName() + " has banned " + targetName);
        
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            target.kickPlayer(finalBanMessage);
        }

        String bcMsg = plugin.getMainConfigManager().getBcBan()
                .replace("{target}", targetName)
                .replace("{staff}", staff.getName())
                .replace("{duration}", timeDisplay)
                .replace("{reason}", reason);
        broadcast(bcMsg);

        if (staff instanceof Player) {
            ((Player) staff).playSound(((Player) staff).getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.0f);
        }

        plugin.getDiscordManager().sendWebhook("ban", targetName, staff.getName(), reason, timeDisplay, null);
    }

    @Override
    public void unbanPlayer(CommandSender staff, String targetName) {
        Bukkit.getBanList(BanList.Type.NAME).pardon(targetName);
        staff.sendMessage(MessageUtils.getColoredMessage(
            plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgUnbanSuccess().replace("{target}", targetName)
        ));
    }

    // --- MUTE ---
    @Override
    public void mutePlayer(CommandSender staff, String targetName, String reason, String durationStr) {
        UUID uuid = getUniqueId(targetName);
        long duration = TimeUtils.parseDuration(durationStr);
        String timeDisplay = TimeUtils.getDurationString(duration, plugin.getMainConfigManager());
        
        logHistory(targetName, "MUTE", reason, staff.getName(), timeDisplay);

        long expiry = (duration == -1) ? -1 : System.currentTimeMillis() + duration;
        
        muteCache.put(uuid, expiry);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = db.getConnection(); 
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO maxstaff_mutes (uuid, reason, staff, expiry) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE reason=?, staff=?, expiry=?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, reason);
                ps.setString(3, staff.getName());
                ps.setLong(4, expiry);
                ps.setString(5, reason);
                ps.setString(6, staff.getName());
                ps.setLong(7, expiry);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            String muteScreen = plugin.getMainConfigManager().getScreenMute().replace("{staff}", staff.getName());
            target.sendMessage(MessageUtils.getColoredMessage(muteScreen));
        }

        String bcMsg = plugin.getMainConfigManager().getBcMute()
                .replace("{target}", targetName)
                .replace("{staff}", staff.getName())
                .replace("{duration}", timeDisplay)
                .replace("{reason}", reason);
        broadcast(bcMsg);

        if (staff instanceof Player) {
            ((Player) staff).playSound(((Player) staff).getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 1.2f);
        }

        plugin.getDiscordManager().sendWebhook("mute", targetName, staff.getName(), reason, timeDisplay, null);
    }

    @Override
    public void unmutePlayer(CommandSender staff, String targetName) {
        UUID uuid = getUniqueId(targetName);

        if (muteCache.containsKey(uuid)) {
            muteCache.remove(uuid);
            
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try (Connection conn = db.getConnection(); 
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM maxstaff_mutes WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

            staff.sendMessage(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgUnmuteSuccess().replace("{target}", targetName)
            ));
            
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) target.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getScreenUnmute()));
        } else {
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgNotMuted()));
        }
    }

    @Override
    public boolean isMuted(UUID uuid) {
        if (!muteCache.containsKey(uuid)) return false;

        long expiry = muteCache.get(uuid);
        
        if (expiry != -1 && System.currentTimeMillis() > expiry) {
            muteCache.remove(uuid);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try (Connection conn = db.getConnection(); 
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM maxstaff_mutes WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
            return false;
        }
        return true;
    }

    // --- WARN ---
    @Override
    public void warnPlayer(CommandSender staff, String targetName, String reason) {
        logHistory(targetName, "WARN", reason, staff.getName(), "N/A"); 
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int count = getHistoryCount(targetName, "WARN");
            
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                target.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgWarnReceived().replace("{reason}", reason)));
            }

            String bcMsg = plugin.getMainConfigManager().getBcWarn()
                    .replace("{target}", targetName)
                    .replace("{staff}", staff.getName())
                    .replace("{count}", String.valueOf(count))
                    .replace("{reason}", reason);
            broadcast(bcMsg);

            checkWarnThresholds(targetName, count);
            plugin.getDiscordManager().sendWebhook("warn", targetName, staff.getName(), reason, null, String.valueOf(count));
        }, 5L); 
    }

    private void checkWarnThresholds(String targetName, int count) {
        ConfigurationSection thresholds = plugin.getMainConfigManager().getWarnThresholds();
        if (thresholds == null) return;

        String key = String.valueOf(count);
        if (thresholds.contains(key)) {
            String command = thresholds.getString(key);
            if (command != null) {
                String finalCmd = command.replace("{target}", targetName);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
            }
        }
    }

    @Override
    public void resetHistory(String targetName, String type) {
        UUID uuid = getUniqueId(targetName);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = db.getConnection()) {
                if (type.equalsIgnoreCase("all")) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM maxstaff_history WHERE uuid = ?")) {
                        ps.setString(1, uuid.toString());
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM maxstaff_history WHERE uuid = ? AND type = ?")) {
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
        int current = getHistoryCount(targetName, type);
        if (current <= 0) return false;

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
    public List<String> getBannedPlayerNames() {
        return Bukkit.getBanList(org.bukkit.BanList.Type.NAME).getBanEntries().stream()
                .map(org.bukkit.BanEntry::getTarget)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getMutedPlayerNames() {
        List<String> names = new ArrayList<>();
        for (UUID uuid : muteCache.keySet()) {
            if (isMuted(uuid)) {
                names.add(Bukkit.getOfflinePlayer(uuid).getName());
            }
        }
        return names;
    }

    @Override
    public void savePlayerIP(UUID uuid, String ip) {
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
    public String getPlayerIP(String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) return target.getAddress().getAddress().getHostAddress();
        
        UUID uuid = getUniqueId(targetName);
        try (Connection conn = db.getConnection(); 
             PreparedStatement ps = conn.prepareStatement("SELECT ip FROM maxstaff_ip_cache WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("ip");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void banIPPlayer(CommandSender staff, String target, String reason, String durationStr) {
        String ip = target; 
        if (!target.contains(".")) {
            ip = getPlayerIP(target);
        }
        
        if (ip == null || ip.isEmpty()) {
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgNoIPFound()));
            return;
        }

        long duration = TimeUtils.parseDuration(durationStr);
        String timeDisplay = TimeUtils.getDurationString(duration, plugin.getMainConfigManager());
        Date expiry = (duration == -1) ? null : new Date(System.currentTimeMillis() + duration);

        logHistory(target, "BAN-IP", reason, staff.getName(), timeDisplay);

        String banMessage = MessageUtils.getColoredMessage(plugin.getMainConfigManager().getScreenBan()
                .replace("{staff}", staff.getName())
                .replace("{reason}", reason)
                .replace("{duration}", timeDisplay));

        Bukkit.getBanList(BanList.Type.IP).addBan(ip, banMessage, expiry, staff.getName());

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getAddress().getAddress().getHostAddress().equals(ip)) {
                online.kickPlayer(banMessage);
            }
        }

        if (plugin.getMainConfigManager().isBroadcastEnabled()) {
            String bcMsg = plugin.getMainConfigManager().getBcBanIP()
                    .replace("{target}", target)
                    .replace("{staff}", staff.getName())
                    .replace("{duration}", timeDisplay);
            broadcast(bcMsg); 
        }

        plugin.getDiscordManager().sendWebhook("ban", target + " (IP)", staff.getName(), reason, timeDisplay, null);
    }

    @Override
    public void unbanIPPlayer(CommandSender staff, String target) {
        String ip = target;
        if (!target.contains(".")) {
            ip = getPlayerIP(target);
        }
        if (ip == null) {
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgInvalidIP().replace("{target}", target)));
            return;
        }
        Bukkit.unbanIP(ip);
        staff.sendMessage(MessageUtils.getColoredMessage(
            plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgUnbanIPSuccess().replace("{ip}", ip)
        ));
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

    private void broadcast(String msg) {
        if (plugin.getMainConfigManager().isBroadcastEnabled()) {
            MessageUtils.broadcastToPlayersOnly(msg);
        }
    }
}