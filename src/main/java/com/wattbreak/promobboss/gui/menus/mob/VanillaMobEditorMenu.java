package com.wattbreak.promobboss.gui.menus.mob;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.boss.Boss;
import com.wattbreak.promobboss.common.IMobEditable;
import com.wattbreak.promobboss.common.VanillaMobSettings;
import com.wattbreak.promobboss.gui.Menu;
import com.wattbreak.promobboss.mob.Mob;
import com.wattbreak.promobboss.utility.ChatHelper;
import com.wattbreak.promobboss.utility.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class VanillaMobEditorMenu extends Menu {

    private final IMobEditable entity;
    private final VanillaMobSettings settings;
    private final Menu parentMenu;

    private static final int SLOT_MAINHAND = 10;
    private static final int SLOT_HELMET = 19;
    private static final int SLOT_CHESTPLATE = 28;
    private static final int SLOT_LEGGINGS = 37;
    private static final int SLOT_BOOTS = 46;

    private static final int SLOT_ENTITY_TYPE = 16;
    private static final int SLOT_HEALTH = 25;
    private static final int SLOT_DAMAGE = 34;

    private static final int SLOT_BACK = 49;

    private static final List<EntityType> SUPPORTED_TYPES = Arrays.asList(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CAVE_SPIDER,
            EntityType.CREEPER, EntityType.WITHER_SKELETON, EntityType.STRAY, EntityType.HUSK,
            EntityType.VINDICATOR, EntityType.PILLAGER, EntityType.PIGLIN_BRUTE, EntityType.BLAZE
    );

    public VanillaMobEditorMenu(Player player, IMobEditable entity, Menu parentMenu) {
        super(player);
        this.entity = entity;
        this.settings = entity.getVanillaMobSettings();
        this.parentMenu = parentMenu;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessage("gui.vanilla-editor.title");
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        int slot = event.getSlot();
        ClickType click = event.getClick();

        EquipmentSelectorMenu.EquipmentType equipmentType = getEquipmentTypeFromSlot(slot);
        if (equipmentType != null) {
            handleEquipmentClick(click, equipmentType);
            return;
        }

        switch (slot) {
            case SLOT_ENTITY_TYPE:
                if (click.isLeftClick()) changeEntityType();
                break;
            case SLOT_HEALTH:
                if (click.isLeftClick()) handleNumericInput("health", "Can");
                return;
            case SLOT_DAMAGE:
                if (click.isLeftClick()) handleNumericInput("damage", "Hasar");
                return;
            case SLOT_BACK:
                parentMenu.open();
                return;
            default:
                return;
        }
        saveAndReload(false);
    }

    private void handleEquipmentClick(ClickType click, EquipmentSelectorMenu.EquipmentType type) {
        if (click.isLeftClick() && !click.isShiftClick()) {
            new EquipmentSelectorMenu(player, entity, type, this).open();
        }
        else if (click.isShiftClick() && click.isLeftClick()) {
            new EnchantmentSelectorMenu(player, entity, type, this).open();
        }
        else if (click.isRightClick()) {
            ItemStack handItem = player.getInventory().getItemInMainHand();

            if (handItem == null || handItem.getType() == Material.AIR) {
                settings.setEquipment(type, "AIR");
                settings.getEnchantMapFor(type).clear();
                player.sendMessage(plugin.getMessage("gui.vanilla-editor.equipment-removed", "%type%", type.name()));
            } else {
                settings.setEquipment(type, handItem.getType().name());
                settings.getEnchantMapFor(type).clear();
                player.sendMessage(plugin.getMessage("gui.vanilla-editor.equipment-set", "%item%", handItem.getType().name(), "%type%", type.name()));
            }
            saveAndReload(false);
        }
    }

    private void handleNumericInput(String settingType, String settingName) {
        player.closeInventory();
        String cancelKeyword = plugin.getMessage("general.cancel-keyword");
        player.sendMessage(plugin.getMessage("gui.vanilla-editor.prompt-numeric", "%setting%", settingName, "%cancel_keyword%", cancelKeyword));

        plugin.getChatInputHandler().newListener(player, input -> {
            if (input.equalsIgnoreCase(cancelKeyword)) {
                player.sendMessage(plugin.getMessage("gui.boss-editor-menu.prompt-cancelled"));
                new VanillaMobEditorMenu(player, entity, parentMenu).open();
                return;
            }
            try {
                double value = Double.parseDouble(input);
                if (value >= 0) {
                    if ("health".equals(settingType)) settings.setHealth(value);
                    else if ("damage".equals(settingType)) settings.setDamage(value);
                    player.sendMessage(plugin.getMessage("gui.vanilla-editor.value-set", "%setting%", settingName, "%value%", String.format("%.1f", value)));
                } else {
                    player.sendMessage(plugin.getMessage("gui.vanilla-editor.invalid-range-positive"));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessage("gui.vanilla-editor.invalid-number-format"));
            }
            saveAndReload(true);
        });
    }

    private void changeEntityType() {
        EntityType currentType;
        try {
            currentType = EntityType.valueOf(settings.getEntityType().toUpperCase());
        } catch (IllegalArgumentException e) {
            currentType = EntityType.ZOMBIE;
        }
        int currentIndex = SUPPORTED_TYPES.indexOf(currentType);
        int nextIndex = (currentIndex + 1) % SUPPORTED_TYPES.size();
        settings.setEntityType(SUPPORTED_TYPES.get(nextIndex).name());
    }

    private void saveAndReload(boolean openAfter) {
        if (entity instanceof Boss) {
            plugin.getBossManager().saveBoss((Boss) entity);
        } else if (entity instanceof Mob) {
            plugin.getMobManager().saveMob((Mob) entity);
        }

        if (openAfter) {
            new VanillaMobEditorMenu(player, entity, parentMenu).open();
        } else {
            setMenuItems();
        }
    }

    @Override
    public void setMenuItems() {
        inventory.clear();
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < getSlots(); i++) inventory.setItem(i, filler);
        for(int i = 10; i <= 46; i++) if (i%9 != 0 && i%9 != 8) inventory.setItem(i, null);

        setupEquipmentSlot(SLOT_HELMET, EquipmentSelectorMenu.EquipmentType.HELMET, Material.LEATHER_HELMET);
        setupEquipmentSlot(SLOT_CHESTPLATE, EquipmentSelectorMenu.EquipmentType.CHESTPLATE, Material.LEATHER_CHESTPLATE);
        setupEquipmentSlot(SLOT_LEGGINGS, EquipmentSelectorMenu.EquipmentType.LEGGINGS, Material.LEATHER_LEGGINGS);
        setupEquipmentSlot(SLOT_BOOTS, EquipmentSelectorMenu.EquipmentType.BOOTS, Material.LEATHER_BOOTS);
        setupEquipmentSlot(SLOT_MAINHAND, EquipmentSelectorMenu.EquipmentType.WEAPON, Material.DIAMOND_SWORD);

        inventory.setItem(SLOT_ENTITY_TYPE, new ItemBuilder(Material.PLAYER_HEAD).setName(plugin.getMessage("gui.vanilla-editor.item.entity-type.title")).setLore(plugin.getLanguageManager().getMessageList("gui.vanilla-editor.item.entity-type.lore", "%type%", settings.getEntityType())).build());
        inventory.setItem(SLOT_HEALTH, new ItemBuilder(Material.RED_DYE).setName(plugin.getMessage("gui.vanilla-editor.item.health.title")).setLore(plugin.getLanguageManager().getMessageList("gui.vanilla-editor.item.health.lore", "%value%", String.format("%.1f", settings.getHealth()))).build());
        inventory.setItem(SLOT_DAMAGE, new ItemBuilder(Material.BLAZE_POWDER).setName(plugin.getMessage("gui.vanilla-editor.item.damage.title")).setLore(plugin.getLanguageManager().getMessageList("gui.vanilla-editor.item.damage.lore", "%value%", String.format("%.1f", settings.getDamage()))).addItemFlags(ItemFlag.HIDE_ATTRIBUTES).build());

        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.BARRIER).setName(plugin.getMessage("gui.buttons.back")).setLore(plugin.getLanguageManager().getMessageList("gui.buttons.back-lore")).build());
    }

    private void setupEquipmentSlot(int slot, EquipmentSelectorMenu.EquipmentType type, Material defaultIcon) {
        String materialName = settings.getEquipment(type);
        ItemStack item;
        try {
            item = new ItemStack((materialName == null || materialName.equalsIgnoreCase("AIR")) ? defaultIcon : Material.valueOf(materialName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            item = new ItemStack(Material.BARRIER);
            materialName = "GEÇERSİZ";
        }

        ItemBuilder itemBuilder = new ItemBuilder(item)
                .setName(plugin.getMessage("gui.vanilla-editor.item.equipment.title-" + type.name().toLowerCase()))
                .setLore(plugin.getLanguageManager().getMessageList("gui.vanilla-editor.item.equipment.lore", "%item%", materialName))
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        Map<String, Integer> enchants = settings.getEnchantMapFor(type);
        if (enchants != null && !enchants.isEmpty()) {
            itemBuilder.addLoreLine(" ");
            itemBuilder.addLoreLine("&bSpells:");
            enchants.forEach((enchantKey, level) -> {
                itemBuilder.addLoreLine("&7- " + enchantKey.replace('_', ' ').toUpperCase() + " " + level);
            });
        }

      /*  itemBuilder.addLoreLine(" ");
        itemBuilder.addLoreLine(plugin.getMessage("gui.vanilla-editor.item.equipment.lore-select"));
        itemBuilder.addLoreLine(plugin.getMessage("gui.vanilla-editor.item.equipment.lore-equip"));
        itemBuilder.addLoreLine(plugin.getMessage("gui.vanilla-editor.item.equipment.lore-enchant")); */

        inventory.setItem(slot, itemBuilder.build());
    }

    private EquipmentSelectorMenu.EquipmentType getEquipmentTypeFromSlot(int slot) {
        switch (slot) {
            case SLOT_MAINHAND: return EquipmentSelectorMenu.EquipmentType.WEAPON;
            case SLOT_HELMET: return EquipmentSelectorMenu.EquipmentType.HELMET;
            case SLOT_CHESTPLATE: return EquipmentSelectorMenu.EquipmentType.CHESTPLATE;
            case SLOT_LEGGINGS: return EquipmentSelectorMenu.EquipmentType.LEGGINGS;
            case SLOT_BOOTS: return EquipmentSelectorMenu.EquipmentType.BOOTS;
            default: return null;
        }
    }
}