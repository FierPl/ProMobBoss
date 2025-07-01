package com.wattbreak.promobboss.gui.menus.mobs;

import com.wattbreak.promobboss.gui.PaginatedMenu;
import com.wattbreak.promobboss.mob.Mob;
import com.wattbreak.promobboss.utility.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class WorldSelectorMenu extends PaginatedMenu {

    private final Mob mob;
    private final List<World> worlds;

    public WorldSelectorMenu(Player player, Mob mob) {
        super(player);
        this.mob = mob;
        this.worlds = new ArrayList<>(Bukkit.getWorlds());
    }

    @Override
    public String getMenuName() {
        return plugin.getMessage("gui.world-selector.title");
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        super.handleMenu(event);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        if (event.getSlot() == 49) {
            new MobEditorMenu(player, mob).open();
            return;
        }

        String worldName = ItemBuilder.getNbt(event.getCurrentItem(), "world-name");
        if (worldName != null) {
            World selectedWorld = Bukkit.getWorld(worldName);
            if (selectedWorld != null) {
                mob.setSpawnWorld(selectedWorld.getName());
                plugin.getMobManager().saveMob(mob);
                player.sendMessage(plugin.getMessage("gui.mob-editor.world-set", "%world%", selectedWorld.getName()));
                new MobEditorMenu(player, mob).open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        addMenuBorder();
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).setName(plugin.getMessage("gui.buttons.back")).build());

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = page * maxItemsPerPage + i;
            if (index >= worlds.size()) break;

            World currentWorld = worlds.get(index);

            Material icon;
            switch(currentWorld.getEnvironment()) {
                case NETHER:
                    icon = Material.NETHERRACK;
                    break;
                case THE_END:
                    icon = Material.END_STONE;
                    break;
                default:
                    icon = Material.GRASS_BLOCK;
                    break;
            }

            ItemBuilder worldItem = new ItemBuilder(icon)
                    .setName("&a" + currentWorld.getName())
                    .setLore(plugin.getLanguageManager().getMessageList("gui.world-selector.lore"))
                    .setNbt("world-name", currentWorld.getName());

            // If this world is already chosen, let it shine.
            if (currentWorld.getName().equals(mob.getSpawnWorld())) {
                worldItem.setGlowing(true);
            }

            inventory.addItem(worldItem.build());
        }
    }
}