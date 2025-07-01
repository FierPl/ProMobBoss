package com.wattbreak.promobboss.gui.menus.mob;

import com.wattbreak.promobboss.common.IMobEditable;
import com.wattbreak.promobboss.common.IMobTyped;
import com.wattbreak.promobboss.boss.Boss;
import com.wattbreak.promobboss.common.VanillaMobSettings;
import com.wattbreak.promobboss.gui.Menu;
import com.wattbreak.promobboss.gui.PaginatedMenu;
import com.wattbreak.promobboss.utility.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public class EquipmentSelectorMenu extends PaginatedMenu {

    public enum EquipmentType { HELMET, CHESTPLATE, LEGGINGS, BOOTS, WEAPON }

    private final IMobEditable entity;
    private final EquipmentType type;
    private final List<Material> equipmentList;
    private final Menu parentMenu;

    private static final List<Material> HELMETS = Arrays.asList(Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET, Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET, Material.TURTLE_HELMET);
    private static final List<Material> CHESTPLATES = Arrays.asList(Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE, Material.GOLDEN_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE);
    private static final List<Material> LEGGINGS = Arrays.asList(Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS, Material.GOLDEN_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS);
    private static final List<Material> BOOTS = Arrays.asList(Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS, Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS);
    private static final List<Material> WEAPONS = Arrays.asList(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE);

    public EquipmentSelectorMenu(Player player, IMobEditable entity, EquipmentType type, Menu parentMenu) {
        super(player);
        this.entity = entity;
        this.type = type;
        this.parentMenu = parentMenu;
        this.equipmentList = getListForType(type);
    }

    private List<Material> getListForType(EquipmentType type) {
        switch (type) {
            case HELMET: return HELMETS;
            case CHESTPLATE: return CHESTPLATES;
            case LEGGINGS: return LEGGINGS;
            case BOOTS: return BOOTS;
            case WEAPON: return WEAPONS;
            default: return Stream.of(HELMETS, CHESTPLATES, LEGGINGS, BOOTS, WEAPONS).flatMap(List::stream).collect(Collectors.toList());
        }
    }

    @Override public String getMenuName() { return plugin.getMessage("gui.equipment-selector.title", "%type%", type.name()); }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        super.handleMenu(event);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        if (event.getSlot() == 49 && clickedItem.getType() == Material.BARRIER) {
            parentMenu.open();
            return;
        }

        Material material = clickedItem.getType();
        if (material == Material.BLACK_STAINED_GLASS_PANE || material.name().contains("BUTTON") || material == Material.BOOK) {
            return;
        }
        if (!equipmentList.contains(material)) return;

        VanillaMobSettings settings = entity.getVanillaMobSettings();
        String materialName = material.name();

        switch (type) {
            case HELMET: settings.setHelmet(materialName); break;
            case CHESTPLATE: settings.setChestplate(materialName); break;
            case LEGGINGS: settings.setLeggings(materialName); break;
            case BOOTS: settings.setBoots(materialName); break;
            case WEAPON: settings.setMainHand(materialName); break;
        }

        // TODO: Save change
        // if (mobTyped instanceof Boss) plugin.getBossManager().saveBoss((Boss) mobTyped);
        // ...

        player.sendMessage(plugin.getMessage("gui.equipment-selector.item-selected", "%item%", materialName));
        parentMenu.open();
    }

    @Override
    public void setMenuItems() {
        addMenuBorder();
        inventory.setItem(49, new ItemBuilder(Material.BARRIER)
                .setName(plugin.getMessage("gui.buttons.back"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.buttons.back-lore"))
                .build());

        if (equipmentList != null && !equipmentList.isEmpty()) {
            for (int i = 0; i < maxItemsPerPage; i++) {
                index = maxItemsPerPage * page + i;
                if (index >= equipmentList.size()) break;

                Material currentMaterial = equipmentList.get(index);
                ItemStack equipmentItem = new ItemBuilder(currentMaterial)
                        .setName("&e" + currentMaterial.name().replace("_", " ").toLowerCase())
                        .setLore(plugin.getLanguageManager().getMessageList("gui.equipment-selector.lore"))
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build();
                inventory.addItem(equipmentItem);
            }
        }
    }
}