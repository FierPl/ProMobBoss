package com.wattbreak.promobboss.config;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.utility.ChatHelper;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class LanguageManager {

    private final ProMobBoss plugin;
    private final Map<String, FileConfiguration> loadedLanguageFiles = new HashMap<>();
    private FileConfiguration activeLangConfig;
    private String currentPrefix;

    public LanguageManager(ProMobBoss plugin) {
        this.plugin = plugin;
        loadAllLanguageFiles();
        setActiveLanguage(plugin.getConfigManager().getMainConfig().getString("language", "en_US"));
    }

    /**
     * Copies default language files from JAR to disk and installs all language files.
     */
    private void loadAllLanguageFiles() {
        loadedLanguageFiles.clear();

        List<String> defaultLanguages = Arrays.asList("en_US.yml", "tr_TR.yml", "fr_FR.yml");

        for (String langFileName : defaultLanguages) {
            saveDefaultLanguageFile(langFileName);
        }

        File langFolder = new File(plugin.getDataFolder(), "languages");
        File[] langFiles = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (langFiles == null) {
            plugin.getLogger().warning("No language files found in 'languages' folder after attempting to save defaults.");
            return;
        }

        for (File file : langFiles) {
            String langCode = file.getName().replace(".yml", "");
            loadedLanguageFiles.put(langCode.toLowerCase(), YamlConfiguration.loadConfiguration(file));
        }
        plugin.getLogger().info("Loaded " + loadedLanguageFiles.size() + " language file(s).");
    }

    /**
     * Copies a given language file from a JAR if it is not on disk.
     * @param fileName Name of the file to copy (e.g. “tr_TR.yml”).
     */
    private void saveDefaultLanguageFile(String fileName) {
        File langFile = new File(plugin.getDataFolder(), "languages/" + fileName);
        if (!langFile.exists()) {
            try {
                plugin.saveResource("languages/" + fileName, false);
                plugin.getLogger().info("Default language file created: " + fileName);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "FATAL: Could not find the embedded resource '" + "languages/" + fileName + "'. " +
                                "Please ensure this file exists in your project's 'src/main/resources/languages/' directory and that your project is built correctly.", e);
            }
        }
    }

    /**
     * Sets the active language and updates the prefix.
     * @param langCode The code of the language to load (e.g. “en_US”, “en_TR”).
     */
    public void setActiveLanguage(String langCode) {
        FileConfiguration newConfig = loadedLanguageFiles.get(langCode.toLowerCase());
        if (newConfig == null) {
            plugin.getLogger().warning("Language file '" + langCode + ".yml' not found. Defaulting to en_US.");
            newConfig = loadedLanguageFiles.get("en_us");
            if (newConfig == null) {
                plugin.getLogger().severe("No default language file (en_US.yml) found! Messages will be broken.");
                return;
            }
        }
        this.activeLangConfig = newConfig;
        this.currentPrefix = ChatHelper.colorize(activeLangConfig.getString("prefix", "&8[&bPro&3MB&8] &r"));
        plugin.getLogger().info("Active language set to: " + langCode);
    }

    /**
     * Retrieves the message from the active language file.
     * @param path Path to the message.
     * @param def Default value if message not found.
     * @return Raw message string.
     */
    public String getRawMessage(String path, String def) {
        if (activeLangConfig == null) {
            plugin.getLogger().severe("Active language config is null! Path: " + path);
            return def;
        }
        return activeLangConfig.getString(path, def);
    }

    /**
     * Retrieves the message from the active language file, modifies variables and applies color codes.
     */
    public String getMessage(String path, String... replacements) {
        String message = getRawMessage(path, "&cMessage not found: " + path);
        if (!path.equalsIgnoreCase("dummy")) {
            message = message.replace("{prefix}", this.currentPrefix);
        } else {
            message = message.replace("{prefix}", "");
        }
        if (replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                if (replacements[i] != null && replacements[i+1] != null) {
                    message = message.replace(replacements[i], replacements[i + 1]);
                }
            }
        }
        return ChatHelper.colorize(message);
    }

    /**
     * Gets a text list (for lore) from the active language file.
     */
    public List<String> getMessageList(String path, String... replacements) {
        if (activeLangConfig == null) {
            plugin.getLogger().severe("Active language config is null! Path: " + path);
            List<String> errorList = new ArrayList<>();
            errorList.add("&cActive language config is null!");
            errorList.add("&cList not found: " + path);
            return errorList;
        }
        List<String> rawList = activeLangConfig.getStringList(path);
        if (rawList == null || rawList.isEmpty()) {
            List<String> errorList = new ArrayList<>();
            errorList.add("&cList not found: " + path);
            return errorList;
        }
        return rawList.stream().map(line -> {
            String processedLine = line;
            if (replacements.length % 2 == 0) {
                for (int i = 0; i < replacements.length; i += 2) {
                    if (replacements[i] != null && replacements[i+1] != null) {
                        processedLine = processedLine.replace(replacements[i], replacements[i + 1]);
                    }
                }
            }
            return ChatHelper.colorize(processedLine);
        }).collect(Collectors.toList());
    }

    public void reloadLanguage() {
        loadAllLanguageFiles();
        setActiveLanguage(plugin.getConfigManager().getMainConfig().getString("language", "en_US"));
        plugin.getLogger().info("Language files reloaded.");
    }
}