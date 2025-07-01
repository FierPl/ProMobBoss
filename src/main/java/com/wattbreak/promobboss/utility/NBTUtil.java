package com.wattbreak.promobboss.utility;

import com.wattbreak.promobboss.ProMobBoss;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

public class NBTUtil {

    private static final ProMobBoss plugin = ProMobBoss.getInstance();

    /**
     * Adds a String NBT tag to a PersistentDataHolder (Entity, ItemStack Meta, etc.).
     * @param holder Object to add NBT to.
     * @param key Key of the tag.
     * @param value The value of the tag.
     */
    public static void setString(PersistentDataHolder holder, String key, String value) {
        if (holder == null) return;
        NamespacedKey nbtKey = new NamespacedKey(plugin, key);
        holder.getPersistentDataContainer().set(nbtKey, PersistentDataType.STRING, value);
    }

    /**
     * Reads a String NBT tag from a PersistentDataHolder.
     * @param holder Object to read NBT from.
     * @param key Key of the tag.
     * @return The value of the tag, or null if not found.
     */
    public static String getString(PersistentDataHolder holder, String key) {
        if (holder == null) return null;
        NamespacedKey nbtKey = new NamespacedKey(plugin, key);
        if (holder.getPersistentDataContainer().has(nbtKey, PersistentDataType.STRING)) {
            return holder.getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);
        }
        return null;
    }
}