package com.wattbreak.promobboss.data;

import com.wattbreak.promobboss.ProMobBoss;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

public class SQLiteDataSource implements IDataSource {

    private final ProMobBoss plugin;
    private Connection connection;
    private final String tableName = "promb_player_stats";

    public SQLiteDataSource(ProMobBoss plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "playerdata.db");
            if (!dbFile.exists()) {
                dbFile.createNewFile();
            }
            // Establish SQLite connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            // Create the table (if it does not exist)
            try (Statement statement = connection.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                        "uuid VARCHAR(36) PRIMARY KEY NOT NULL," +
                        "last_known_name VARCHAR(16) NOT NULL," +
                        "bosses_killed INT DEFAULT 0 NOT NULL" +
                        ");";
                statement.execute(sql);
            }
        } catch (SQLException | IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite database!", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to close SQLite connection!", e);
        }
    }

    @Override
    public void ensurePlayerExists(Player player) {
        // Run asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final String sql = "INSERT OR IGNORE INTO " + tableName + " (uuid, last_known_name) VALUES(?,?);";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setString(2, player.getName());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to ensure player data for " + player.getName(), e);
            }
        });
    }

    @Override
    public void incrementBossKills(UUID uuid, String playerName, int amount) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final String sql = "INSERT INTO " + tableName + " (uuid, last_known_name, bosses_killed) VALUES(?,?,?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET bosses_killed = bosses_killed + excluded.bosses_killed, last_known_name = excluded.last_known_name;";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, playerName);
                ps.setInt(3, amount);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to increment boss kills for " + playerName, e);
            }
        });
    }

    @Override
    public void getBossKills(UUID uuid, Consumer<Integer> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final String sql = "SELECT bosses_killed FROM " + tableName + " WHERE uuid = ?;";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                int kills = 0;
                if (rs.next()) {
                    kills = rs.getInt("bosses_killed");
                }
                int finalKills = kills;
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalKills));
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to get boss kills for " + uuid, e);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(0));
            }
        });
    }


    @Override
    public void getLeaderboardAsync(int limit, Consumer<List<LeaderboardEntry>> callback) {
        if (connection == null) {
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(new ArrayList<>()));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<LeaderboardEntry> leaderboard = new ArrayList<>();
            final String sql = "SELECT uuid, last_known_name, bosses_killed FROM " + tableName + " ORDER BY bosses_killed DESC LIMIT ?;";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String name = rs.getString("last_known_name");
                    int kills = rs.getInt("bosses_killed");
                    leaderboard.add(new LeaderboardEntry(uuid, name, kills));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Could not fetch leaderboard data from SQLite.", e);
            }
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(leaderboard));
        });
    }

}