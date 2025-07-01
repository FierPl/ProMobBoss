package com.wattbreak.promobboss.gui.menus;

import com.wattbreak.promobboss.boss.Boss;
import com.wattbreak.promobboss.gui.Menu;
import com.wattbreak.promobboss.gui.menus.mob.MobTypeSelectionMenu;
import com.wattbreak.promobboss.gui.menus.reward.RewardListMenu;
import com.wattbreak.promobboss.utility.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class BossEditorMenu extends Menu {

    private final Boss boss;

    private static final int SLOT_INFO_PANEL = 4;
    private static final int SLOT_NAME = 20;
    private static final int SLOT_STATUS = 22;
    private static final int SLOT_LOCATION = 24;
    private static final int SLOT_MOB_TYPE = 29;
    private static final int SLOT_TIME = 31;
    private static final int SLOT_REWARDS = 33;
    private static final int SLOT_BACK = 49;

    public BossEditorMenu(Player player, Boss boss) {
        super(player);
        this.boss = boss;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessage("gui.boss-editor-menu.title", "%boss_name%", boss.getDisplayName());
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        int slot = event.getSlot();

        switch (slot) {
            case SLOT_NAME:
                player.closeInventory();
                player.sendMessage(plugin.getMessage("gui.boss-editor-menu.prompt-name"));
                plugin.getChatInputHandler().newListener(player, newName -> {
                    if (newName.equalsIgnoreCase("iptal")) {
                        player.sendMessage(plugin.getMessage("gui.boss-editor-menu.prompt-cancelled"));
                        new BossEditorMenu(player, boss).open();
                        return;
                    }
                    boss.setDisplayName(newName);
                    plugin.getBossManager().saveBoss(boss);
                    player.sendMessage(plugin.getMessage("gui.boss-editor-menu.name-set", "%name%", newName));
                    new BossEditorMenu(player, boss).open();
                });
                break;
            case SLOT_STATUS:
                boss.setEnabled(!boss.isEnabled());
                plugin.getBossManager().saveBoss(boss);
                setMenuItems();
                break;
            case SLOT_LOCATION:
                player.closeInventory();
                boss.setSpawnLocation(player.getLocation());
                plugin.getBossManager().saveBoss(boss);
                player.sendMessage(plugin.getMessage("gui.boss-editor-menu.location-set"));
                new BossEditorMenu(player, boss).open();
                break;
            case SLOT_MOB_TYPE:
                new MobTypeSelectionMenu(player, boss, this).open();
                return;

            case SLOT_TIME:
                new TimeSelectorMenu(player, boss).open();
                return;

            case SLOT_REWARDS:
                new RewardListMenu(player, boss, this).open();
                return;

            case SLOT_BACK:
                new BossListMenu(player).open();
                return;
            default:
                return;
        }
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < getSlots(); i++) {
            if (i > 9 && i < 45 && (i % 9 != 0 && i % 9 != 8)) continue;
            inventory.setItem(i, filler);
        }

        final String status = boss.isEnabled() ? "&aAktif" : "&cDevre Dışı";
        final String location = (boss.getSpawnLocation() != null)
                ? String.format("&f%s, %.0f, %.0f, %.0f", boss.getSpawnLocation().getWorld().getName(),
                boss.getSpawnLocation().getX(), boss.getSpawnLocation().getY(), boss.getSpawnLocation().getZ())
                : "&cAyarlanmadı";
        final String day = (boss.getScheduleDay() != null) ? plugin.getMessage("days." + boss.getScheduleDay().toLowerCase()) : "&cAyarlanmadı";
        final String time = (boss.getScheduleTime() != null) ? boss.getScheduleTime() : "&cAyarlanmadı";
        final String mobType = (boss.getMythicMobName() != null && !boss.getMythicMobName().isEmpty()) ? boss.getMythicMobName() : "Varsayılan"; // TODO

        ItemBuilder infoPanel = new ItemBuilder(Material.PAPER)
                .setName(plugin.getLanguageManager().getMessage("gui.boss-editor-menu.item.info-panel.title", "%boss_name%", boss.getDisplayName()))
                .setLore(plugin.getLanguageManager().getMessageList(
                        "gui.boss-editor-menu.item.info-panel.lore",
                        "%status%", status,
                        "%location%", location,
                        "%mob_type%", mobType,
                        "%day%", day,
                        "%time%", time));
        inventory.setItem(SLOT_INFO_PANEL, infoPanel.build());

        // --- AKSİYON BUTONLARI ---
        inventory.setItem(SLOT_NAME, new ItemBuilder(Material.NAME_TAG)
                .setName(plugin.getMessage("gui.boss-editor-menu.item.name.title"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.boss-editor-menu.item.name.lore", "%name%", boss.getDisplayName()))
                .build());

        String statusTitle = "gui.boss-editor-menu.item.status." + (boss.isEnabled() ? "title-enabled" : "title-disabled");
        String statusLore = "gui.boss-editor-menu.item.status." + (boss.isEnabled() ? "lore-enabled" : "lore-disabled");
        inventory.setItem(SLOT_STATUS, new ItemBuilder(boss.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(plugin.getMessage(statusTitle))
                .setLore(plugin.getLanguageManager().getMessageList(statusLore))
                .build());

        inventory.setItem(SLOT_LOCATION, new ItemBuilder(Material.COMPASS)
                .setName(plugin.getMessage("gui.boss-editor-menu.item.location.title"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.boss-editor-menu.item.location.lore"))
                .build());

        inventory.setItem(SLOT_MOB_TYPE, new ItemBuilder(Material.PLAYER_HEAD)
                .setName(plugin.getMessage("gui.boss-editor-menu.item.mob-type.title"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.boss-editor-menu.item.mob-type.lore"))
                .build());

        inventory.setItem(SLOT_TIME, new ItemBuilder(Material.CLOCK)
                .setName(plugin.getMessage("gui.boss-editor-menu.item.time.title"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.boss-editor-menu.item.time.lore"))
                .build());

        inventory.setItem(SLOT_REWARDS, new ItemBuilder(Material.CHEST)
                .setName(plugin.getMessage("gui.boss-editor-menu.item.rewards.title"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.boss-editor-menu.item.rewards.lore"))
                .build());

        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.BARRIER)
                .setName(plugin.getMessage("gui.buttons.back"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.buttons.back-lore"))
                .build());
    }
}