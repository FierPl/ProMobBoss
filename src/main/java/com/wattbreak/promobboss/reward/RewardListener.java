package com.wattbreak.promobboss.reward;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.boss.Boss;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public class RewardListener implements Listener {

    private final ProMobBoss plugin;
    private final Map<UUID, RewardSession> openRewardInventories = new HashMap<>();

    private static class RewardSession {
        Inventory inventory;
        Boss boss;
        RewardSession(Inventory inventory, Boss boss) {
            this.inventory = inventory;
            this.boss = boss;
        }
    }

    public RewardListener(ProMobBoss plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void registerRewardInventory(UUID playerUUID, Inventory inventory, Boss boss) {
        openRewardInventories.put(playerUUID, new RewardSession(inventory, boss));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (openRewardInventories.containsKey(playerUUID)) {
            RewardSession session = openRewardInventories.get(playerUUID);

            if (event.getInventory().equals(session.inventory)) {
                List<Reward> newRewards = new ArrayList<>();

                for(ItemStack item : event.getInventory().getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        Reward reward = new Reward(Reward.RewardType.ITEM);
                        reward.setItem(item.clone());
                        newRewards.add(reward);
                    }
                }

                session.boss.setRewards(newRewards);


                plugin.getLogger().info("Rewards updated for boss: " + session.boss.getConfigName());

                openRewardInventories.remove(playerUUID);
            }
        }
    }
}