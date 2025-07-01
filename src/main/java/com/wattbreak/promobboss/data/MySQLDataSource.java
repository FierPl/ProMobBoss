package com.wattbreak.promobboss.data;

import com.wattbreak.promobboss.ProMobBoss;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.AbstractMap;

public class MySQLDataSource implements IDataSource{

    private final ProMobBoss plugin;
    private DatabaseManager dbManager;

    public MySQLDataSource(ProMobBoss plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        this.dbManager = new DatabaseManager(plugin);
    }

    @Override
    public void shutdown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Override
    public void ensurePlayerExists(Player player) {
        if (dbManager == null || !dbManager.isConnected()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final String sql = "INSERT IGNORE INTO promb_player_stats (uuid, last_known_name) VALUES (?, ?);";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setString(2, player.getName());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not ensure player data exists for " + player.getName(), e);
            }
        });
    }

    @Override
    public void incrementBossKills(UUID uuid, String playerName, int amount) {
        if (dbManager == null || !dbManager.isConnected()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final String sql = "INSERT INTO promb_player_stats (uuid, last_known_name, bosses_killed) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE bosses_killed = bosses_killed + VALUES(bosses_killed), last_known_name = VALUES(last_known_name);";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, playerName);
                ps.setInt(3, amount);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not update boss kills for " + playerName, e);
            }
        });
    }

    @Override
    public void getBossKills(UUID uuid, Consumer<Integer> callback) {
        if (dbManager == null || !dbManager.isConnected()) {
            callback.accept(0);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final String sql = "SELECT bosses_killed FROM promb_player_stats WHERE uuid = ?;";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                int kills = rs.next() ? rs.getInt("bosses_killed") : 0;

                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(kills));
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not retrieve boss kills for " + uuid.toString(), e);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(0));
            }
        });
    }

    @Override
    public void getLeaderboardAsync(int limit, Consumer<List<LeaderboardEntry>> callback) {
        if (dbManager == null || !dbManager.isConnected()) {
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(new ArrayList<>()));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<LeaderboardEntry> leaderboard = new ArrayList<>();
            final String sql = "SELECT uuid, last_known_name, bosses_killed FROM promb_player_stats ORDER BY bosses_killed DESC LIMIT ?;";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String name = rs.getString("last_known_name");
                    int kills = rs.getInt("bosses_killed");
                    leaderboard.add(new LeaderboardEntry(uuid, name, kills));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Could not fetch leaderboard data from MySQL.", e);
            }
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(leaderboard));
        });
    }
}