package com.wattbreak.promobboss.gui.menus;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.boss.Boss;
import com.wattbreak.promobboss.common.IMobEditable;
import com.wattbreak.promobboss.gui.Menu;
import com.wattbreak.promobboss.gui.menus.mobs.MobListMenu;
import com.wattbreak.promobboss.mob.Mob;
import com.wattbreak.promobboss.utility.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class DeleteConfirmMenu extends Menu {

    private final IMobEditable entityToDelete;

    public DeleteConfirmMenu(Player player, IMobEditable entityToDelete) {
        super(player);
        this.entityToDelete = entityToDelete;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessage("gui.delete-confirm-menu.title", "%name%", entityToDelete.getDisplayName());
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        switch (event.getSlot()) {
            case 11:
                if (entityToDelete instanceof Boss) {
                    plugin.getBossManager().removeBoss((Boss) entityToDelete);
                    player.sendMessage(plugin.getMessage("system.boss-removed", "%name%", entityToDelete.getDisplayName()));
                    new BossListMenu(player).open();
                } else if (entityToDelete instanceof Mob) {
                    plugin.getMobManager().removeMob((Mob) entityToDelete);
                    player.sendMessage(plugin.getMessage("system.mob-removed", "%name%", entityToDelete.getDisplayName()));
                    new MobListMenu(player).open();
                }
                break;
            case 15:
                if (entityToDelete instanceof Boss) {
                    new BossListMenu(player).open();
                } else if (entityToDelete instanceof Mob) {
                    new MobListMenu(player).open();
                }
                break;
        }
    }

    @Override
    public void setMenuItems() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < getSlots(); i++) {
            if (i == 11 || i == 15) continue;
            inventory.setItem(i, filler);
        }

        inventory.setItem(11, new ItemBuilder(Material.LIME_WOOL)
                .setName(plugin.getMessage("gui.buttons.confirm"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.delete-confirm-menu.item.lore", "%name%", entityToDelete.getDisplayName()))
                .build());

        inventory.setItem(15, new ItemBuilder(Material.RED_WOOL)
                .setName(plugin.getMessage("gui.buttons.cancel"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.buttons.cancel-lore"))
                .build());
    }
}