package com.wattbreak.promobboss.gui.menus;

import com.wattbreak.promobboss.boss.Boss;
import com.wattbreak.promobboss.gui.Menu;
import com.wattbreak.promobboss.gui.menus.BossEditorMenu; // Geri butonu için
import com.wattbreak.promobboss.utility.ChatHelper;
import com.wattbreak.promobboss.utility.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList; // List kullanıldığı için
import java.util.Arrays;
import java.util.List;

public class TimeSelectorMenu extends Menu {

    private final Boss boss;
    private static final List<String> DAYS_OF_WEEK = Arrays.asList(
            "EVERYDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    );

    public TimeSelectorMenu(Player player, Boss boss) {
        super(player);
        this.boss = boss;
    }

    @Override public String getMenuName() { return plugin.getMessage("gui.time-selector-menu.title"); }
    @Override public int getSlots() { return 27; }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        switch (event.getSlot()) {
            case 11:
                if (boss.getScheduleType() == Boss.ScheduleType.SCHEDULED) {
                    changeDay();
                }
                break;
            case 13:
                toggleScheduleType();
                break;
            case 15:
                if (boss.getScheduleType() == Boss.ScheduleType.SCHEDULED) {
                    handleTimeInput();
                } else {
                    handleIntervalInput();
                }
                return;
            case 26:
                new BossEditorMenu(player, boss).open();
                return;
        }
        plugin.getBossManager().saveBoss(boss);
        setMenuItems();
    }

    private void toggleScheduleType() {
        if (boss.getScheduleType() == Boss.ScheduleType.SCHEDULED) {
            boss.setScheduleType(Boss.ScheduleType.RECURRING);
        } else {
            boss.setScheduleType(Boss.ScheduleType.SCHEDULED);
        }
    }

    private void handleIntervalInput() {
        player.closeInventory();
        String cancelKeyword = plugin.getMessage("general.cancel-keyword"); // İptal kelimesini çek
        player.sendMessage(plugin.getMessage("gui.time-selector-menu.prompt-interval", "%cancel_keyword%", cancelKeyword)); // Prompt'u güncelle

        plugin.getChatInputHandler().newListener(player, input -> {
            if (input.equalsIgnoreCase(cancelKeyword)) {
                player.sendMessage(plugin.getMessage("gui.boss-editor-menu.prompt-cancelled"));
                new TimeSelectorMenu(player, boss).open();
                return;
            }
            try {
                int interval = Integer.parseInt(input);
                if (interval > 0) {
                    boss.setRecurringIntervalMinutes(interval);
                    player.sendMessage(plugin.getMessage("gui.time-selector-menu.interval-set", "%minutes%", String.valueOf(interval)));
                } else {
                    player.sendMessage(plugin.getMessage("gui.time-selector-menu.invalid-interval"));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessage("gui.vanilla-editor.invalid-number-format"));
            }
            plugin.getBossManager().saveBoss(boss);
            new TimeSelectorMenu(player, boss).open();
        });
    }

    private void handleTimeInput() {
        player.closeInventory();
        String cancelKeyword = plugin.getMessage("general.cancel-keyword"); // İptal kelimesini çek
        player.sendMessage(plugin.getMessage("gui.time-selector-menu.prompt-time", "%cancel_keyword%", cancelKeyword)); // Prompt'u güncelle

        plugin.getChatInputHandler().newListener(player, input -> {
            if (input.equalsIgnoreCase(cancelKeyword)) { // KULLANIM GÜNCELLENDİ
                player.sendMessage(plugin.getMessage("gui.boss-editor-menu.prompt-cancelled"));
                new TimeSelectorMenu(player, boss).open();
                return;
            }
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                LocalTime.parse(input, formatter);

                boss.setScheduleTime(input);
                plugin.getBossManager().saveBoss(boss);
                player.sendMessage(plugin.getMessage("gui.time-selector-menu.time-set", "%time%", input));

            } catch (DateTimeParseException e) {
                player.sendMessage(plugin.getMessage("gui.time-selector-menu.invalid-time-format"));
            }
            plugin.getBossManager().saveBoss(boss);
            new TimeSelectorMenu(player, boss).open();
        });
    }

    private void changeDay() {
        int currentIndex = DAYS_OF_WEEK.indexOf(boss.getScheduleDay());
        int nextIndex = (currentIndex + 1) % DAYS_OF_WEEK.size();
        boss.setScheduleDay(DAYS_OF_WEEK.get(nextIndex));
    }

    @Override
    public void setMenuItems() {
        inventory.clear();
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build();
        for(int i = 0; i < getSlots(); i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(11, null);
        inventory.setItem(13, null);
        inventory.setItem(15, null);


        boolean isScheduled = boss.getScheduleType() == Boss.ScheduleType.SCHEDULED;

        ItemBuilder daySelector = new ItemBuilder(isScheduled ? Material.BOOK : Material.BOOK)
                .setName(plugin.getMessage("gui.time-selector-menu.item.day.title"))
                .setLore(isScheduled ? generateDayLoreFromLanguageFile() : plugin.getLanguageManager().getMessageList("gui.time-selector-menu.item.disabled-lore"));
        if (!isScheduled) daySelector.setGlowing(true);
        inventory.setItem(11, daySelector.build());

        ItemBuilder typeSelector = new ItemBuilder(isScheduled ? Material.CLOCK : Material.REPEATER)
                .setName(plugin.getMessage(isScheduled ? "gui.time-selector-menu.item.type.name-scheduled" : "gui.time-selector-menu.item.type.name-recurring"))
                .setLore(plugin.getLanguageManager().getMessageList(isScheduled ? "gui.time-selector-menu.item.type.lore-scheduled" : "gui.time-selector-menu.item.type.lore-recurring"));
        inventory.setItem(13, typeSelector.build());

        ItemBuilder timeValueSelector;
        if (isScheduled) {
            timeValueSelector = new ItemBuilder(Material.CLOCK)
                    .setName(plugin.getMessage("gui.time-selector-menu.item.time.title"))
                    .setLore(plugin.getLanguageManager().getMessageList("gui.time-selector-menu.item.time.lore", "%time%", boss.getScheduleTime()));
        } else {
            timeValueSelector = new ItemBuilder(Material.REPEATER)
                    .setName(plugin.getMessage("gui.time-selector-menu.item.interval.title"))
                    .setLore(plugin.getLanguageManager().getMessageList("gui.time-selector-menu.item.interval.lore", "%minutes%", String.valueOf(boss.getRecurringIntervalMinutes())));
        }
        inventory.setItem(15, timeValueSelector.build());

        inventory.setItem(26, new ItemBuilder(Material.BARRIER).setName(plugin.getMessage("gui.buttons.back")).build());
    }

    private List<String> generateDayLoreFromLanguageFile() {
        List<String> lore = plugin.getLanguageManager().getMessageList("gui.time-selector-menu.item.day.lore");
        List<String> finalLore = new ArrayList<>(lore);

        for(String day : DAYS_OF_WEEK) {
            String dayName = plugin.getMessage("days." + day.toLowerCase());
            if (day.equals(boss.getScheduleDay())) {
                finalLore.add(ChatHelper.colorize("&e» &a&l" + dayName));
            } else {
                finalLore.add(ChatHelper.colorize("&7- &f" + dayName));
            }
        }
        return finalLore;
    }
}