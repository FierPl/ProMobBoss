package com.wattbreak.promobboss.gui.menus.mob;

import com.wattbreak.promobboss.common.IMobEditable;
import com.wattbreak.promobboss.common.VanillaMobSettings;
import com.wattbreak.promobboss.gui.Menu;
import com.wattbreak.promobboss.gui.PaginatedMenu;
import com.wattbreak.promobboss.utility.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EnchantmentSelectorMenu extends PaginatedMenu {

    private final IMobEditable entity;
    private final EquipmentSelectorMenu.EquipmentType equipmentType;
    private final List<Enchantment> availableEnchants;
    private final Map<String, Integer> currentEnchants;
    private final Menu parentMenu;

    public EnchantmentSelectorMenu(Player player, IMobEditable entity, EquipmentSelectorMenu.EquipmentType type, Menu parentMenu) {
        super(player);
        this.entity = entity;
        this.equipmentType = type;
        this.parentMenu = parentMenu;

        this.availableEnchants = getApplicableEnchants(type);
        this.currentEnchants = getEnchantMapForType(type);
    }

    @Override public String getMenuName() { return plugin.getMessage("gui.enchant-selector.title"); }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        super.handleMenu(event);

        if (event.getSlot() == 49) {
            parentMenu.open();
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() != Material.ENCHANTED_BOOK) return;

        String enchantKey = ItemBuilder.getNbt(clickedItem, "enchant-key");
        if (enchantKey == null) return;

        Enchantment selectedEnchant = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchantKey));
        if (selectedEnchant == null) return;

        player.closeInventory();
        player.sendMessage(plugin.getMessage("gui.enchant-selector.prompt-level"));

        plugin.getChatInputHandler().newListener(player, input -> {
            try {
                int level = Integer.parseInt(input);
                if (level == 0) {
                    currentEnchants.remove(selectedEnchant.getKey().getKey());
                } else if (level > 0 && level <= selectedEnchant.getMaxLevel()) {
                    currentEnchants.put(selectedEnchant.getKey().getKey(), level);
                } else {
                    player.sendMessage("&cInvalid level! Maximum level for this spell: " + selectedEnchant.getMaxLevel());
                }

                // TODO: Save change
                // if (entity instanceof Boss) plugin.getBossManager().saveBoss((Boss)entity);
                // else if (entity instanceof Mob) plugin.getMobManager().saveMob((Mob)entity);

                player.sendMessage(plugin.getMessage("gui.enchant-selector.enchant-set", "%enchant%", enchantKey, "%level%", String.valueOf(level)));
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessage("gui.vanilla-editor.invalid-number-format"));
            }
            new EnchantmentSelectorMenu(player, entity, equipmentType, parentMenu).open();
        });
    }

    @Override
    public void setMenuItems() {
        addMenuBorder();
        inventory.setItem(49, new ItemBuilder(Material.BARRIER)
                .setName(plugin.getMessage("gui.buttons.back"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.buttons.back-lore"))
                .build());

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = page * maxItemsPerPage + i;
            if (index >= availableEnchants.size()) break;

            Enchantment enchant = availableEnchants.get(index);
            String enchantKey = enchant.getKey().getKey();
            int currentLevel = currentEnchants.getOrDefault(enchantKey, 0);

            ItemBuilder enchantItem = new ItemBuilder(Material.ENCHANTED_BOOK)
                    .setName("&b" + enchantKey.replace('_', ' ').toUpperCase())
                    .setLore(plugin.getLanguageManager().getMessageList(
                            "gui.enchant-selector.item.lore",
                            "%level%", String.valueOf(currentLevel)))
                    .setNbt("enchant-key", enchantKey);

            if (currentLevel > 0) {
                enchantItem.setGlowing(true);
            }
            inventory.addItem(enchantItem.build());
        }
    }

    private List<Enchantment> getApplicableEnchants(EquipmentSelectorMenu.EquipmentType type) {
        ItemStack tempItem;
        switch(type) {
            case HELMET: tempItem = new ItemStack(Material.DIAMOND_HELMET); break;
            case CHESTPLATE: tempItem = new ItemStack(Material.DIAMOND_CHESTPLATE); break;
            case LEGGINGS: tempItem = new ItemStack(Material.DIAMOND_LEGGINGS); break;
            case BOOTS: tempItem = new ItemStack(Material.DIAMOND_BOOTS); break;
            case WEAPON: tempItem = new ItemStack(Material.DIAMOND_SWORD); break;
            default: return Arrays.asList(Enchantment.values());
        }

        return Arrays.stream(Enchantment.values())
                .filter(e -> e.canEnchantItem(tempItem))
                .collect(Collectors.toList());
    }

    private Map<String, Integer> getEnchantMapForType(EquipmentSelectorMenu.EquipmentType type) {
        VanillaMobSettings settings = entity.getVanillaMobSettings();
        switch (type) {
            case HELMET: return settings.getHelmetEnchants();
            case CHESTPLATE: return settings.getChestplateEnchants();
            case LEGGINGS: return settings.getLeggingsEnchants();
            case BOOTS: return settings.getBootsEnchants();
            case WEAPON: return settings.getMainHandEnchants();
            default: return new HashMap<>();
        }
    }
}