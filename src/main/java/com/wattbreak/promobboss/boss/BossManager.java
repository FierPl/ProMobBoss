package com.wattbreak.promobboss.boss;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.common.VanillaMobSettings;
import com.wattbreak.promobboss.reward.Reward;
import com.wattbreak.promobboss.utility.ItemSerializer;
import com.wattbreak.promobboss.utility.LocationSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;


public class BossManager {

    private final ProMobBoss plugin;
    private final Map<String, Boss> loadedBosses = new HashMap<>();
    private final Map<UUID, Boss> activeBosses = new HashMap<>();

    private final File bossesFolder;
    private final File rewardsFolder;

    public BossManager(ProMobBoss plugin) {
        this.plugin = plugin;
        this.bossesFolder = new File(plugin.getDataFolder(), "Bosses");
        if (!bossesFolder.exists()) {
            bossesFolder.mkdirs();
        }
        this.rewardsFolder = new File(plugin.getDataFolder(), "Bosses/Rewards");
        if (!rewardsFolder.exists()) {
            rewardsFolder.mkdirs();
        }
        loadAllBosses();
    }

    public void loadAllBosses() {
        loadedBosses.clear();
        File[] bossFiles = bossesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (bossFiles == null) {
            plugin.getLogger().info("No boss configurations found in 'Bosses' folder.");
            return;
        }

        for (File file : bossFiles) {
            String configName = file.getName().replace(".yml", "");
            FileConfiguration bossConfig = YamlConfiguration.loadConfiguration(file);
            Boss boss = new Boss(configName);

            // Laod basic Setting
            boss.setDisplayName(bossConfig.getString("display-name", "&c" + configName));
            boss.setEnabled(bossConfig.getBoolean("enabled", false));
            if (bossConfig.isConfigurationSection("spawn-location")) {
                boss.setSpawnLocation(LocationSerializer.deserialize(bossConfig.getConfigurationSection("spawn-location")));
            }

            // Load Schedule Settings
            ConfigurationSection scheduleSection = bossConfig.getConfigurationSection("schedule");
            if (scheduleSection != null) {
                try {
                    boss.setScheduleType(Boss.ScheduleType.valueOf(scheduleSection.getString("type", "SCHEDULED")));
                } catch (IllegalArgumentException e) {
                    boss.setScheduleType(Boss.ScheduleType.SCHEDULED);
                }
                boss.setRecurringIntervalMinutes(scheduleSection.getInt("interval-minutes", 60));
                boss.setScheduleDay(scheduleSection.getString("day", "EVERYDAY"));
                boss.setScheduleTime(scheduleSection.getString("time", "20:00"));
            }

            // Load Mob Settings
            boss.setMobTypeSource(bossConfig.getString("mob-settings.source", "VANILLA_CUSTOM"));
            boss.setMythicMobName(bossConfig.getString("mob-settings.mythic-mob-name"));

            // Vanilla Mob Settings
            ConfigurationSection vanillaSection = bossConfig.getConfigurationSection("mob-settings.vanilla-custom");
            if (vanillaSection != null) {
                VanillaMobSettings settings = boss.getVanillaMobSettings();
                settings.setEntityType(vanillaSection.getString("entity-type", "ZOMBIE"));
                settings.setHealth(vanillaSection.getDouble("health", 100.0));
                settings.setDamage(vanillaSection.getDouble("damage", 10.0));

                ConfigurationSection equipSection = vanillaSection.getConfigurationSection("equipment");
                if (equipSection != null) {
                    settings.setMainHand(equipSection.getString("main-hand", "AIR"));
                    settings.setHelmet(equipSection.getString("helmet", "AIR"));
                    settings.setChestplate(equipSection.getString("chestplate", "AIR"));
                    settings.setLeggings(equipSection.getString("leggings", "AIR"));
                    settings.setBoots(equipSection.getString("boots", "AIR"));

                    // --- SPELL INSTALLATION ---
                    settings.setMainHandEnchants(loadEnchantMap(equipSection.getConfigurationSection("main-hand-enchants")));
                    settings.setHelmetEnchants(loadEnchantMap(equipSection.getConfigurationSection("helmet-enchants")));
                    settings.setChestplateEnchants(loadEnchantMap(equipSection.getConfigurationSection("chestplate-enchants")));
                    settings.setLeggingsEnchants(loadEnchantMap(equipSection.getConfigurationSection("leggings-enchants")));
                    settings.setBootsEnchants(loadEnchantMap(equipSection.getConfigurationSection("boots-enchants")));
                }
            }

            loadRewardsForBoss(boss);
            loadedBosses.put(configName.toLowerCase(), boss);
        }
        plugin.getLogger().info("Loaded " + loadedBosses.size() + " boss configuration(s).");
    }

