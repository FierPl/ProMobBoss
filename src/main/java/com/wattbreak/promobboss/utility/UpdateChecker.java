package com.wattbreak.promobboss.utility;

import com.wattbreak.promobboss.ProMobBoss;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray; // JSONArray importu
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public class UpdateChecker {

    private final ProMobBoss plugin;
    private final String githubRepo;
    private String latestVersion;
    private boolean updateAvailable = false;

    public UpdateChecker(ProMobBoss plugin, String githubRepo) {
        this.plugin = plugin;
        this.githubRepo = githubRepo;
        checkForUpdate();
    }

    public void checkForUpdate() {
        if (!plugin.getConfigManager().getMainConfig().getBoolean("update-checker", true)) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://api.github.com/repos/" + githubRepo + "/tags");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                    connection.setRequestProperty("User-Agent", "ProMobBoss-Update-Checker");

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        JSONParser parser = new JSONParser();
                        JSONArray tags = (JSONArray) parser.parse(response.toString());

                        if (tags.isEmpty()) {
                            plugin.getLogger().info("No tags found for update checking. Please create a tag on GitHub (e.g., v1.0.1).");
                            return;
                        }

                        JSONObject latestTagObject = (JSONObject) tags.get(0);
                        String latestTag = (String) latestTagObject.get("name");

                        if (latestTag != null && latestTag.startsWith("v")) {
                            latestTag = latestTag.substring(1);
                        }

                        String currentVersion = plugin.getDescription().getVersion().replace("-SNAPSHOT", "");
                        latestVersion = latestTag;

                        if (latestVersion != null && !isSameVersion(currentVersion, latestVersion)) {
                            updateAvailable = true;
                            plugin.getLogger().info("A new version of ProMobBoss is available! Current: v" + currentVersion + ", Latest: v" + latestVersion);
                            plugin.getLogger().info("Download it from: https://github.com/" + githubRepo + "/releases/tag/" + latestTagObject.get("name"));
                        } else {
                            plugin.getLogger().info("ProMobBoss is up to date! (v" + currentVersion + ")");
                        }
                    } else {
                        plugin.getLogger().warning("Failed to check for updates. GitHub API returned: " + responseCode + " for URL: " + url.toString());
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "An error occurred while checking for updates: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private boolean isSameVersion(String current, String latest) {
        return current.equalsIgnoreCase(latest);
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void notifyPlayer(Player player) {
        if (isUpdateAvailable() && player.hasPermission("promb.admin")) {
            player.sendMessage(plugin.getMessage("update-checker.new-version-available",
                    "%current_version%", plugin.getDescription().getVersion(),
                    "%latest_version%", latestVersion));
            player.sendMessage(plugin.getMessage("update-checker.download-link",
                    "%link%", "https://github.com/" + githubRepo + "/releases/latest"));
        }
    }
}