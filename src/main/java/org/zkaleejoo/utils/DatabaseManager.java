package org.zkaleejoo.utils;

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
        connect();
        setupTables();
    }

    private void connect() {
        HikariConfig config = new HikariConfig();
        String host = plugin.getMainConfigManager().getDbHost();
        int port = plugin.getMainConfigManager().getDbPort();
        String db = plugin.getMainConfigManager().getDbDatabase();
        
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=" + plugin.getMainConfigManager().isDbUseSSL());
        config.setUsername(plugin.getMainConfigManager().getDbUser());
        config.setPassword(plugin.getMainConfigManager().getDbPassword());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
        plugin.getLogger().info("Conexión a MySQL establecida correctamente.");
    }

    private void setupTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Tabla de Historial
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS maxstaff_history (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "uuid VARCHAR(36), " +
                    "name VARCHAR(16), " +
                    "type VARCHAR(10), " +
                    "reason TEXT, " +
                    "staff VARCHAR(16), " +
                    "duration VARCHAR(50), " +
                    "date VARCHAR(50))");

            // Tabla de Mutes Activos
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS maxstaff_mutes (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "reason TEXT, " +
                    "staff VARCHAR(16), " +
                    "expiry BIGINT)");

            // Tabla de IPs (Cache)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS maxstaff_ip_cache (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "ip VARCHAR(45))");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}