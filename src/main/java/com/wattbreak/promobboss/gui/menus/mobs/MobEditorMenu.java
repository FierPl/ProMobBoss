package com.wattbreak.promobboss.gui.menus.mobs;

import com.wattbreak.promobboss.gui.Menu;
import com.wattbreak.promobboss.gui.menus.mob.MobTypeSelectionMenu;
import com.wattbreak.promobboss.gui.menus.reward.RewardListMenu;
import com.wattbreak.promobboss.mob.Mob;
import com.wattbreak.promobboss.utility.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class MobEditorMenu extends Menu {

    private final Mob mob;

    private static final int SLOT_NAME = 20;
    private static final int SLOT_STATUS = 21;
    private static final int SLOT_MOB_TYPE = 22;
    private static final int SLOT_REWARDS = 23;
    private static final int SLOT_SPAWN_WORLD = 24;
    private static final int SLOT_SPAWN_INTERVAL = 30;
    private static final int SLOT_MAX_ALIVE = 32;
    private static final int SLOT_BACK = 49;

    public MobEditorMenu(Player player, Mob mob) {
        super(player);
        this.mob = mob;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessage("gui.mob-editor.title", "%mob_name%", mob.getDisplayName());
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;

        switch (event.getSlot()) {
            case SLOT_NAME:
                handleStringInput("name", "İsim", mob.getDisplayName());
                return;
            case SLOT_STATUS:
                mob.setEnabled(!mob.isEnabled());
                saveAndReload();
                break;
            case SLOT_MOB_TYPE:
                new MobTypeSelectionMenu(player, mob, this).open();
                return;
            case SLOT_REWARDS:
                new RewardListMenu(player, mob, this).open();
                return;
            case SLOT_SPAWN_WORLD:
                new WorldSelectorMenu(player, mob).open();
                return;
            case SLOT_SPAWN_INTERVAL:
                handleNumericInput("interval", "Spawn Sıklığı (Dakika)", 1, Integer.MAX_VALUE);
                return;
            case SLOT_MAX_ALIVE:
                handleNumericInput("max-alive", "Maksimum Sayı", 1, 1000);
                return;
            case SLOT_BACK:
                new MobListMenu(player).open();
                return;
        }
    }

    private void handleStringInput(String settingType, String settingName, String currentValue) {
        player.closeInventory();
        String cancelKeyword = plugin.getMessage("general.cancel-keyword");
        player.sendMessage(plugin.getMessage("gui.mob-editor.prompt-string", "%setting%", settingName, "%cancel_keyword%", cancelKeyword)); // Prompt'u güncelle

        plugin.getChatInputHandler().newListener(player, input -> {
            if (input.equalsIgnoreCase(cancelKeyword)) {
                player.sendMessage(plugin.getMessage("gui.boss-editor-menu.prompt-cancelled"));
                saveAndReload(true);
                return;
            }
        });
    }

    private void handleNumericInput(String settingType, String settingName, int min, int max) {
        player.closeInventory();
        String cancelKeyword = plugin.getMessage("general.cancel-keyword");
        player.sendMessage(plugin.getMessage("gui.mob-editor.prompt-numeric", "%setting%", settingName, "%cancel_keyword%", cancelKeyword)); // Prompt'u güncelle

        plugin.getChatInputHandler().newListener(player, input -> {
            if (input.equalsIgnoreCase(cancelKeyword)) {
                player.sendMessage(plugin.getMessage("gui.boss-editor-menu.prompt-cancelled"));
                saveAndReload(true);
                return;
            }
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    if ("interval".equals(settingType)) mob.setSpawnInterval(value);
                    else if ("max-alive".equals(settingType)) mob.setMaxAlive(value);

                    player.sendMessage(plugin.getMessage("gui.mob-editor.numeric-set", "%setting%", settingName, "%value%", String.valueOf(value)));
                } else {
                    player.sendMessage(plugin.getMessage("gui.mob-editor.invalid-range", "%min%", String.valueOf(min), "%max%", String.valueOf(max)));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessage("gui.vanilla-editor.invalid-number-format"));
            }
            saveAndReload(true);
        });
    }

    private void saveAndReload() { saveAndReload(false); }

    private void saveAndReload(boolean openAfter) {
        plugin.getMobManager().saveMob(mob);

        if (openAfter) {
            new MobEditorMenu(player, mob).open();
        } else {
            setMenuItems();
        }
    }
    @Override
    public void setMenuItems() {
        inventory.clear();
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build();
        for(int i = 0; i < getSlots(); i++) {
            inventory.setItem(i, filler);
        }
        for(int i = 10; i < 44; i++) {
            if (i % 9 != 0 && i % 9 != 8) {
                inventory.setItem(i, null);
            }
        }
        inventory.setItem(49, null);

        inventory.setItem(SLOT_NAME, new ItemBuilder(Material.NAME_TAG)
                .setName(plugin.getMessage("gui.mob-editor.item.name.title"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.mob-editor.item.name.lore", "%name%", mob.getDisplayName()))
                .build());

        boolean isEnabled = mob.isEnabled();
        inventory.setItem(SLOT_STATUS, new ItemBuilder(isEnabled ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(plugin.getMessage(isEnabled ? "gui.mob-editor.item.status.title-enabled" : "gui.mob-editor.item.status.title-disabled"))
                .setLore(plugin.getLanguageManager().getMessageList(isEnabled ? "gui.mob-editor.item.status.lore-enabled" : "gui.mob-editor.item.status.lore-disabled"))
                .build());

        inventory.setItem(SLOT_MOB_TYPE, new ItemBuilder(Material.PLAYER_HEAD)
                .setName(plugin.getMessage("gui.mob-editor.item.mob-type.title"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.mob-editor.item.mob-type.lore", "%source%", mob.getMobTypeSource()))
                .build());

        inventory.setItem(SLOT_REWARDS, new ItemBuilder(Material.CHEST)
                .setName(plugin.getMessage("gui.mob-editor.item.rewards.title"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.mob-editor.item.rewards.lore"))
                .build());

        inventory.setItem(SLOT_SPAWN_WORLD, new ItemBuilder(Material.GRASS_BLOCK)
                .setName(plugin.getMessage("gui.mob-editor.item.spawn-world.title"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.mob-editor.item.spawn-world.lore", "%world%", mob.getSpawnWorld() == null ? "Ayarlanmadı" : mob.getSpawnWorld()))
                .build());

        inventory.setItem(SLOT_SPAWN_INTERVAL, new ItemBuilder(Material.CLOCK)
                .setName(plugin.getMessage("gui.mob-editor.item.interval.title"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.mob-editor.item.interval.lore", "%minutes%", String.valueOf(mob.getSpawnInterval())))
                .build());

        inventory.setItem(SLOT_MAX_ALIVE, new ItemBuilder(Material.IRON_BARS)
                .setName(plugin.getMessage("gui.mob-editor.item.max-alive.title"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.mob-editor.item.max-alive.lore", "%count%", String.valueOf(mob.getMaxAlive())))
                .build());

        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.BARRIER).setName(plugin.getMessage("gui.buttons.back")).build());
    }
}