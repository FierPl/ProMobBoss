package com.wattbreak.promobboss.data;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class LeaderboardEntry {
    private final UUID uuid;
    private final String playerName;
    private final int kills;

    public LeaderboardEntry(UUID uuid, String playerName, int kills) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.kills = kills;
    }
}