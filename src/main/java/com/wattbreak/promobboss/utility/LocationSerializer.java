package com.wattbreak.promobboss.utility;

import com.wattbreak.promobboss.ProMobBoss;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class LocationSerializer {

    /**
     * Writes a Location object to a ConfigurationSection.
     * @param location Location to save.
     * @param section YML section where the data will be written.
     */
    public static void serialize(Location location, ConfigurationSection section) {
        section.set("world", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }

    /**
     * Creates a Location object from a ConfigurationSection.
     * @param section The YML section from which to read the data.
     * @return Location object created, or null in case of error.
     */
    public static Location deserialize(ConfigurationSection section) {
        if (section == null) return null;

        String worldName = section.getString("world");
        if (worldName == null) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            ProMobBoss.getInstance().getLogger().warning("Could not load world '" + worldName + "' for a boss location. Is the world loaded?");
            return null;
        }

        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }
}