package com.wattbreak.promobboss.mob;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.common.IMobEditable; // Yeni import (Mob yerine)
import com.wattbreak.promobboss.common.VanillaMobSettings;
import com.wattbreak.promobboss.reward.Reward;
import com.wattbreak.promobboss.utility.ItemSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class MobManager {

    private final ProMobBoss plugin;
    private final Map<String, Mob> loadedMobs = new HashMap<>();
    private final File mobsFolder;
    private final File rewardsFolder; // Mob ödül dosyaları için

    public MobManager(ProMobBoss plugin) {
        this.plugin = plugin;
        this.mobsFolder = new File(plugin.getDataFolder(), "Mobs");
        if (!mobsFolder.exists()) {
            mobsFolder.mkdirs();
        }
        this.rewardsFolder = new File(plugin.getDataFolder(), "Mobs/Rewards");
        if (!rewardsFolder.exists()) {
            rewardsFolder.mkdirs();
        }
        loadAllMobs();
    }

    public void loadAllMobs() {
        loadedMobs.clear();
        File[] mobFiles = mobsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (mobFiles == null) {
            plugin.getLogger().info("No custom mob configurations found in 'Mobs' folder.");
            return;
        }

        for (File file : mobFiles) {
            String configName = file.getName().replace(".yml", "");
            FileConfiguration mobConfig = YamlConfiguration.loadConfiguration(file);
            Mob mob = new Mob(configName);

            mob.setDisplayName(mobConfig.getString("display-name", "&a" + configName));
            mob.setEnabled(mobConfig.getBoolean("enabled", false));

            mob.setSpawnWorld(mobConfig.getString("spawn-settings.world", "world"));
            mob.setSpawnInterval(mobConfig.getInt("spawn-settings.interval", 30));
            mob.setMaxAlive(mobConfig.getInt("spawn-settings.max-alive", 5));

            mob.setMobTypeSource(mobConfig.getString("mob-type-source", "VANILLA_CUSTOM"));
            mob.setMythicMobName(mobConfig.getString("mythic-mob-name"));

            ConfigurationSection attributesSection = mobConfig.getConfigurationSection("attributes");
            if (attributesSection != null) {
                VanillaMobSettings settings = mob.getVanillaMobSettings();
                settings.setEntityType(attributesSection.getString("entity-type", "ZOMBIE"));
                settings.setHealth(attributesSection.getDouble("health", 20.0));
                settings.setDamage(attributesSection.getDouble("damage", 3.0));

                ConfigurationSection equipSection = attributesSection.getConfigurationSection("equipment");
                if (equipSection != null) {
                    settings.setMainHand(equipSection.getString("main-hand", "AIR"));
                    settings.setHelmet(equipSection.getString("helmet", "AIR"));
                    settings.setChestplate(equipSection.getString("chestplate", "AIR"));
                    settings.setLeggings(equipSection.getString("leggings", "AIR"));
                    settings.setBoots(equipSection.getString("boots", "AIR"));
                }
            }

            loadRewardsForMob(mob);
            loadedMobs.put(configName.toLowerCase(), mob);
        }
        plugin.getLogger().info("Loaded " + loadedMobs.size() + " custom mob configuration(s).");
    }

    public void saveMob(Mob mob) {
        File mobFile = new File(mobsFolder, mob.getConfigName() + ".yml");
        FileConfiguration mobConfig = new YamlConfiguration();

        mobConfig.set("display-name", mob.getDisplayName());
        mobConfig.set("enabled", mob.isEnabled());

        mobConfig.set("spawn-settings.world", mob.getSpawnWorld());
        mobConfig.set("spawn-settings.interval", mob.getSpawnInterval());
        mobConfig.set("spawn-settings.max-alive", mob.getMaxAlive());

        mobConfig.set("mob-type-source", mob.getMobTypeSource());
        mobConfig.set("mythic-mob-name", mob.getMythicMobName());

        VanillaMobSettings vanillaSettings = mob.getVanillaMobSettings();
        mobConfig.set("attributes.entity-type", vanillaSettings.getEntityType());
        mobConfig.set("attributes.health", vanillaSettings.getHealth());
        mobConfig.set("attributes.damage", vanillaSettings.getDamage());
        mobConfig.set("attributes.equipment.main-hand", vanillaSettings.getMainHand());
        mobConfig.set("attributes.equipment.helmet", vanillaSettings.getHelmet());
        mobConfig.set("attributes.equipment.chestplate", vanillaSettings.getChestplate());
        mobConfig.set("attributes.equipment.leggings", vanillaSettings.getLeggings());
        mobConfig.set("attributes.equipment.boots", vanillaSettings.getBoots());

        try {
            mobConfig.save(mobFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save mob file: " + mob.getConfigName(), e);
        }

        if (plugin.getMobSpawner() != null) {
            plugin.getMobSpawner().updateSpawningForMob(mob);
        }
        saveRewardsForMob(mob);
    }

    public boolean createNewMob(String configName) {
        if (loadedMobs.containsKey(configName.toLowerCase())) {
            return false;
        }

        Mob mob = new Mob(configName);
        loadedMobs.put(configName.toLowerCase(), mob);
        saveMob(mob);
        return true;
    }

    public boolean removeMob(Mob mob) {
        if (mob == null) return false;
        String configName = mob.getConfigName();

        loadedMobs.remove(configName.toLowerCase());

        if (plugin.getMobSpawner() != null) {
            plugin.getMobSpawner().removeMobFromSpawning(mob);
        }

        new File(mobsFolder, configName + ".yml").delete();
        new File(rewardsFolder, configName + ".yml").delete(); // Ödül dosyasını da sil

        return true;
    }

    public void saveRewardsForMob(Mob mob) {
        File rewardFile = new File(rewardsFolder, mob.getConfigName() + ".yml");
        FileConfiguration rewardConfig = new YamlConfiguration();

        List<Reward> rewards = mob.getRewards();
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
            plugin.getLogger().log(Level.SEVERE, "Could not save rewards for mob: " + mob.getConfigName(), e);
        }
    }

    private void loadRewardsForMob(Mob mob) {
        File rewardFile = new File(rewardsFolder, mob.getConfigName() + ".yml");
        if (!rewardFile.exists()) return;

        FileConfiguration rewardConfig = YamlConfiguration.loadConfiguration(rewardFile);
        List<Reward> rewards = new ArrayList<>();

        ConfigurationSection rewardsSection = rewardConfig.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            for (String key : rewardsSection.getKeys(false)) {
                String path = "rewards." + key;
                try {
                    Reward.RewardType type = Reward.RewardType.valueOf(rewardConfig.getString(path + ".type", ""));
                    Reward reward = new Reward(type);
                    reward.setTarget(Reward.RewardTarget.valueOf(rewardConfig.getString(path + ".target", "KILLER")));
                    reward.setChance(rewardConfig.getDouble(path + ".chance", 100.0));

                    if (type == Reward.RewardType.ITEM) {
                        ItemStack[] items = ItemSerializer.itemStackArrayFromBase64(rewardConfig.getString(path + ".item", ""));
                        if (items.length > 0) reward.setItem(items[0]);
                    } else {
                        reward.setCommand(rewardConfig.getString(path + ".command"));
                    }
                    if ( (type == Reward.RewardType.ITEM && reward.getItem() != null) || (type == Reward.RewardType.COMMAND && reward.getCommand() != null) ) {
                        rewards.add(reward);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not load a reward for mob " + mob.getConfigName() + ". Check reward file format at key: " + key);
                }
            }
        }
        mob.setRewards(rewards);
    }

    public void reloadMobs() {
        loadAllMobs();
    }

    public Mob getMob(String configName) { return loadedMobs.get(configName.toLowerCase()); }
    public List<Mob> getAllMobs() { return new ArrayList<>(loadedMobs.values()); }
}