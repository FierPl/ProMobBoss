package com.wattbreak.promobboss.gui.menus.mobs;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.gui.PaginatedMenu;
import com.wattbreak.promobboss.gui.menus.DeleteConfirmMenu; // DeleteConfirmMenu importu
import com.wattbreak.promobboss.gui.menus.MainMenu; // MainMenu importu
import com.wattbreak.promobboss.gui.menus.mobs.MobEditorMenu;
import com.wattbreak.promobboss.mob.Mob;
import com.wattbreak.promobboss.utility.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MobListMenu extends PaginatedMenu {

    private final List<Mob> mobs;

    public MobListMenu(Player player) {
        super(player);
        this.mobs = plugin.getMobManager().getAllMobs();
    }

    @Override
    public String getMenuName() {
        return plugin.getMessage("gui.mob-list-menu.title");
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        super.handleMenu(event);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (event.getSlot() == 45) {
            player.sendMessage(plugin.getMessage("gui.mob-list-menu.add-new-mob-command"));
            player.closeInventory();
            return;
        }

        if (event.getSlot() == 49 && clickedItem.getType() == Material.BARRIER) {
            new MainMenu(player).open();
            return;
        }

        String mobConfigName = ItemBuilder.getNbt(clickedItem, "mob-config-name");
        if (mobConfigName != null) {
            Mob mob = plugin.getMobManager().getMob(mobConfigName);
            if (mob == null) {
                player.sendMessage("&cThis mob is no longer available. Menu is being revamped.");
                open();
                return;
            }

            if (event.getClick() == ClickType.LEFT) {
                new MobEditorMenu(player, mob).open();
            } else if (event.getClick() == ClickType.RIGHT) {
                new DeleteConfirmMenu(player, mob).open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        addMenuBorder();

        inventory.setItem(49, new ItemBuilder(Material.BARRIER)
                .setName(plugin.getMessage("gui.buttons.back"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.buttons.back-lore")).build());

        inventory.setItem(45, new ItemBuilder(Material.EMERALD)
                .setName(plugin.getMessage("gui.mob-list-menu.add-new-mob"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.mob-list-menu.add-new-mob-lore"))
                .build());

        if (mobs.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER).setName(plugin.getMessage("gui.mob-list-menu.no-mobs")).build());
            return;
        }

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = page * maxItemsPerPage + i;
            if (index >= mobs.size()) break;

            Mob currentMob = mobs.get(index);

            String world = currentMob.getSpawnWorld() != null ? currentMob.getSpawnWorld() : "Ayarlanmadı";
            String interval = String.valueOf(currentMob.getSpawnInterval());
            String maxAlive = String.valueOf(currentMob.getMaxAlive());

            ItemStack mobItem = new ItemBuilder(Material.ZOMBIE_SPAWN_EGG)
                    .setName(currentMob.getDisplayName())
                    .setLore(
                            plugin.getMessage("gui.mob-list-menu.item.lore-world", "%world%", world),
                            plugin.getMessage("gui.mob-list-menu.item.lore-interval", "%interval%", interval),
                            plugin.getMessage("gui.mob-list-menu.item.lore-max", "%max_alive%", maxAlive),
                            " ",
                            plugin.getMessage("gui.mob-list-menu.item.lore-edit"),
                            plugin.getMessage("gui.mob-list-menu.item.lore-delete")
                    )
                    .setNbt("mob-config-name", currentMob.getConfigName())
                    .build();

            inventory.addItem(mobItem);
        }
    }
}