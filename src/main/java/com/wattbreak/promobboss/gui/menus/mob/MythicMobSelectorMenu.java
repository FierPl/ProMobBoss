package com.wattbreak.promobboss.gui.menus.mob;

import com.wattbreak.promobboss.common.IMobTyped;
import com.wattbreak.promobboss.gui.Menu;
import com.wattbreak.promobboss.gui.PaginatedMenu;
import com.wattbreak.promobboss.utility.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * This menu lists the IDs of all MythicMobs on the server and allows the user to select one.
 * Can be used in both Boss and Mob edit streams.
 */
public class MythicMobSelectorMenu extends PaginatedMenu {

    private final IMobTyped mobTyped;
    private final Menu parentMenu;
    private final List<String> mobInternalNames;

    public MythicMobSelectorMenu(Player player, IMobTyped mobTyped, Menu parentMenu) {
        super(player);
        this.mobTyped = mobTyped;
        this.parentMenu = parentMenu;
        this.mobInternalNames = plugin.getMythicMobsHandler() != null ?
                plugin.getMythicMobsHandler().getMobInternalNames() : new ArrayList<>();
    }

    @Override
    public String getMenuName() {
        return plugin.getMessage("gui.mythicmob-selector.title");
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        super.handleMenu(event);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (event.getSlot() == 49 && clickedItem.getType() == Material.BARRIER) {
            parentMenu.open();
            return;
        }

        if (clickedItem.getType() != Material.PLAYER_HEAD) {
            return;
        }

        String internalName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        mobTyped.setMythicMobName(internalName);

        // TODO: Call the relevant manager to save the change permanently in the file
        // if (mobTyped instanceof Boss) plugin.getBossManager().saveBoss((Boss) mobTyped);
        // else if (mobTyped instanceof Mob) plugin.getMobManager().saveMob((Mob) mobTyped);

        player.sendMessage(plugin.getMessage("gui.mythicmob-selector.mob-selected", "%mob%", internalName));

        parentMenu.open();
    }

    @Override
    public void setMenuItems() {
        addMenuBorder();
        inventory.setItem(49, new ItemBuilder(Material.BARRIER)
                .setName(plugin.getMessage("gui.buttons.back"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.buttons.back-lore"))
                .build());

        if (mobInternalNames.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                    .setName(plugin.getMessage("gui.mythicmob-selector.no-mobs-found.title"))
                    .setLore(plugin.getLanguageManager().getMessageList("gui.mythicmob-selector.no-mobs-found.lore"))
                    .build());
            return;
        }

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = page * maxItemsPerPage + i;
            if (index >= mobInternalNames.size()) break;

            String currentMobName = mobInternalNames.get(index);

            ItemStack mobItem = new ItemBuilder(Material.PLAYER_HEAD)
                    .setName("&e" + currentMobName)
                    .setLore(plugin.getLanguageManager().getMessageList("gui.mythicmob-selector.lore"))
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
            inventory.addItem(mobItem);
        }
    }
}