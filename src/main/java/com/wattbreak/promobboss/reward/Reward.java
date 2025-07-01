package com.wattbreak.promobboss.reward;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

@Getter
@Setter
public class Reward {

    public enum RewardType { COMMAND, ITEM }
    public enum RewardTarget { KILLER, TOP_DAMAGE, RANDOM_DAMAGER }

    private RewardType type;
    private RewardTarget target;
    private double chance;

    private String command;
    private ItemStack item;

    public Reward(RewardType type) {
        this.type = (type != null) ? type : RewardType.ITEM;
        this.target = RewardTarget.KILLER;
        this.chance = 100.0;
        this.command = "";
        this.item = null;
    }
}