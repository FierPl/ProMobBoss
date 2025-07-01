package com.wattbreak.promobboss.gui.menus.reward;

import com.wattbreak.promobboss.boss.Boss;
import com.wattbreak.promobboss.common.IMobEditable;
import com.wattbreak.promobboss.gui.Menu;
import com.wattbreak.promobboss.mob.Mob;
import com.wattbreak.promobboss.reward.Reward;
import com.wattbreak.promobboss.utility.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class RewardConfigMenu extends Menu {

    private final IMobEditable entity;
    private final Reward reward;
    private final Menu parentMenu;

    private static final int SLOT_REWARD_DISPLAY = 13;
    private static final int SLOT_TARGET = 29;
    private static final int SLOT_CHANCE = 31;
    private static final int SLOT_VALUE = 33;
    private static final int SLOT_DELETE = 49;
    private static final int SLOT_BACK = 48;

    public RewardConfigMenu(Player player, IMobEditable entity, Reward reward, Menu parentMenu) {
        super(player);
        this.entity = entity;
        this.reward = reward;
        this.parentMenu = parentMenu;
    }
    @Override
    public String getMenuName() {
        return plugin.getMessage("gui.reward-config-menu.title");
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        boolean changed = false;

        switch (event.getSlot()) {
            case SLOT_TARGET:
                changeTarget();
                changed = true;
                break;
            case SLOT_CHANCE:
                handleChanceInput();
                return;
            case SLOT_VALUE:
                handleChangeRewardValue();
                return;
            case SLOT_DELETE:
                entity.getRewards().remove(reward);

                if (entity instanceof Boss) {
                    plugin.getBossManager().saveRewardsForBoss((Boss) entity);
                } else if (entity instanceof Mob) {
                    plugin.getMobManager().saveRewardsForMob((Mob) entity);
                }

                player.sendMessage(plugin.getMessage("gui.reward-config-menu.reward-deleted"));
                parentMenu.open();
                return;
            case SLOT_BACK:
                parentMenu.open();
                return;
            default:
                return;
        }

        if (changed) {
            if (entity instanceof Boss) {
                plugin.getBossManager().saveRewardsForBoss((Boss) entity);
            } else if (entity instanceof Mob) {
                plugin.getMobManager().saveRewardsForMob((Mob) entity);
            }
            setMenuItems();
        }
    }

    /**
     * Changes the distribution target of the reward (KILLER, TOP_DAMAGE, RANDOM_DAMAGER).
     */
    private void changeTarget() {
        Reward.RewardTarget[] targets = Reward.RewardTarget.values();
        int currentIndex = reward.getTarget().ordinal();
        int nextIndex = (currentIndex + 1) % targets.length;
        reward.setTarget(targets[nextIndex]);
    }

    /**
     * Receives and records the chance of the prize drop via chat.
     */
    private void handleChanceInput() {
        player.closeInventory();
        String cancelKeyword = plugin.getMessage("general.cancel-keyword");
        player.sendMessage(plugin.getMessage("gui.reward-config-menu.prompt-chance", "%cancel_keyword%", cancelKeyword)); // Prompt mesajı

        plugin.getChatInputHandler().newListener(player, input -> {
            if (input.equalsIgnoreCase(cancelKeyword)) {
                player.sendMessage(plugin.getMessage("gui.boss-editor-menu.prompt-cancelled"));
                new RewardConfigMenu(player, entity, reward, parentMenu).open();
                return;
            }
            try {
                double chance = Double.parseDouble(input);
                if (chance < 0 || chance > 100) {
                    player.sendMessage(plugin.getMessage("gui.reward-config-menu.invalid-chance-range"));
                } else {
                    reward.setChance(chance);
                    if (entity instanceof Boss) {
                        plugin.getBossManager().saveRewardsForBoss((Boss) entity);
                    } else if (entity instanceof Mob) {
                        plugin.getMobManager().saveRewardsForMob((Mob) entity);
                    }
                    player.sendMessage(plugin.getMessage("gui.reward-config-menu.chance-set", "%chance%", String.valueOf(chance)));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessage("gui.vanilla-editor.invalid-number-format"));
            }
            new RewardConfigMenu(player, entity, reward, parentMenu).open();
        });
    }

    /**
     * Sets the value of the reward (Item or Command) via chat or player's hand.
     */
    private void handleChangeRewardValue() {
        player.closeInventory();

        String cancelKeyword = plugin.getMessage("general.cancel-keyword");

        if (reward.getType() == Reward.RewardType.ITEM) {
            player.sendMessage(plugin.getMessage("gui.reward-config-menu.prompt-item", "%cancel_keyword%", cancelKeyword)); // Prompt mesajı

            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (handItem == null || handItem.getType() == Material.AIR) {
                player.sendMessage(plugin.getMessage("gui.reward-config-menu.no-item-in-hand"));
            } else {
                reward.setItem(handItem.clone());
                if (entity instanceof Boss) {
                    plugin.getBossManager().saveRewardsForBoss((Boss) entity);
                } else if (entity instanceof Mob) {
                    plugin.getMobManager().saveRewardsForMob((Mob) entity);
                }
                player.sendMessage(plugin.getMessage("gui.reward-config-menu.item-set"));
            }
            new RewardConfigMenu(player, entity, reward, parentMenu).open();

        } else {
            player.sendMessage(plugin.getMessage("gui.reward-config-menu.prompt-command", "%cancel_keyword%", cancelKeyword)); // Prompt mesajı

            plugin.getChatInputHandler().newListener(player, input -> {
                if (input.equalsIgnoreCase(cancelKeyword)) {
                    player.sendMessage(plugin.getMessage("gui.boss-editor-menu.prompt-cancelled"));
                    new RewardConfigMenu(player, entity, reward, parentMenu).open();
                    return;
                }
                String command = input.startsWith("/") ? input.substring(1) : input;
                reward.setCommand(command);
                if (entity instanceof Boss) {
                    plugin.getBossManager().saveRewardsForBoss((Boss) entity);
                } else if (entity instanceof Mob) {
                    plugin.getMobManager().saveRewardsForMob((Mob) entity);
                }
                player.sendMessage(plugin.getMessage("gui.reward-config-menu.command-set", "%command%", command));
                new RewardConfigMenu(player, entity, reward, parentMenu).open();
            });
        }
    }

    @Override
    public void setMenuItems() {
        inventory.clear();
        ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < getSlots(); i++) {
            inventory.setItem(i, filler);
        }
        for (int i = 1; i < 5; i++) {
            for (int j = 1; j < 8; j++) {
                inventory.setItem(i * 9 + j, null);
            }
        }

        ItemStack displayItem;
        Reward.RewardType currentType = (reward.getType() != null) ? reward.getType() : Reward.RewardType.ITEM;

        if (currentType == Reward.RewardType.ITEM && reward.getItem() != null) {
            displayItem = reward.getItem().clone();
            new ItemBuilder(displayItem)
                    .addLoreLine(plugin.getMessage("gui.reward-config-menu.value.display-lore-item"));
        } else {
            String commandText = (reward.getCommand() != null) ? reward.getCommand() : "COMMAND_NOT_SET";
            displayItem = new ItemBuilder(Material.COMMAND_BLOCK)
                    .setName(plugin.getMessage("gui.reward-config-menu.value.name-command"))
                    .setLore(plugin.getLanguageManager().getMessageList("gui.reward-config-menu.value.display-lore-command", "%command%", commandText))
                    .build();
        }
        inventory.setItem(SLOT_REWARD_DISPLAY, displayItem);

        inventory.setItem(SLOT_TARGET, new ItemBuilder(Material.PLAYER_HEAD)
                .setName(plugin.getMessage("gui.reward-config-menu.target.name"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.reward-config-menu.target.lore", "%target%", reward.getTarget().name()))
                .build());

        inventory.setItem(SLOT_CHANCE, new ItemBuilder(Material.GHAST_TEAR)
                .setName(plugin.getMessage("gui.reward-config-menu.chance.name"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.reward-config-menu.chance.lore", "%chance%", String.format("%.1f", reward.getChance())))
                .build());

        ItemBuilder valueButton = new ItemBuilder(currentType == Reward.RewardType.ITEM ? Material.DIAMOND : Material.COMMAND_BLOCK)
                .setName(plugin.getMessage("gui.reward-config-menu.value.name" + currentType.name().toLowerCase()))
                .setLore(plugin.getLanguageManager().getMessageList("gui.reward-config-menu.value.lore" + currentType.name().toLowerCase()));
        inventory.setItem(SLOT_VALUE, valueButton.build());

        inventory.setItem(SLOT_DELETE, new ItemBuilder(Material.TNT)
                .setName(plugin.getMessage("gui.reward-config-menu.delete.name"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.reward-config-menu.delete.lore"))
                .build());

        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.BARRIER)
                .setName(plugin.getMessage("gui.buttons.back"))
                .setLore(plugin.getLanguageManager().getMessageList("gui.buttons.back-lore"))
                .build());
    }
}