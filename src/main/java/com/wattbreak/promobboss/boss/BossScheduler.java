package com.wattbreak.promobboss.boss;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.common.VanillaMobSettings;
import com.wattbreak.promobboss.gui.menus.mob.EquipmentSelectorMenu;
import com.wattbreak.promobboss.utility.ChatHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public class BossScheduler {

    private final ProMobBoss plugin;
    private final BossManager bossManager;
    private final Map<Boss, LocalDateTime> nextSpawnTimes = new HashMap<>();
    private final Map<Boss, Long> lastAnnouncedSeconds = new HashMap<>();

    public BossScheduler(ProMobBoss plugin, BossManager bossManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
        reloadAllBossSchedules();
        startScheduler();
    }

    public void reloadAllBossSchedules() {
        plugin.getLogger().info("Recalculating all boss spawn times...");
        nextSpawnTimes.clear();
        lastAnnouncedSeconds.clear();
        for (Boss boss : bossManager.getAllBosses()) {
            updateScheduleForBoss(boss);
        }
    }

    public void updateScheduleForBoss(Boss boss) {
        nextSpawnTimes.remove(boss);
        lastAnnouncedSeconds.remove(boss);
        if (!boss.isEnabled()) {
            return;
        }
        LocalDateTime nextSpawn = calculateNextSpawnTime(boss);
        if (nextSpawn != null) {
            nextSpawnTimes.put(boss, nextSpawn);
            plugin.getLogger().info("Scheduled boss '" + boss.getConfigName() + "' for: " + nextSpawn.toString());
        }
    }

    private LocalDateTime calculateNextSpawnTime(Boss boss) {
        LocalDateTime now = LocalDateTime.now();
        if (boss.getScheduleType() == Boss.ScheduleType.RECURRING) {
            return now.plusMinutes(boss.getRecurringIntervalMinutes());
        } else {
            try {
                if (boss.getScheduleTime() == null || boss.getScheduleDay() == null) {
                    throw new NullPointerException("Schedule time or day is null");
                }
                LocalTime spawnTime = LocalTime.parse(boss.getScheduleTime());
                String daySetting = boss.getScheduleDay();
                if ("EVERYDAY".equalsIgnoreCase(daySetting)) {
                    LocalDateTime todaySpawn = now.with(spawnTime);
                    return now.isAfter(todaySpawn) ? todaySpawn.plusDays(1) : todaySpawn;
                } else {
                    DayOfWeek targetDay = DayOfWeek.valueOf(daySetting.toUpperCase());
                    LocalDateTime nextDaySpawn = now.with(targetDay).with(spawnTime);
                    return now.isAfter(nextDaySpawn) ? nextDaySpawn.plusWeeks(1) : nextDaySpawn;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not parse schedule for boss " + boss.getConfigName() + ". Invalid day/time format. Error: " + e.getMessage());
                return null;
            }
        }
    }

    private void startScheduler() {
        long interval = plugin.getConfigManager().getSettingsConfig().getLong("boss-scheduler.check-interval", 20L);
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    checkAllBosses();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "An error occurred in the BossScheduler task!", e);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, interval);
    }

    private void checkAllBosses() {
        LocalDateTime now = LocalDateTime.now();
        new HashMap<>(nextSpawnTimes).forEach((boss, spawnTime) -> {
            if (spawnTime == null) return;
            long secondsUntilSpawn = ChronoUnit.SECONDS.between(now, spawnTime);
            if (secondsUntilSpawn > 0) {
                checkAnnouncements(boss, secondsUntilSpawn);
                checkCountdown(boss, secondsUntilSpawn);
            }

            if (now.isAfter(spawnTime) || now.isEqual(spawnTime)) {
                spawnBoss(boss);
                updateScheduleForBoss(boss);
            }
        });
    }

    private void checkAnnouncements(Boss boss, long secondsUntilSpawn) {
        ConfigurationSection announceSection = plugin.getConfigManager().getSettingsConfig().getConfigurationSection("boss-scheduler.announcements.before-spawn");
        if (announceSection == null) return;

        for (String key : announceSection.getKeys(false)) {
            long announceTime = announceSection.getLong(key + ".time");
            if (secondsUntilSpawn == announceTime && (!lastAnnouncedSeconds.containsKey(boss) || lastAnnouncedSeconds.get(boss) != announceTime)) {
                String messagePath = announceSection.getString(key + ".message-path");
                if (messagePath == null) continue;
                String worldName = (boss.getSpawnLocation() != null && boss.getSpawnLocation().getWorld() != null) ? boss.getSpawnLocation().getWorld().getName() : "Bilinmiyor";
                String message = plugin.getMessage(messagePath, "%boss_name%", boss.getDisplayName(), "%world%", worldName);
                Bukkit.broadcastMessage(message);
                lastAnnouncedSeconds.put(boss, announceTime);
            }
        }
    }

    private void checkCountdown(Boss boss, long secondsUntilSpawn) {
        ConfigurationSection countdownSection = plugin.getConfigManager().getSettingsConfig().getConfigurationSection("boss-scheduler.countdown");
        if (countdownSection == null || !countdownSection.getBoolean("enabled", true)) return;

        long startFrom = countdownSection.getLong("start-from", 5);
        if (secondsUntilSpawn <= startFrom && secondsUntilSpawn > 0 &&
                (!lastAnnouncedSeconds.containsKey(boss) || lastAnnouncedSeconds.get(boss) != secondsUntilSpawn)) {
            String messagePath = countdownSection.getString("message-path");
            if (messagePath == null) return;
            String message = plugin.getMessage(messagePath, "%boss_name%", boss.getDisplayName(), "%time%", String.valueOf(secondsUntilSpawn));
            Bukkit.broadcastMessage(message);
            lastAnnouncedSeconds.put(boss, secondsUntilSpawn);
        }
    }

    public void spawnBoss(Boss boss) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Location loc = boss.getSpawnLocation();
                if (loc == null || loc.getWorld() == null) {
                    plugin.getLogger().warning("Boss " + boss.getConfigName() + " could not be spawned because its location is not set or world is not loaded.");
                    return;
                }

                Entity spawnedEntity = null;
                if ("MYTHIC_MOBS".equalsIgnoreCase(boss.getMobTypeSource())) {
                    if (plugin.getMythicMobsHandler() != null && plugin.isMythicMobsEnabled()) {
                        spawnedEntity = plugin.getMythicMobsHandler().spawnMythicMob(boss.getMythicMobName(), loc);
                        if (spawnedEntity == null) {
                            plugin.getLogger().warning("Failed to spawn MythicMob: " + boss.getMythicMobName() + " for boss " + boss.getConfigName());
                        }
                    } else {
                        plugin.getLogger().warning("Boss " + boss.getConfigName() + " is set to MythicMobs, but the integration is not ready.");
                    }
                } else {
                    spawnedEntity = spawnVanillaBoss(boss, loc);
                }

                if (spawnedEntity != null) {
                    bossManager.registerActiveBoss(spawnedEntity, boss);
                    plugin.getLogger().info("Boss '" + boss.getConfigName() + "' spawned at " + loc.getWorld().getName() + " X:" + (int)loc.getX() + " Y:" + (int)loc.getY() + " Z:" + (int)loc.getZ());
                } else {
                    plugin.getLogger().warning("Failed to spawn boss: " + boss.getConfigName());
                }

                ConfigurationSection onSpawnSection = plugin.getConfigManager().getSettingsConfig().getConfigurationSection("boss-scheduler.announcements.on-spawn");
                if(onSpawnSection != null && onSpawnSection.getBoolean("enabled", true)) {
                    String messagePath = onSpawnSection.getString("message-path");
                    if (messagePath == null) return;
                    String message = plugin.getMessage(messagePath, "%boss_name%", boss.getDisplayName());
                    Bukkit.broadcastMessage(message);
                }

                if (plugin.getDiscordWebhookHandler() != null && plugin.getDiscordWebhookHandler().isEnabled("boss-spawn")) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("%boss_name%", boss.getConfigName());
                    placeholders.put("%boss_displayname%", ChatHelper.stripColor(boss.getDisplayName()));
                    placeholders.put("%world%", loc.getWorld().getName());
                    placeholders.put("%location_x%", String.format("%.0f", loc.getX()));
                    placeholders.put("%location_y%", String.format("%.0f", loc.getY()));
                    placeholders.put("%location_z%", String.format("%.0f", loc.getZ()));
                    plugin.getDiscordWebhookHandler().sendMessage("boss-spawn", placeholders);
                }
            }
        }.runTask(plugin);
    }

    private LivingEntity spawnVanillaBoss(Boss boss, Location loc) {
        VanillaMobSettings settings = boss.getVanillaMobSettings();
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(settings.getEntityType().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid entity type '" + settings.getEntityType() + "' for boss " + boss.getConfigName());
            return null;
        }

        Entity entity = loc.getWorld().spawnEntity(loc, entityType);
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;

            livingEntity.setCustomName(ChatHelper.colorize(boss.getDisplayName()));
            livingEntity.setCustomNameVisible(true);
            if (livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(settings.getHealth());
            }
            livingEntity.setHealth(settings.getHealth());
            if (livingEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                livingEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(settings.getDamage());
            }

            EntityEquipment equipment = livingEntity.getEquipment();
            if (equipment != null) {
                equipment.setHelmet(createEnchantedItem(settings, EquipmentSelectorMenu.EquipmentType.HELMET));
                equipment.setChestplate(createEnchantedItem(settings, EquipmentSelectorMenu.EquipmentType.CHESTPLATE));
                equipment.setLeggings(createEnchantedItem(settings, EquipmentSelectorMenu.EquipmentType.LEGGINGS));
                equipment.setBoots(createEnchantedItem(settings, EquipmentSelectorMenu.EquipmentType.BOOTS));
                equipment.setItemInMainHand(createEnchantedItem(settings, EquipmentSelectorMenu.EquipmentType.WEAPON));

                equipment.setHelmetDropChance(0f);
                equipment.setChestplateDropChance(0f);
                equipment.setLeggingsDropChance(0f);
                equipment.setBootsDropChance(0f);
                equipment.setItemInMainHandDropChance(0f);
            }

            livingEntity.setPersistent(true);
            livingEntity.setRemoveWhenFarAway(false);
            return livingEntity;
        }
        return null;
    }

    private ItemStack createEnchantedItem(VanillaMobSettings settings, EquipmentSelectorMenu.EquipmentType type) {
        String materialName = settings.getEquipment(type);
        if (materialName == null || materialName.equalsIgnoreCase("AIR")) {
            return new ItemStack(Material.AIR);
        }

        ItemStack item;
        try {
            item = new ItemStack(Material.valueOf(materialName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material name '" + materialName + "' for equipment slot " + type.name());
            return new ItemStack(Material.AIR);
        }

        Map<String, Integer> enchantments = settings.getEnchantMapFor(type);
        if (enchantments != null && !enchantments.isEmpty()) {
            for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(entry.getKey().toLowerCase()));
                if (enchantment != null) {
                    item.addUnsafeEnchantment(enchantment, entry.getValue());
                }
            }
        }
        return item;
    }

    public long getSecondsUntilNextEvent() {
        LocalDateTime now = LocalDateTime.now();
        Optional<Long> minSeconds = nextSpawnTimes.values().stream()
                .filter(time -> time != null && time.isAfter(now))
                .map(time -> ChronoUnit.SECONDS.between(now, time))
                .min(Long::compare);
        return minSeconds.orElse(-1L);
    }

    public long getSecondsUntilBoss(String bossConfigName) {
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<Boss, LocalDateTime> entry : nextSpawnTimes.entrySet()) {
            if (entry.getKey().getConfigName().equalsIgnoreCase(bossConfigName)) {
                LocalDateTime spawnTime = entry.getValue();
                return (spawnTime != null && spawnTime.isAfter(now)) ? ChronoUnit.SECONDS.between(now, spawnTime) : -1L;
            }
        }
        return -1L;
    }
}