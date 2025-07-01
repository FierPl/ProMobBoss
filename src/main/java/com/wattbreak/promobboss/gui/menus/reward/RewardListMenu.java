package com.wattbreak.promobboss.gui.menus.reward;

import com.wattbreak.promobboss.boss.Boss;
import com.wattbreak.promobboss.common.IMobEditable;
import com.wattbreak.promobboss.gui.Menu;
import com.wattbreak.promobboss.gui.PaginatedMenu;
import com.wattbreak.promobboss.gui.menus.BossEditorMenu;
import com.wattbreak.promobboss.reward.Reward;
import com.wattbreak.promobboss.utility.ChatHelper;
import com.wattbreak.promobboss.utility.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class RewardListMenu extends PaginatedMenu {

    private final IMobEditable entity;
    private final Menu parentMenu;

    public RewardListMenu(Player player, IMobEditable entity, Menu parentMenu) {
        super(player);
        this.entity = entity;
        this.parentMenu = parentMenu;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessage("gui.reward-list-menu.title", "%name%", entity.getDisplayName()); // %boss_name% yerine %name% kullandım, daha genel
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        super.handleMenu(event);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (event.getSlot() == 45) {
            new RewardTypeSelectorMenu(player, entity, this).open();
            return;
        }

        if (event.getSlot() == 49 && clickedItem.getType() == Material.BARRIER) {
            parentMenu.open();
            return;
        }

        String rewardIndexStr = ItemBuilder.getNbt(clickedItem, "reward-index");
        if (rewardIndexStr != null) {
            try {
                int rewardIndex = Integer.parseInt(rewardIndexStr);
                if (rewardIndex >= 0 && rewardIndex < entity.getRewards().size()) {
                    Reward clickedReward = entity.getRewards().get(rewardIndex);
                    new RewardConfigMenu(player, entity, clickedReward, this).open();
                }
            } catch (NumberFormatException ignored) { }
        }
    }

    @Override
    public void setMenuItems() {
        addMenuBorder();

        inventory.setItem(49, new ItemBuilder(Material.BARRIER)
                .setName(plugin.getMessage("gui.buttons.back"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.buttons.back-lore")).build());

        inventory.setItem(45, new ItemBuilder(Material.EMERALD)
                .setName(plugin.getMessage("gui.reward-list-menu.add-new"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.reward-list-menu.add-new-lore"))
                .build());

        List<Reward> rewards = entity.getRewards();
        if (rewards.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER).setName(plugin.getMessage("gui.reward-list-menu.no-rewards")).build());
            return;
        }

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = page * maxItemsPerPage + i;
            if (index >= rewards.size()) break;

            Reward reward = rewards.get(index);
            ItemStack displayItem;

            String nbtValue = String.valueOf(index);

            if (reward.getType() == Reward.RewardType.ITEM && reward.getItem() != null) {
                displayItem = reward.getItem().clone();
                ItemMeta meta = displayItem.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    lore.add(" ");
                    lore.add(ChatHelper.colorize(plugin.getMessage("gui.reward-list-menu.item-lore.prefix")));
                    lore.add(ChatHelper.colorize(plugin.getMessage("gui.reward-list-menu.item-lore.chance", "%chance%", String.format("%.1f", reward.getChance()))));
                    lore.add(ChatHelper.colorize(plugin.getMessage("gui.reward-list-menu.item-lore.target", "%target%", reward.getTarget().name())));
                    lore.add(" ");
                    lore.add(ChatHelper.colorize(plugin.getMessage("gui.reward-list-menu.item-lore.click-to-edit")));
                    meta.setLore(lore);

                    NamespacedKey nbtKey = new NamespacedKey(plugin, "reward-index");
                    meta.getPersistentDataContainer().set(nbtKey, PersistentDataType.STRING, nbtValue);

                    displayItem.setItemMeta(meta);
                }
            } else if (reward.getType() == Reward.RewardType.COMMAND) {
                displayItem = new ItemBuilder(Material.COMMAND_BLOCK)
                        .setName(plugin.getMessage("gui.reward-list-menu.command-name"))
                        .setLore(plugin.getLanguageManager().getMessageList("gui.reward-list-menu.command-lore",
                                "%command%", reward.getCommand(),
                                "%chance%", String.format("%.1f", reward.getChance()),
                                "%target%", reward.getTarget().name()))
                        .setNbt("reward-index", nbtValue)
                        .build();
            } else {
                continue;
            }
            inventory.addItem(displayItem);
        }
    }
}