    private Map<String, Integer> loadEnchantMap(ConfigurationSection section) {
        Map<String, Integer> enchants = new HashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (section.isInt(key)) {
                    enchants.put(key, section.getInt(key));
                }
            }
        }
        return enchants;
    }

    public void saveBoss(Boss boss) {
        File bossFile = new File(bossesFolder, boss.getConfigName() + ".yml");
        FileConfiguration bossConfig = new YamlConfiguration();

        bossConfig.set("display-name", boss.getDisplayName());
        bossConfig.set("enabled", boss.isEnabled());

        if (boss.getSpawnLocation() != null) {
            LocationSerializer.serialize(boss.getSpawnLocation(), bossConfig.createSection("spawn-location"));
        } else {
            bossConfig.set("spawn-location", null);
        }

        bossConfig.set("schedule.type", boss.getScheduleType().name());
        bossConfig.set("schedule.interval-minutes", boss.getRecurringIntervalMinutes());
        bossConfig.set("schedule.day", boss.getScheduleDay());
        bossConfig.set("schedule.time", boss.getScheduleTime());

        bossConfig.set("mob-settings.source", boss.getMobTypeSource());
        bossConfig.set("mob-settings.mythic-mob-name", boss.getMythicMobName());

        VanillaMobSettings settings = boss.getVanillaMobSettings();
        String vanillaPath = "mob-settings.vanilla-custom.";

        // Basic vanilla settings
        bossConfig.set(vanillaPath + "entity-type", settings.getEntityType());
        bossConfig.set(vanillaPath + "health", settings.getHealth());
        bossConfig.set(vanillaPath + "damage", settings.getDamage());

        // Equipment settings
        String equipPath = vanillaPath + "equipment.";
        bossConfig.set(equipPath + "main-hand", settings.getMainHand());
        bossConfig.set(equipPath + "helmet", settings.getHelmet());
        bossConfig.set(equipPath + "chestplate", settings.getChestplate());
        bossConfig.set(equipPath + "leggings", settings.getLeggings());
        bossConfig.set(equipPath + "boots", settings.getBoots());

        // --- RECORDING SPELLS ---
        bossConfig.set(equipPath + "main-hand-enchants", settings.getMainHandEnchants());
        bossConfig.set(equipPath + "helmet-enchants", settings.getHelmetEnchants());
        bossConfig.set(equipPath + "chestplate-enchants", settings.getChestplateEnchants());
        bossConfig.set(equipPath + "leggings-enchants", settings.getLeggingsEnchants());
        bossConfig.set(equipPath + "boots-enchants", settings.getBootsEnchants());

        try {
            bossConfig.save(bossFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save boss file: " + boss.getConfigName(), e);
        }

        if (plugin.getBossScheduler() != null) {
            plugin.getBossScheduler().updateScheduleForBoss(boss);
        }
    }

    public boolean createNewBoss(String configName) {
        if (loadedBosses.containsKey(configName.toLowerCase())) {
            return false;
        }
        File bossFile = new File(bossesFolder, configName + ".yml");
        try {
            if (bossFile.createNewFile()) {
                Boss boss = new Boss(configName);
                boss.setDisplayName("&c" + configName);
                boss.setEnabled(false);
                loadedBosses.put(configName.toLowerCase(), boss);
                saveBoss(boss);
                return true;
            }
            return false;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create new boss file: " + configName, e);
            return false;
        }
    }

    public boolean removeBoss(Boss boss) {
        if (boss == null) return false;
        String configName = boss.getConfigName();

        loadedBosses.remove(configName.toLowerCase());
        boss.setEnabled(false);
        if (plugin.getBossScheduler() != null) {
            plugin.getBossScheduler().updateScheduleForBoss(boss);
        }

        new File(bossesFolder, configName + ".yml").delete();
        new File(rewardsFolder, configName + ".yml").delete();

        return true;
    }

    public void saveRewardsForBoss(Boss boss) {
        File rewardFile = new File(rewardsFolder, boss.getConfigName() + ".yml");
        FileConfiguration rewardConfig = new YamlConfiguration();

        List<Reward> rewards = boss.getRewards();
        if (rewards == null || rewards.isEmpty()) {
            if (rewardFile.exists()) rewardFile.delete();
            return;
        }

        for(int i = 0; i < rewards.size(); i++) {
            Reward reward = rewards.get(i);
            String path = "rewards." + i;
            rewardConfig.set(path + ".type", reward.getType().name());
            rewardConfig.set(path + ".target", reward.getTarget().name());
            rewardConfig.set(path + ".chance", reward.getChance());

            if (reward.getType() == Reward.RewardType.ITEM && reward.getItem() != null) {
                rewardConfig.set(path + ".item", ItemSerializer.itemStackArrayToBase64(new ItemStack[]{reward.getItem()}));
            } else {
                rewardConfig.set(path + ".command", reward.getCommand());
            }
        }

        try {
            rewardConfig.save(rewardFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save rewards for boss: " + boss.getConfigName(), e);
        }
    }

    private void loadRewardsForBoss(Boss boss) {
        File rewardFile = new File(rewardsFolder, boss.getConfigName() + ".yml");
        if (!rewardFile.exists()) return;

        FileConfiguration rewardConfig = YamlConfiguration.loadConfiguration(rewardFile);
        List<Reward> rewards = new ArrayList<>();

        ConfigurationSection rewardsSection = rewardConfig.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            for (String key : rewardsSection.getKeys(false)) {
                String path = "rewards." + key;
                try {
                    String typeStr = rewardConfig.getString(path + ".type", "");
                    Reward.RewardType type = null;
                    try {
                        if (!typeStr.isEmpty()) {
                            type = Reward.RewardType.valueOf(typeStr.toUpperCase());
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid reward type '" + typeStr + "' for boss " + boss.getConfigName() + " at key " + key + ". Skipping reward.");
                        continue;
                    }

                    Reward reward = new Reward(type);

                    reward.setTarget(Reward.RewardTarget.valueOf(rewardConfig.getString(path + ".target", "KILLER").toUpperCase()));
                    reward.setChance(rewardConfig.getDouble(path + ".chance", 100.0));

                    if (reward.getType() == Reward.RewardType.ITEM) {
                        ItemStack[] items = ItemSerializer.itemStackArrayFromBase64(rewardConfig.getString(path + ".item", ""));
                        if (items != null && items.length > 0) reward.setItem(items[0]);
                    } else {
                        reward.setCommand(rewardConfig.getString(path + ".command", ""));
                    }

                    if ( (reward.getType() == Reward.RewardType.ITEM && reward.getItem() != null) ||
                            (reward.getType() == Reward.RewardType.COMMAND && !reward.getCommand().isEmpty()) ) {
                        rewards.add(reward);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not load a reward for boss " + boss.getConfigName() + ". Check reward file format at key: " + key + ". Error: " + e.getMessage());
                }
            }
        }
        boss.setRewards(rewards);
    }

    public void reloadBosses() {
        loadAllBosses();
    }

    public void registerActiveBoss(Entity entity, Boss boss) { activeBosses.put(entity.getUniqueId(), boss); }
    public void unregisterActiveBoss(Entity entity) { activeBosses.remove(entity.getUniqueId()); }
    public boolean isActiveBoss(Entity entity) { return activeBosses.containsKey(entity.getUniqueId()); }
    public Boss getBossFromEntity(Entity entity) { return activeBosses.get(entity.getUniqueId()); }
    public Boss getBoss(String configName) { return loadedBosses.get(configName.toLowerCase()); }
    public List<Boss> getAllBosses() { return new ArrayList<>(loadedBosses.values()); }
}