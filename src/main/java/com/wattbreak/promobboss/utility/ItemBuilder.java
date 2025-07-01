package com.wattbreak.promobboss.utility;

import com.google.common.collect.Lists;
import com.wattbreak.promobboss.ProMobBoss;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemBuilder {

    private ItemStack itemStack;
    private ItemMeta itemMeta;

    private final ProMobBoss plugin = ProMobBoss.getInstance();

    /**
     * Used to create a new ItemStack.
     * @param material Initial material.
     */
    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = this.itemStack.getItemMeta();
    }

    /**
     * --- NEWLY ADDED CONSTRUCTOR ---
     * Used to edit an existing ItemStack.
     * @param itemStack ItemStack to edit.
     */
    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.itemMeta = itemStack.getItemMeta();
    }

    public ItemBuilder setName(String name) {
        itemMeta.setDisplayName(ChatHelper.colorize(name));
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        itemMeta.setLore(ChatHelper.colorize(Arrays.asList(lore)));
        return this;
    }

    public ItemBuilder addLoreLine(String line) {
        if (itemMeta != null) {
            List<String> lore = itemMeta.hasLore() ? new ArrayList<>(itemMeta.getLore()) : new ArrayList<>();
            lore.add(ChatHelper.colorize(line));
            itemMeta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder addLore(List<String> linesToAdd) {
        if (itemMeta != null) {
            List<String> lore = itemMeta.hasLore() ? new ArrayList<>(itemMeta.getLore()) : new ArrayList<>();
            lore.addAll(ChatHelper.colorize(linesToAdd));
            itemMeta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder setMaterial(Material material) {
        this.itemStack.setType(material);
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        itemMeta.setLore(ChatHelper.colorize(lore));
        return this;
    }

    public ItemBuilder addEnchant(Enchantment enchantment, int level, boolean ignoreRestrictions) {
        itemMeta.addEnchant(enchantment, level, ignoreRestrictions);
        return this;
    }

    public ItemBuilder setGlowing(boolean glowing) {
        if (glowing) {
            itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder addItemFlags(ItemFlag... flags) {
        itemMeta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder setCustomModelData(int data) {
        itemMeta.setCustomModelData(data);
        return this;
    }

    public ItemStack build() {
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public ItemBuilder setNbt(String key, String value) {
        if (itemMeta != null) {
            NamespacedKey nbtKey = new NamespacedKey(ProMobBoss.getInstance(), key);
            itemMeta.getPersistentDataContainer().set(nbtKey, PersistentDataType.STRING, value);
        }
        return this;
    }

    public static String getNbt(ItemStack item, String key) {
        if (item == null || !item.hasItemMeta()) return null;
        NamespacedKey nbtKey = new NamespacedKey(ProMobBoss.getInstance(), key);
        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(nbtKey, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);
        }
        return null;
    }
}