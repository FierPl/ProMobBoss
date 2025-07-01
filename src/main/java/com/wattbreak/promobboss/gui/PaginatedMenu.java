package com.wattbreak.promobboss.gui;

import com.wattbreak.promobboss.utility.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public abstract class PaginatedMenu extends Menu {

    protected int page = 0;
    protected int maxItemsPerPage = 28; // 4 satır x 7 sütun
    protected int index = 0;

    public PaginatedMenu(Player player) {
        super(player);
    }

    public void addMenuBorder() {
        ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < 10; i++) {
            if(inventory.getItem(i) == null) inventory.setItem(i, filler);
        }
        inventory.setItem(17, filler);
        inventory.setItem(18, filler);
        for (int i = 26; i < 36; i++) {
            if(inventory.getItem(i) == null) inventory.setItem(i, filler);
        }
        for (int i = 36; i < getSlots(); i++) {
            if(inventory.getItem(i) == null) inventory.setItem(i, filler);
        }

        inventory.setItem(48, new ItemBuilder(Material.STONE_BUTTON)
                .setName(plugin.getMessage("gui.buttons.previous-page"))
                .setLore(plugin.getMessage("gui.buttons.previous-page-lore")).build());

        inventory.setItem(49, new ItemBuilder(Material.BOOK)
                .setName(plugin.getMessage("gui.buttons.page-info", "%page%", String.valueOf(page + 1)))
                .setLore(plugin.getMessage("gui.buttons.page-info-lore")).build());

        inventory.setItem(50, new ItemBuilder(Material.STONE_BUTTON)
                .setName(plugin.getMessage("gui.buttons.next-page"))
                .setLore(plugin.getMessage("gui.buttons.next-page-lore")).build());
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        if (event.getSlot() == 48) {
            if (page > 0) {
                page--;
                super.open();
            }
        } else if (event.getSlot() == 50) {
            page++;
            super.open();
        }
    }
}