package com.wattbreak.promobboss.common;

import com.wattbreak.promobboss.gui.menus.mob.EquipmentSelectorMenu;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class VanillaMobSettings {
    private String entityType = "ZOMBIE";
    private double health = 100.0;
    private double damage = 10.0;

    private String mainHand = "AIR";
    private String helmet = "AIR";
    private String chestplate = "AIR";
    private String leggings = "AIR";
    private String boots = "AIR";

    private Map<String, Integer> mainHandEnchants = new HashMap<>();
    private Map<String, Integer> helmetEnchants = new HashMap<>();
    private Map<String, Integer> chestplateEnchants = new HashMap<>();
    private Map<String, Integer> leggingsEnchants = new HashMap<>();
    private Map<String, Integer> bootsEnchants = new HashMap<>();

    public String getEquipment(EquipmentSelectorMenu.EquipmentType type) {
        switch (type) {
            case WEAPON: return getMainHand();
            case HELMET: return getHelmet();
            case CHESTPLATE: return getChestplate();
            case LEGGINGS: return getLeggings();
            case BOOTS: return getBoots();
            default: return "AIR";
        }
    }

    public void setEquipment(EquipmentSelectorMenu.EquipmentType type, String material) {
        switch (type) {
            case WEAPON: setMainHand(material); break;
            case HELMET: setHelmet(material); break;
            case CHESTPLATE: setChestplate(material); break;
            case LEGGINGS: setLeggings(material); break;
            case BOOTS: setBoots(material); break;
        }
    }

    public Map<String, Integer> getEnchantMapFor(EquipmentSelectorMenu.EquipmentType type) {
        switch (type) {
            case WEAPON: return getMainHandEnchants();
            case HELMET: return getHelmetEnchants();
            case CHESTPLATE: return getChestplateEnchants();
            case LEGGINGS: return getLeggingsEnchants();
            case BOOTS: return getBootsEnchants();
            default: return new HashMap<>();
        }
    }


}