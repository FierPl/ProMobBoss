package com.wattbreak.promobboss.gui.menus;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.boss.Boss;
import com.wattbreak.promobboss.gui.PaginatedMenu;
import com.wattbreak.promobboss.utility.ChatHelper;
import com.wattbreak.promobboss.utility.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class BossListMenu extends PaginatedMenu {

    private final List<Boss> bosses;

    public BossListMenu(Player player) {
        super(player);
        this.bosses = plugin.getBossManager().getAllBosses();
    }

    @Override
    public String getMenuName() {
        return plugin.getMessage("gui.boss-list-menu.title");
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        super.handleMenu(event);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (event.getSlot() == 45) {
            player.sendMessage(plugin.getMessage("gui.boss-list-menu.add-new-boss-command"));
            player.closeInventory();
            return;
        }

        if (event.getSlot() == 49 && clickedItem.getType() == Material.BARRIER) {
            new MainMenu(player).open();
            return;
        }

        String bossConfigName = ItemBuilder.getNbt(clickedItem, "boss-config-name");
        if (bossConfigName != null) {
            Boss boss = plugin.getBossManager().getBoss(bossConfigName);
            if (boss == null) {
                player.sendMessage(plugin.getMessage("gui.boss-list-menu.boss-not-found-refresh"));
                open();
                return;
            }

            if (event.getClick() == ClickType.LEFT) {
                new BossEditorMenu(player, boss).open();
            } else if (event.getClick() == ClickType.RIGHT) {
                new DeleteConfirmMenu(player, boss).open();
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
                .setName(plugin.getMessage("gui.boss-list-menu.add-new-boss"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.boss-list-menu.add-new-boss-lore"))
                .build());

        if (bosses.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER).setName(plugin.getMessage("gui.boss-list-menu.no-bosses-configured")).build()); // DİL DOSYASINDAN
            return;
        }

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = page * maxItemsPerPage + i;
            if (index >= bosses.size()) break;

            Boss currentBoss = bosses.get(index);

            String status = currentBoss.isEnabled() ? plugin.getMessage("general.status-active") : plugin.getMessage("general.status-disabled"); // DİL DOSYASINDAN
            String location = "Not set";
            if (currentBoss.getSpawnLocation() != null && currentBoss.getSpawnLocation().getWorld() != null) {
                location = plugin.getMessage("gui.boss-list-menu.location-format",
                        "%world%", currentBoss.getSpawnLocation().getWorld().getName(),
                        "%x%", String.format("%.0f", currentBoss.getSpawnLocation().getX()),
                        "%y%", String.format("%.0f", currentBoss.getSpawnLocation().getY()),
                        "%z%", String.format("%.0f", currentBoss.getSpawnLocation().getZ())
                );
            }

            // Boss için item'ı oluştur ve NBT ekle
            ItemStack bossItem = new ItemBuilder(Material.DRAGON_EGG)
                    .setName(currentBoss.getDisplayName())
                    .setLore(
                            plugin.getMessage("gui.boss-list-menu.item.lore-status", "%status%", status),
                            plugin.getMessage("gui.boss-list-menu.item.lore-location", "%location%", location),
                            " ",
                            plugin.getMessage("gui.boss-list-menu.item.lore-edit"),
                            plugin.getMessage("gui.boss-list-menu.item.lore-delete")
                    )
                    .setNbt("boss-config-name", currentBoss.getConfigName())
                    .build();

            inventory.addItem(bossItem);
        }
    }
}