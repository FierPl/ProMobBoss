package com.wattbreak.promobboss.reward;

import java.util.List;

/**
 * Common interface for rewardable entities (Boss, Mob).
 * This interface allows to create reward menus (RewardListMenu, RewardConfigMenu)
 * allows it to work with both Boss and Mob objects.
 */
public interface IRewardable {
    String getConfigName();
    String getDisplayName();
    List<Reward> getRewards();
    void setRewards(List<Reward> rewards);
}