package com.wattbreak.promobboss.data;

import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.function.Consumer;

import java.util.List;
import java.util.Map;

public interface IDataSource {
    void init();
    void shutdown();
    void ensurePlayerExists(Player player);
    void incrementBossKills(UUID uuid, String playerName, int amount);
    void getBossKills(UUID uuid, Consumer<Integer> callback);

    void getLeaderboardAsync(int limit, Consumer<List<LeaderboardEntry>> callback);
}