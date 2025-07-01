package com.wattbreak.promobboss.integrations;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.data.IDataSource;
import com.wattbreak.promobboss.data.LeaderboardCache;
import com.wattbreak.promobboss.data.LeaderboardEntry;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ProMobBossExpansion extends PlaceholderExpansion {

    private final ProMobBoss plugin;
    private final IDataSource dataSource;
    private final LeaderboardCache leaderboardCache;

    private final Map<String, String> timePlaceholdersCache = new HashMap<>();
    private BukkitRunnable refreshTask;

    public ProMobBossExpansion(ProMobBoss plugin) {
        this.plugin = plugin;
        this.dataSource = plugin.getDataSource();
        this.leaderboardCache = plugin.getLeaderboardCache();
        startRefreshingPlaceholders();
    }

    @Override public @NotNull String getIdentifier() { return "promb"; }
    @Override public @NotNull String getAuthor() { return "WattBreak"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }
    @Override public boolean canRegister() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {

        // %promb_event_time%
        if (params.equalsIgnoreCase("event_time")) {
            return timePlaceholdersCache.getOrDefault("event_time", plugin.getMessage("placeholders.not-available"));
        }

        // %promb_event_time_boss_<bosismi>%
        if (params.startsWith("event_time_boss_")) {
            String bossName = params.substring("event_time_boss_".length());
            return timePlaceholdersCache.getOrDefault("event_time_boss_" + bossName, plugin.getMessage("placeholders.not-available"));
        }

        if (params.startsWith("leaderboard_")) {
            if (leaderboardCache == null) return plugin.getMessage("placeholders.loading");

            String[] parts = params.split("_");
            if (parts.length == 3) {
                try {
                    String type = parts[1];
                    int rank = Integer.parseInt(parts[2]);
                    if (rank < 1) return "";

                    int index = rank - 1;

                    List<LeaderboardEntry> leaderboard = leaderboardCache.getCachedLeaderboard();

                    if (index < leaderboard.size()) {
                        LeaderboardEntry entry = leaderboard.get(index);
                        if ("name".equalsIgnoreCase(type)) return entry.getPlayerName();
                        if ("kill".equalsIgnoreCase(type)) return String.valueOf(entry.getKills());
                    } else {
                        return plugin.getMessage("placeholders.not-found-rank");
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid leaderboard rank format: " + params);
                    return "";
                }
            }
        }

        if (player == null) {
            return "";
        }
        // %promb_boss_kill%
        if (params.equalsIgnoreCase("boss_kill")) {
            return String.valueOf(leaderboardCache.getCachedPlayerKills(player.getUniqueId()));
        }

        return null;
    }

    private void startRefreshingPlaceholders() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        refreshTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getBossScheduler() != null) {
                    long secondsUntilNext = plugin.getBossScheduler().getSecondsUntilNextEvent();
                    timePlaceholdersCache.put("event_time", formatTime(secondsUntilNext));

                    plugin.getBossManager().getAllBosses().forEach(boss -> {
                        long seconds = plugin.getBossScheduler().getSecondsUntilBoss(boss.getConfigName());
                        timePlaceholdersCache.put("event_time_boss_" + boss.getConfigName(), formatTime(seconds));
                    });
                }
            }
        };
        refreshTask.runTaskTimer(plugin, 0L, 20L);
        plugin.getLogger().info("Time placeholders refresh task started.");
    }

    public void onDisable() {
        if (refreshTask != null) {
            refreshTask.cancel();
            plugin.getLogger().info("Time placeholders refresh task stopped.");
        }
    }

    private String formatTime(long totalSeconds) {
        if (totalSeconds < 0) {
            return plugin.getMessage("placeholders.event-none");
        }
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = ((totalSeconds % 86400) % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (days > 0) {
            return plugin.getMessage("placeholders.format-days", "%days%", String.valueOf(days), "%hours%", String.valueOf(hours)); // DİL DOSYASINDAN
        } else if (hours > 0) {
            return plugin.getMessage("placeholders.format-hours", "%hours%", String.valueOf(hours), "%minutes%", String.valueOf(minutes), "%seconds%", String.valueOf(seconds)); // DİL DOSYASINDAN
        } else {
            return plugin.getMessage("placeholders.format-minutes", "%minutes%", String.valueOf(minutes), "%seconds%", String.valueOf(seconds)); // DİL DOSYASINDAN
        }
    }
}