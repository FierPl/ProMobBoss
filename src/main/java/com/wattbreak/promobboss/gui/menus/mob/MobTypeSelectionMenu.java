package com.wattbreak.promobboss.gui.menus.mob;

import com.wattbreak.promobboss.common.IMobEditable;
import com.wattbreak.promobboss.common.IMobTyped;
import com.wattbreak.promobboss.gui.Menu;
import com.wattbreak.promobboss.utility.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

/**
 * This menu shows whether a Boss or Mob originated from MythicMobs or Custom Mob
 It is used to select and change the *.
 */
public class MobTypeSelectionMenu extends Menu {

    private final IMobEditable entity;
    private final Menu parentMenu;

    public MobTypeSelectionMenu(Player player, IMobEditable entity, Menu parentMenu) {
        super(player);
        this.entity = entity;
        this.parentMenu = parentMenu;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessage("gui.mob-type-selection.title");
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;

        switch (event.getSlot()) {
            case 11:
                if (plugin.isMythicMobsEnabled()) {
                    new MythicMobSelectorMenu(player, entity, this).open();
                } else {
                    player.sendMessage(plugin.getMessage("gui.mob-type-selection.mythicmobs-not-found"));
                    player.closeInventory();
                }
                break;
            case 13:
                toggleMobSource();
                setMenuItems();
                break;
            case 15:
                new VanillaMobEditorMenu(player, entity, this).open();
                break;
            case 18:
                parentMenu.open();
                break;
        }
    }

    private void toggleMobSource() {
        String currentSource = entity.getMobTypeSource();
        if (!plugin.isMythicMobsEnabled()) {
            player.sendMessage(plugin.getMessage("gui.mob-type-selection.mythicmobs-not-found-toggle"));
            return;
        }

        if ("VANILLA_CUSTOM".equalsIgnoreCase(currentSource)) {
            entity.setMobTypeSource("MYTHIC_MOBS");
        } else {
            entity.setMobTypeSource("VANILLA_CUSTOM");
        }

        // TODO: The save method should be called to save the change permanently
        // if (mobTyped instanceof Boss) plugin.getBossManager().saveBoss((Boss) mobTyped);
        // else if (mobTyped instanceof Mob) plugin.getMobManager().saveMob((Mob) mobTyped);
    }

    @Override
    public void setMenuItems() {
        inventory.clear();
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < getSlots(); i++) inventory.setItem(i, filler);
        for (int i = 10; i < 17; i++) {
            if (i != 11 && i != 13 && i != 15) inventory.setItem(i, null);
        }
        inventory.setItem(18, null);

        ItemBuilder mmButton = new ItemBuilder(Material.DRAGON_HEAD)
                .setName(plugin.getMessage("gui.mob-type-selection.edit-mythicmobs-item.name"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.mob-type-selection.edit-mythicmobs-item.lore"))
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        if (!plugin.isMythicMobsEnabled()) {
            mmButton.setMaterial(Material.BARRIER)
                    .setName(plugin.getMessage("gui.mob-type-selection.edit-mythicmobs-item.name-disabled"));
        }
        inventory.setItem(11, mmButton.build());

        ItemBuilder toggleButton;
        if ("MYTHIC_MOBS".equalsIgnoreCase(entity.getMobTypeSource())) {
            toggleButton = new ItemBuilder(Material.BEACON)
                    .setName(plugin.getMessage("gui.mob-type-selection.toggle-item.name-is-mythic"))
                    .setLore(plugin.getLanguageManager().getMessageList("gui.mob-type-selection.toggle-item.lore-is-mythic"))
                    .setGlowing(true);
        } else {
            toggleButton = new ItemBuilder(Material.CRAFTING_TABLE)
                    .setName(plugin.getMessage("gui.mob-type-selection.toggle-item.name-is-vanilla"))
                    .setLore(plugin.getLanguageManager().getMessageList("gui.mob-type-selection.toggle-item.lore-is-vanilla"))
                    .setGlowing(true);
        }
        inventory.setItem(13, toggleButton.build());

        // Right Button: Edit Private Mob
        ItemBuilder vanillaButton = new ItemBuilder(Material.ZOMBIE_HEAD)
                .setName(plugin.getMessage("gui.mob-type-selection.edit-vanilla-item.name"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.mob-type-selection.edit-vanilla-item.lore"));
        inventory.setItem(15, vanillaButton.build());

        // Back button
        inventory.setItem(18, new ItemBuilder(Material.BARRIER)
                .setName(plugin.getMessage("gui.buttons.back"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.buttons.back-lore"))
                .build());
    }
}