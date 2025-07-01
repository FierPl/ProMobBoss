package com.wattbreak.promobboss.gui.menus;

import com.wattbreak.promobboss.gui.Menu;
import com.wattbreak.promobboss.gui.menus.mobs.MobListMenu;
import com.wattbreak.promobboss.utility.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class MainMenu extends Menu {

    public MainMenu(Player player) {
        super(player);
    }

    @Override
    public String getMenuName() {
        return plugin.getMessage("gui.main-menu.title");
    }

    @Override
    public int getSlots() {
        return 27; // 3 sıra
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        switch (event.getSlot()) {
            case 11:
                new BossListMenu(player).open();
                break;
            case 15:
                new MobListMenu(player).open();
                break;
        }
    }

    @Override
    public void setMenuItems() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < getSlots(); i++) {
            inventory.setItem(i, filler);
        }

        ItemBuilder bossButton = new ItemBuilder(Material.NETHERITE_SWORD)
                .setName(plugin.getMessage("gui.main-menu.boss-button.name"))
                .setLore(plugin.getMessage("gui.main-menu.boss-button.lore").split("\\|")) // Lore'u | ile ayırarak satırlara böl
                .addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

        ItemBuilder mobButton = new ItemBuilder(Material.ZOMBIE_HEAD)
                .setName(plugin.getMessage("gui.main-menu.mobs-button.name"))
                .setLore(plugin.getMessage("gui.main-menu.mobs-button.lore").split("\\|"));

        inventory.setItem(11, bossButton.build());
        inventory.setItem(15, mobButton.build());
    }
}