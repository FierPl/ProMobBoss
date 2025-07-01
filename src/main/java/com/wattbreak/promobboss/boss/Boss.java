package com.wattbreak.promobboss.boss;

import com.wattbreak.promobboss.common.IMobEditable; // Yeni import
import com.wattbreak.promobboss.common.VanillaMobSettings; // Ortak settings sınıfı
import com.wattbreak.promobboss.reward.Reward; // Reward objesi
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Bu sınıf, bir boss'un tüm yapılandırma ayarlarını temsil eder.
 * IMobEditable arayüzünü implemente ederek, Mob sistemiyle ortak olan menüleri kullanabilir.
 */
@Getter
@Setter
// Boss artık IMobEditable'ı implemente ediyor.
public class Boss implements IMobEditable {

    private final String configName;
    private String displayName;
    private boolean enabled;

    private Location spawnLocation;
    private ScheduleType scheduleType = ScheduleType.SCHEDULED;
    private String scheduleDay = "EVERYDAY"; // Varsayılan değer
    private String scheduleTime = "20:00";   // Varsayılan değer
    private int recurringIntervalMinutes = 60;

    private String mobTypeSource = "VANILLA_CUSTOM";
    private String mythicMobName;

    private VanillaMobSettings vanillaMobSettings = new VanillaMobSettings();

    private List<Reward> rewards = new ArrayList<>();

    public Boss(String configName) {
        this.configName = configName;
        this.displayName = "&c" + configName; // Varsayılan isim
    }

    // --- IMobEditable Arayüz Metodları ---
    // Lombok sayesinde birçok getter/setter otomatik sağlanır.
    // Ancak arayüzün beklediği her metodun burada bir implementasyonu olmalı.

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

    @Override
    public Location getSpawnLocation() {
        return this.spawnLocation;
    }

    @Override
    public void setSpawnLocation(Location location) {
        this.spawnLocation = location;
    }

    // Mob'a özel spawn ayarları, Boss için geçerli değil.
    // Varsayılan değerler veya null döndürebilirler.
    @Override public String getSpawnWorld() { return null; }
    @Override public void setSpawnWorld(String world) { /* Bu boss için uygulanmaz */ }
    @Override public int getSpawnInterval() { return 0; }
    @Override public void setSpawnInterval(int interval) { /* Bu boss için uygulanmaz */ }
    @Override public int getMaxAlive() { return 0; }
    @Override public void setMaxAlive(int maxAlive) { /* Bu boss için uygulanmaz */ }


    public enum ScheduleType {
        RECURRING, SCHEDULED
    }
}