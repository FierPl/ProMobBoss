package com.wattbreak.promobboss.mob;

import com.wattbreak.promobboss.common.IMobEditable; // Yeni import
import com.wattbreak.promobboss.common.VanillaMobSettings; // Ortak settings sınıfı
import com.wattbreak.promobboss.reward.Reward; // Reward objesi
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location; // IMobEditable için

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents all configuration settings of a custom mob.
 * Implement the IMobEditable interface and use the menus common to the Boss system.
 */
@Getter
@Setter
public class Mob implements IMobEditable {

    private final String configName;
    private String displayName;
    private boolean enabled;

    private String spawnWorld;
    private int spawnInterval;
    private int maxAlive;

    private String mobTypeSource = "VANILLA_CUSTOM";
    private String mythicMobName;

    private VanillaMobSettings vanillaMobSettings = new VanillaMobSettings();

    private List<Reward> rewards = new ArrayList<>();

    public Mob(String configName) {
        this.configName = configName;
        this.displayName = "&a" + configName;
        this.enabled = false;
        this.spawnWorld = "world";
        this.spawnInterval = 30;
        this.maxAlive = 5;
    }


    @Override
    public List<Reward> getRewards() {
        return this.rewards;
    }

    @Override
    public void setRewards(List<Reward> rewards) {
        this.rewards = rewards;
    }

    @Override
    public VanillaMobSettings getVanillaMobSettings() {
        return this.vanillaMobSettings;
    }

    @Override public Location getSpawnLocation() { return null; }
    @Override public void setSpawnLocation(Location location) { /* This does not apply to mob */ }
}