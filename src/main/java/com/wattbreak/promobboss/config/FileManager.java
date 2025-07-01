package com.wattbreak.promobboss.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.wattbreak.promobboss.ProMobBoss;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public class FileManager {

    private final ProMobBoss plugin;
    private FileConfiguration dataConfig = null;
    private File configFile = null;
    private final String fileName;
    private final String folderName;

    /**
     * Creates a new FileManager for a specific YAML file.
     * @param plugin The main plugin instance.
     * @param fileName The name of the file (e.g., "setting.yml").
     * @param folderName The subfolder inside the plugin's data folder (can be null for root).
     */
    public FileManager(ProMobBoss plugin, String fileName, String folderName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.folderName = folderName;
        saveDefaultConfig();
    }

    public void reloadConfig() {
        if (this.configFile == null) {
            File dataFolder = (folderName != null) ? new File(plugin.getDataFolder(), folderName) : plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdir();
            }
            this.configFile = new File(dataFolder, this.fileName);
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(this.configFile);

        InputStream defaultStream = plugin.getResource( (folderName != null ? folderName + "/" : "") + fileName);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            this.dataConfig.setDefaults(defaultConfig);
        }
    }

    public FileConfiguration getConfig() {
        if (this.dataConfig == null) {
            reloadConfig();
        }
        return this.dataConfig;
    }

    public void saveConfig() {
        if (this.dataConfig == null || this.configFile == null) {
            return;
        }
        try {
            getConfig().save(this.configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + this.configFile, e);
        }
    }

    public void saveDefaultConfig() {
        File dataFolder = (folderName != null) ? new File(plugin.getDataFolder(), folderName) : plugin.getDataFolder();
        this.configFile = new File(dataFolder, fileName);

        if (!this.configFile.exists()) {
            plugin.saveResource((folderName != null ? folderName + "/" : "") + fileName, false);
        }
    }
}