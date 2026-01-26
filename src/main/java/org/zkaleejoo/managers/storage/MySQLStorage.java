package org.zkaleejoo.managers.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MySQLStorage implements StorageProvider {

    private final MaxStaff plugin;
    private HikariDataSource dataSource;

    public MySQLStorage(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        MainConfigManager config = plugin.getMainConfigManager();
        
        HikariConfig hikariConfig = new HikariConfig();
        String jdbcUrl = "jdbc:mysql://" + config.getSqlHost() + ":" + config.getSqlPort() + "/" + config.getSqlDatabase() + "?" + config.getSqlProperties();
        
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(config.getSqlUser());
        hikariConfig.setPassword(config.getSqlPass());
        
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.setMaximumPoolSize(10);

        this.dataSource = new HikariDataSource(hikariConfig);
        createTables();
    }

    private void createTables() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS maxstaff_history (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "name VARCHAR(16), " +
                    "type VARCHAR(10), " +
                    "reason TEXT, " +
                    "staff VARCHAR(16), " +
                    "duration VARCHAR(32), " +
                    "date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS maxstaff_mutes (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "reason TEXT, " +
                    "expiry BIGINT, " +
                    "staff VARCHAR(16))");

            stmt.execute("CREATE TABLE IF NOT EXISTS maxstaff_ips (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "ip VARCHAR(45))");
            
            plugin.getLogger().info("Tablas de MySQL verificadas/creadas con éxito.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void logHistory(UUID uuid, String name, String type, String reason, String staff, String duration) {
        executeAsync("INSERT INTO maxstaff_history (uuid, name, type, reason, staff, duration) VALUES (?, ?, ?, ?, ?, ?)",
                uuid.toString(), name, type.toUpperCase(), reason, staff, duration);
    }

    @Override
    public int getHistoryCount(UUID uuid, String type) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM maxstaff_history WHERE uuid = ? AND type = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type.toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    @Override
    public List<String> getHistoryDetails(UUID uuid, String type) {
        List<String> details = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT date, staff, reason, duration FROM maxstaff_history WHERE uuid = ? AND type = ? ORDER BY date DESC")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type.toUpperCase());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                details.add(rs.getTimestamp("date").getTime() + "|" + rs.getString("staff") + "|" + rs.getString("reason") + "|" + rs.getString("duration"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return details;
    }

    @Override
    public void resetHistory(UUID uuid, String type) {
        if (type.equalsIgnoreCase("all")) {
            executeAsync("DELETE FROM maxstaff_history WHERE uuid = ?", uuid.toString());
        } else {
            executeAsync("DELETE FROM maxstaff_history WHERE uuid = ? AND type = ?", uuid.toString(), type.toUpperCase());
        }
    }

    @Override
    public boolean takeHistory(UUID uuid, String type, int amount) {
        executeAsync("DELETE FROM maxstaff_history WHERE uuid = ? AND type = ? ORDER BY date DESC LIMIT ?", 
                uuid.toString(), type.toUpperCase(), amount);
        return true;
    }

    @Override
    public void saveMute(UUID uuid, String reason, long expiry, String staff) {
        executeAsync("REPLACE INTO maxstaff_mutes (uuid, reason, expiry, staff) VALUES (?, ?, ?, ?)",
                uuid.toString(), reason, expiry, staff);
    }

    @Override
    public void removeMute(UUID uuid) {
        executeAsync("DELETE FROM maxstaff_mutes WHERE uuid = ?", uuid.toString());
    }

    @Override
    public CompletableFuture<Long> getMuteExpiry(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT expiry FROM maxstaff_mutes WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getLong("expiry");
            } catch (SQLException e) { e.printStackTrace(); }
            return 0L;
        });
    }

    @Override
    public void saveIP(UUID uuid, String ip) {
        executeAsync("REPLACE INTO maxstaff_ips (uuid, ip) VALUES (?, ?)", uuid.toString(), ip);
    }

    @Override
    public String getIP(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT ip FROM maxstaff_ips WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("ip");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public List<UUID> getAltsByIP(String ip) {
        List<UUID> alts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM maxstaff_ips WHERE ip = ?")) {
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                alts.add(UUID.fromString(rs.getString("uuid")));
            }
        } catch (SQLException | IllegalArgumentException e) { e.printStackTrace(); }
        return alts;
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private void executeAsync(String sql, Object... params) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}