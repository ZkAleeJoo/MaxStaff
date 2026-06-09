package org.zkaleejoo.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.zkaleejoo.MaxStaff;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
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
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS maxstaff_history (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "uuid VARCHAR(36), " +
                    "name VARCHAR(16), " +
                    "type VARCHAR(10), " +
                    "reason TEXT, " +
                    "staff VARCHAR(16), " +
                    "duration VARCHAR(50), " +
                    "created_at BIGINT NOT NULL)");


            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS maxstaff_mutes (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16), " +
                    "reason TEXT, " +
                    "staff VARCHAR(16), " +
                    "expiry BIGINT)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS maxstaff_bans (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16), " +
                    "reason TEXT, " +
                    "staff VARCHAR(16), " +
                    "expiry BIGINT, " +
                    "created_at BIGINT NOT NULL)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS maxstaff_ip_bans (" +
                    "ip VARCHAR(45) PRIMARY KEY, " +
                    "reason TEXT, " +
                    "staff VARCHAR(16), " +
                    "expiry BIGINT, " +
                    "created_at BIGINT NOT NULL)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS maxstaff_sync_actions (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "action_type VARCHAR(20), " +
                    "target_uuid VARCHAR(36), " +
                    "target_name VARCHAR(64), " +
                    "reason TEXT, " +
                    "staff VARCHAR(16), " +
                    "duration VARCHAR(50), " +
                    "created_at BIGINT NOT NULL, " +
                    "expires_at BIGINT, " +
                    "source_server VARCHAR(64))");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS maxstaff_ip_cache (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "ip VARCHAR(45))");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS maxstaff_sync_action_acks (" +
                    "action_id BIGINT NOT NULL, " +
                    "server_id VARCHAR(64) NOT NULL, " +
                    "processed_at BIGINT NOT NULL, " +
                    "PRIMARY KEY (action_id, server_id))");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS maxstaff_name_cache (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "last_name VARCHAR(16), " +
                    "updated_at BIGINT NOT NULL)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS maxstaff_vanish_states (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16), " +
                    "enabled BOOLEAN NOT NULL DEFAULT TRUE, " +
                    "updated_at BIGINT NOT NULL, " +
                    "server_id VARCHAR(64))");

        migrateTables(conn, stmt);


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void migrateTables(Connection conn, Statement stmt) throws SQLException {
        if (!columnExists(conn, "maxstaff_history", "created_at")) {
            stmt.executeUpdate("ALTER TABLE maxstaff_history ADD COLUMN created_at BIGINT NOT NULL DEFAULT 0");
            plugin.getLogger().info("[MaxStaff SQL] Added created_at column to maxstaff_history.");
        }

        if (columnExists(conn, "maxstaff_history", "date")) {
            stmt.executeUpdate("UPDATE maxstaff_history " +
                    "SET created_at = COALESCE(UNIX_TIMESTAMP(STR_TO_DATE(date, '%d/%m/%Y %H:%i')) * 1000, created_at) " +
                    "WHERE (created_at = 0 OR created_at IS NULL) AND date IS NOT NULL AND date <> ''");
        }

        stmt.executeUpdate("UPDATE maxstaff_history SET created_at = " + System.currentTimeMillis() + " WHERE created_at = 0 OR created_at IS NULL");

        createIndexIfMissing(conn, stmt, "maxstaff_history", "idx_history_uuid_type", "uuid, type");
        createIndexIfMissing(conn, stmt, "maxstaff_history", "idx_history_uuid_created_at", "uuid, created_at");

        if (!columnExists(conn, "maxstaff_mutes", "name")) {
            stmt.executeUpdate("ALTER TABLE maxstaff_mutes ADD COLUMN name VARCHAR(16) NULL AFTER uuid");
            plugin.getLogger().info("[MaxStaff SQL] Added name column to maxstaff_mutes.");
        }

        createIndexIfMissing(conn, stmt, "maxstaff_bans", "idx_bans_name", "name");
        createIndexIfMissing(conn, stmt, "maxstaff_bans", "idx_bans_expiry", "expiry");
        createIndexIfMissing(conn, stmt, "maxstaff_ip_bans", "idx_ip_bans_expiry", "expiry");
        createIndexIfMissing(conn, stmt, "maxstaff_sync_actions", "idx_sync_created_at", "created_at");
        createIndexIfMissing(conn, stmt, "maxstaff_sync_actions", "idx_sync_target_uuid", "target_uuid");
        createIndexIfMissing(conn, stmt, "maxstaff_sync_actions", "idx_sync_expires_at", "expires_at");

        createIndexIfMissing(conn, stmt, "maxstaff_sync_action_acks", "idx_sync_acks_server", "server_id");
        createIndexIfMissing(conn, stmt, "maxstaff_sync_action_acks", "idx_sync_acks_processed", "processed_at");

        createIndexIfMissing(conn, stmt, "maxstaff_name_cache", "idx_name_cache_last_name", "last_name");
        createIndexIfMissing(conn, stmt, "maxstaff_vanish_states", "idx_vanish_enabled", "enabled");
    }

    private boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet columns = metaData.getColumns(conn.getCatalog(), null, tableName, columnName)) {
            return columns.next();
        }
    }

    private void createIndexIfMissing(Connection conn, Statement stmt, String tableName, String indexName, String columns) throws SQLException {
        if (indexExists(conn, tableName, indexName)) {
            return;
        }

        stmt.executeUpdate("CREATE INDEX " + indexName + " ON " + tableName + " (" + columns + ")");
    }

    private boolean indexExists(Connection conn, String tableName, String indexName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet indexes = metaData.getIndexInfo(conn.getCatalog(), null, tableName, false, false)) {
            while (indexes.next()) {
                String currentIndexName = indexes.getString("INDEX_NAME");
                if (indexName.equalsIgnoreCase(currentIndexName)) {
                    return true;
                }
            }
        }
        return false;
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
