package org.zkaleejoo.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.zkaleejoo.MaxStaff;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final MaxStaff plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(MaxStaff plugin) {
        this.plugin = plugin;
        if (plugin.getMainConfigManager().isSqlEnabled()) {
            setupPool();
            createTables();
        }
    }

    private void setupPool() {
        var config = plugin.getMainConfigManager();
        HikariConfig hikariConfig = new HikariConfig();
        
        String host = plugin.getConfig().getString("database.host");
        String port = plugin.getConfig().getString("database.port");
        String name = plugin.getConfig().getString("database.name");

        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + name);
        hikariConfig.setUsername(plugin.getConfig().getString("database.user"));
        hikariConfig.setPassword(plugin.getConfig().getString("database.password"));
        
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        this.dataSource = new HikariDataSource(hikariConfig);
    }

    private void createTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS ms_history (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "uuid VARCHAR(36)," +
                    "type VARCHAR(10)," +
                    "reason TEXT," +
                    "staff VARCHAR(16)," +
                    "duration VARCHAR(32)," +
                    "date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS ms_mutes (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "expiry LONG," +
                    "reason TEXT," +
                    "staff VARCHAR(16))");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource != null ? dataSource.getConnection() : null;
    }

    public void close() {
        if (dataSource != null) dataSource.close();
    }
}