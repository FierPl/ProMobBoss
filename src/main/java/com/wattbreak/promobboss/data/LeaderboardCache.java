package com.wattbreak.promobboss.data;

import com.wattbreak.promobboss.ProMobBoss;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class LeaderboardCache {

    private final ProMobBoss plugin;
    private final IDataSource dataSource;

    private volatile List<LeaderboardEntry> cachedLeaderboard = new ArrayList<>();
    private final Map<UUID, Integer> cachedPlayerKills = new ConcurrentHashMap<>();

    public LeaderboardCache(ProMobBoss plugin, IDataSource dataSource) {
        this.plugin = plugin;
        this.dataSource = dataSource;
        startCacheUpdateTask();
    }

    public void startCacheUpdateTask() {
        long updateIntervalSeconds = plugin.getConfigManager().getSettingsConfig().getLong("leaderboard.cache-duration", 600L);

        new BukkitRunnable() {
            @Override
            public void run() {
                dataSource.getLeaderboardAsync(10, result -> {
                    cachedLeaderboard = result;
                    plugin.getLogger().info("Leaderboard cache updated (" + result.size() + " entries).");

                    cachedPlayerKills.clear();
                    for(LeaderboardEntry entry : result) {
                        cachedPlayerKills.put(entry.getUuid(), entry.getKills());
                    }
                });
            }
        }.runTaskTimerAsynchronously(plugin, 0L, updateIntervalSeconds * 20L);
    }

    public List<LeaderboardEntry> getCachedLeaderboard() {
        return cachedLeaderboard;
    }

    public int getCachedPlayerKills(UUID playerUUID) {
        return cachedPlayerKills.getOrDefault(playerUUID, 0);
    }

    public void updatePlayerKillsInCache(UUID playerUUID, int newKills) {
        cachedPlayerKills.put(playerUUID, newKills);
    }
}