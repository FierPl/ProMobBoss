package com.wattbreak.promobboss.config;

import org.bukkit.configuration.file.FileConfiguration;
import com.wattbreak.promobboss.ProMobBoss;

public class ConfigManager {

    private final ProMobBoss plugin;

    private FileManager settingsFile;
    private FileManager discordFile;

    public ConfigManager(ProMobBoss plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveDefaultConfig();

        settingsFile = new FileManager(plugin, "setting.yml", null);
        discordFile = new FileManager(plugin, "discord.yml", null);

        plugin.getLogger().info("All configuration files have been loaded.");
    }

    public void reloadConfigs() {
        plugin.reloadConfig(); // Main config.yml
        settingsFile.reloadConfig();
        discordFile.reloadConfig();

        plugin.getLogger().info("All configuration files have been reloaded.");
    }

    public FileConfiguration getMainConfig() {
        return plugin.getConfig();
    }

    public FileConfiguration getSettingsConfig() {
        return settingsFile.getConfig();
    }

    public FileConfiguration getDiscordConfig() {
        return discordFile.getConfig();
    }
}