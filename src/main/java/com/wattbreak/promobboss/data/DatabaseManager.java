package com.wattbreak.promobboss.data;

import com.wattbreak.promobboss.ProMobBoss;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class DatabaseManager {

    private final ProMobBoss plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(ProMobBoss plugin) {
        this.plugin = plugin;
        if (plugin.getConfigManager().getMainConfig().getBoolean("database.enabled", false)) {
            setupDataSource();
            initializeTables();
        } else {
            plugin.getLogger().warning("Database is disabled in config.yml. Player stats will not be saved.");
        }
    }

    private void setupDataSource() {
        plugin.getLogger().info("Setting up database connection pool (HikariCP)...");
        HikariConfig config = new HikariConfig();

        // pull database information from config.yml
        String host = plugin.getConfigManager().getMainConfig().getString("database.host");
        int port = plugin.getConfigManager().getMainConfig().getInt("database.port");
        String dbName = plugin.getConfigManager().getMainConfig().getString("database.database");
        String username = plugin.getConfigManager().getMainConfig().getString("database.username");
        String password = plugin.getConfigManager().getMainConfig().getString("database.password");

        // Generate the JDBC URL
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&autoReconnect=true");
        config.setUsername(username);
        config.setPassword(password);

        // Performance and Reliability Settings
        config.setPoolName("ProMobBoss-HikariPool");
        config.setMaximumPoolSize(plugin.getConfigManager().getMainConfig().getInt("database.pool-settings.maximum-pool-size", 10));
        config.setMinimumIdle(plugin.getConfigManager().getMainConfig().getInt("database.pool-settings.minimum-idle", 5));
        config.setConnectionTimeout(plugin.getConfigManager().getMainConfig().getLong("database.pool-settings.connection-timeout", 30000));
        config.setIdleTimeout(plugin.getConfigManager().getMainConfig().getLong("database.pool-settings.idle-timeout", 600000));
        config.setMaxLifetime(plugin.getConfigManager().getMainConfig().getLong("database.pool-settings.max-lifetime", 1800000));

        // Recommended MySQL settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        try {
            this.dataSource = new HikariDataSource(config);
            plugin.getLogger().info("Database connection pool successfully configured.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create database connection pool!", e);
            dataSource = null;
        }
    }

    private void initializeTables() {
        // This method creates the necessary tables when the plugin is first opened.
        // Runs ASYNC so that it doesn't block the main thread.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // The “try-with-resources” block automatically closes Connection and PreparedStatement.
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement(
                         "CREATE TABLE IF NOT EXISTS promb_player_stats (" +
                                 "uuid VARCHAR(36) PRIMARY KEY NOT NULL," +
                                 "last_known_name VARCHAR(16) NOT NULL," +
                                 "bosses_killed INT DEFAULT 0 NOT NULL," +
                                 "mobs_killed INT DEFAULT 0 NOT NULL" +
                                 ");"
                 )) {
                ps.executeUpdate();
                plugin.getLogger().info("Database tables verified/created successfully.");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create database tables!", e);
            }
        });
    }

    /**
     * Retrieves a database connection from the connection pool.
     * @return A SQL Connection object.
     * @throws SQLException If the connection cannot be received.
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database is not connected.");
        }
        return dataSource.getConnection();
    }

    /**
     * Safely closes the connection pool when closing the plugin.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed.");
        }
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }
}