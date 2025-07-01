package com.wattbreak.promobboss.integrations;

import com.wattbreak.promobboss.ProMobBoss;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class DiscordWebhookHandler {

    private final ProMobBoss plugin;

    public DiscordWebhookHandler(ProMobBoss plugin) {
        this.plugin = plugin;
    }

    /**
     * A specific type of message (e.g. “boss-spawn”, “boss-kill”) in the Discord webhook settings
     * checks if it is enabled.
     * @param messageKey Key of the message to check (e.g. “boss-spawn”).
     * @return True if enabled, false otherwise.
     */
    public boolean isEnabled(String messageKey) {
        ConfigurationSection config = plugin.getConfigManager().getDiscordConfig();
        return config != null &&
                config.getBoolean("enabled", false) &&
                config.isConfigurationSection("messages." + messageKey) &&
                config.getBoolean("messages." + messageKey + ".enabled", true);
    }

    public void sendMessage(String messageKey, Map<String, String> placeholders) {
        ConfigurationSection config = plugin.getConfigManager().getDiscordConfig();
        if (!isEnabled(messageKey)) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    String webhookName = config.getString("messages." + messageKey + ".webhook", "default");
                    String webhookUrl = config.getString("webhooks." + webhookName);

                    if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("PASTE_YOUR_WEBHOOK_URL_HERE")) {
                        plugin.getLogger().warning("Discord webhook URL for '" + webhookName + "' is not configured in discord.yml. Message '" + messageKey + "' not sent.");
                        return;
                    }
                    URL url = new URL(webhookUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json; utf-8");
                    connection.setRequestProperty("User-Agent", "ProMobBoss");
                    connection.setDoOutput(true);

                    JSONObject json = createJsonPayload(config.getConfigurationSection("messages." + messageKey), placeholders);

                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = json.toJSONString().getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }

                    int responseCode = connection.getResponseCode();
                    if (responseCode >= 300) {
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"))) {
                            StringBuilder response = new StringBuilder();
                            String responseLine;
                            while ((responseLine = br.readLine()) != null) {
                                response.append(responseLine.trim());
                            }
                            plugin.getLogger().warning("Discord webhook returned an error code: " + responseCode + " - Response: " + response.toString() + " for message: " + messageKey);
                        }
                    } else if (responseCode >= 200 && responseCode < 300) {
                        plugin.getLogger().fine("Discord webhook message '" + messageKey + "' sent successfully (Response: " + responseCode + ").");
                    }

                } catch (Exception e) {
                    plugin.getLogger().warning("Could not send Discord webhook message '" + messageKey + "': " + e.getMessage());
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    @SuppressWarnings("unchecked")
    private JSONObject createJsonPayload(ConfigurationSection section, Map<String, String> placeholders) {
        JSONObject payload = new JSONObject();

        addIfPresent(payload, "content", section, placeholders);
        addIfPresent(payload, "username", section, placeholders);
        addIfPresent(payload, "avatar_url", section, placeholders);

        if (!section.isConfigurationSection("embed")) {
            return payload;
        }

        JSONObject embed = new JSONObject();
        ConfigurationSection embedSection = section.getConfigurationSection("embed");

        addIfPresent(embed, "title", embedSection, placeholders);
        addIfPresent(embed, "description", embedSection, placeholders);

        if (embedSection.contains("color")) {
            embed.put("color", embedSection.getInt("color"));
        }

        if (embedSection.isConfigurationSection("author")) {
            JSONObject author = new JSONObject();
            addIfPresent(author, "name", embedSection.getConfigurationSection("author"), placeholders);
            addIfPresent(author, "icon_url", embedSection.getConfigurationSection("author"), placeholders);
            if (!author.isEmpty()) embed.put("author", author);
        }

        if (embedSection.isConfigurationSection("thumbnail")) {
            JSONObject thumbnail = new JSONObject();
            addIfPresent(thumbnail, "url", embedSection.getConfigurationSection("thumbnail"), placeholders);
            if(!thumbnail.isEmpty()) embed.put("thumbnail", thumbnail);
        }

        if (embedSection.isList("fields")) {
            JSONArray fields = new JSONArray();
            for (Map<?, ?> fieldMap : embedSection.getMapList("fields")) {
                JSONObject field = new JSONObject();

                Object nameObj = fieldMap.get("name");
                Object valueObj = fieldMap.get("value");
                Object inlineObj = fieldMap.get("inline");

                String name = (nameObj != null) ? replacePlaceholders(String.valueOf(nameObj), placeholders) : "";
                String value = (valueObj != null) ? replacePlaceholders(String.valueOf(valueObj), placeholders) : "";

                if (name.isEmpty() || value.isEmpty()) {
                    plugin.getLogger().warning("Discord webhook field 'name' or 'value' is empty. Skipping field.");
                    continue;
                }

                field.put("name", name);
                field.put("value", value);

                boolean isInline = (inlineObj instanceof Boolean) ? (Boolean) inlineObj : false;
                field.put("inline", isInline);

                fields.add(field);
            }
            if (!fields.isEmpty()) {
                embed.put("fields", fields);
            }
        }

        if (embedSection.isConfigurationSection("image")) {
            JSONObject image = new JSONObject();
            addIfPresent(image, "url", embedSection.getConfigurationSection("image"), placeholders);
            if(!image.isEmpty()) embed.put("image", image);
        }

        if (embedSection.isConfigurationSection("footer")) {
            JSONObject footer = new JSONObject();
            addIfPresent(footer, "text", embedSection.getConfigurationSection("footer"), placeholders);
            if(!footer.isEmpty()) embed.put("footer", footer);
        }

        if (embedSection.getBoolean("timestamp", false)) {
            embed.put("timestamp", Instant.now().toString());
        }

        if (!embed.isEmpty()) {
            payload.put("embeds", Arrays.asList(embed));
        }
        return payload;
    }

    @SuppressWarnings("unchecked")
    private void addIfPresent(JSONObject json, String key, ConfigurationSection section, Map<String, String> placeholders) {
        String rawValue = section.getString(key);
        String value = replacePlaceholders(rawValue, placeholders);
        if (value != null && !value.isEmpty()) {
            json.put(key, value);
        }
    }

    private String replacePlaceholders(String text, Map<String, String> placeholders) {
        if (text == null) return null;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }
}