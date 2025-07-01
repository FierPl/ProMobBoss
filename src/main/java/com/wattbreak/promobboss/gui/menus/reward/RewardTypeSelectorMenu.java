package com.wattbreak.promobboss.gui.menus.reward;

import com.wattbreak.promobboss.boss.Boss;
import com.wattbreak.promobboss.common.IMobEditable;
import com.wattbreak.promobboss.gui.Menu;
import com.wattbreak.promobboss.reward.IRewardable;
import com.wattbreak.promobboss.reward.Reward;
import com.wattbreak.promobboss.utility.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class RewardTypeSelectorMenu extends Menu {

    private final IMobEditable entity;
    private final Menu parentMenu;

    public RewardTypeSelectorMenu(Player player, IMobEditable entity, Menu parentMenu) {
        super(player);
        this.entity = entity;
        this.parentMenu = parentMenu;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessage("gui.reward-type-selector.title");
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        Reward newReward = null;

        switch (event.getSlot()) {
            case 11:
                newReward = new Reward(Reward.RewardType.ITEM);
                newReward.setItem(new ItemStack(Material.STONE));
                break;
            case 15:
                newReward = new Reward(Reward.RewardType.COMMAND);
                newReward.setCommand("say %player% won an award!");
                break;
            case 18:
                parentMenu.open();
                return;
        }

        if (newReward != null) {
            entity.getRewards().add(newReward);
            new RewardConfigMenu(player, entity, newReward, this).open();
        }
    }

    @Override
    public void setMenuItems() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build();
        for(int i = 0; i < getSlots(); i++) {
            if((i > 9 && i < 17 && (i != 11 && i != 15)) || i == 18) {
                inventory.setItem(i, null);
            } else {
                inventory.setItem(i, filler);
            }
        }

        inventory.setItem(11, new ItemBuilder(Material.DIAMOND)
                .setName(plugin.getMessage("gui.reward-type-selector.item.name"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.reward-type-selector.item.lore"))
                .build());

        inventory.setItem(15, new ItemBuilder(Material.COMMAND_BLOCK)
                .setName(plugin.getMessage("gui.reward-type-selector.command.name"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.reward-type-selector.command.lore"))
                .build());

        inventory.setItem(18, new ItemBuilder(Material.BARRIER)
                .setName(plugin.getMessage("gui.buttons.back"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.buttons.back-lore"))
                .build());
    }
}