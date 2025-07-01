package com.wattbreak.promobboss.integrations;

import com.wattbreak.promobboss.ProMobBoss;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.api.mobs.MythicMob;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class MythicMobsHandler {

    private final ProMobBoss plugin;
    private final boolean ready;

    public MythicMobsHandler(ProMobBoss plugin) {
        this.plugin = plugin;
        boolean isReady = false;
        try {
            MythicBukkit.inst();
            isReady = true;
            plugin.getLogger().info("Successfully hooked into MythicMobs.");
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Could not hook into MythicMobs API. Integration will be disabled.", t);
        }
        this.ready = isReady;
    }

    public boolean isReady() {
        return ready;
    }

    public List<String> getMobInternalNames() {
        List<String> mobNames = new ArrayList<>();
        if (!isReady()) {
            return mobNames;
        }
        try {
            for (MythicMob mob : MythicBukkit.inst().getMobManager().getMobTypes()) {
                if (mob != null && mob.getInternalName() != null) {
                    mobNames.add(mob.getInternalName());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "An error occurred while fetching MythicMobs list.", e);
        }
        return mobNames;
    }

    public Entity spawnMythicMob(String internalName, Location location) {
        if (!isReady()) return null;
        try {
            return MythicBukkit.inst().getAPIHelper().spawnMythicMob(internalName, location);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to spawn MythicMob: " + internalName, e);
            return null;
        }
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
}