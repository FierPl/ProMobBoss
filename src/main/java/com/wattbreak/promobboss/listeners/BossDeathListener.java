package com.wattbreak.promobboss.listeners;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.boss.Boss;
import com.wattbreak.promobboss.boss.BossManager;
import com.wattbreak.promobboss.mob.Mob;
import com.wattbreak.promobboss.mob.MobManager;
import com.wattbreak.promobboss.reward.Reward;
import com.wattbreak.promobboss.utility.ChatHelper;
import com.wattbreak.promobboss.utility.NBTUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BossDeathListener implements Listener {

    private final ProMobBoss plugin;
    private final BossManager bossManager;
    private final MobManager mobManager;
    private final Map<UUID, Map<UUID, Double>> damageTrackers = new HashMap<>();

    public BossDeathListener(ProMobBoss plugin) {
        this.plugin = plugin;
        this.bossManager = plugin.getBossManager();
        this.mobManager = plugin.getMobManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBossDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        if (!bossManager.isActiveBoss(event.getEntity())) return;

        Player player = (Player) event.getDamager();
        UUID bossUUID = event.getEntity().getUniqueId();
        double damage = event.getFinalDamage();

        damageTrackers.computeIfAbsent(bossUUID, k -> new HashMap<>())
                .merge(player.getUniqueId(), damage, Double::sum);
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();

        if (plugin.getBossManager().isActiveBoss(deadEntity)) {
            UUID bossUUID = deadEntity.getUniqueId();
            Boss boss = plugin.getBossManager().getBossFromEntity(deadEntity);
            Player killer = deadEntity.getKiller();

            if (killer != null) {
                plugin.getDataSource().incrementBossKills(killer.getUniqueId(), killer.getName(), 1);

                int currentKills = plugin.getLeaderboardCache().getCachedPlayerKills(killer.getUniqueId());
                plugin.getLeaderboardCache().updatePlayerKillsInCache(killer.getUniqueId(), currentKills + 1);
            }

            distributeRewards(boss, killer, damageTrackers.get(bossUUID));

            event.getDrops().clear();
            event.setDroppedExp(0);

            bossManager.unregisterActiveBoss(deadEntity);
            damageTrackers.remove(bossUUID);

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%boss_name%", boss.getConfigName());
            placeholders.put("%boss_displayname%", ChatHelper.stripColor(boss.getDisplayName()));
            placeholders.put("%killer_name%", killer != null ? killer.getName() : "Bilinmiyor");
            if (plugin.getDiscordWebhookHandler() != null) {
                plugin.getDiscordWebhookHandler().sendMessage("boss-kill", placeholders);
            }

            return;
        }

        String mobConfigName = NBTUtil.getString(deadEntity, "promb-mob-id");
        if (mobConfigName != null) {
            Mob mob = mobManager.getMob(mobConfigName);
            if (mob != null) {
                plugin.getLogger().info("Custom mob '" + mob.getConfigName() + "' has been killed.");

                Player killer = deadEntity.getKiller();
                distributeRewards(mob, killer, null);

                plugin.getMobSpawner().unregisterActiveMob(mob, deadEntity);

                event.getDrops().clear();
                event.setDroppedExp(0);
            }
        }
    }

    /**
     * Distributes the Boss or Mob's rewards according to the set goals.
     * @param entityConfig Boss or Mob object (assumed to implement the IRewardable interface).
     * @param killer The player who kills (if any).
     * @param damageMap Map of damage dealers (only used for bosses).
     */
    private void distributeRewards(Object entityConfig, Player killer, Map<UUID, Double> damageMap) {
        List<Reward> rewards;
        if (entityConfig instanceof Boss) {
            rewards = ((Boss) entityConfig).getRewards();
        } else if (entityConfig instanceof Mob) {
            rewards = ((Mob) entityConfig).getRewards();
        } else {
            return;
        }

        if (rewards == null || rewards.isEmpty()) return;

        Player topDamager = (damageMap != null) ? getTopDamager(damageMap) : null;
        List<Player> allDamagers = (damageMap != null) ? getAllDamagers(damageMap) : new ArrayList<>();

        for (Reward reward : rewards) {
            if (ThreadLocalRandom.current().nextDouble(0, 100) > reward.getChance()) {
                continue;
            }

            Player targetPlayer = null;
            switch (reward.getTarget()) {
                case KILLER:
                    targetPlayer = killer;
                    break;
                case TOP_DAMAGE:
                    targetPlayer = topDamager;
                    break;
                case RANDOM_DAMAGER:
                    if (!allDamagers.isEmpty()) {
                        targetPlayer = allDamagers.get(ThreadLocalRandom.current().nextInt(allDamagers.size()));
                    }
                    break;
            }
            if (targetPlayer != null && targetPlayer.isOnline()) {
                giveReward(targetPlayer, reward);
            }
        }
    }

    /**
     * Gives a reward (item or command) to the specified player.
     * @param player Player to receive the reward.
     * @param reward The reward object to give.
     */
    private void giveReward(Player player, Reward reward) {
        if (reward.getType() == Reward.RewardType.ITEM) {
            if (reward.getItem() == null) {
                plugin.getLogger().warning("Reward item is null for a reward given to " + player.getName());
                return;
            }
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(reward.getItem().clone());
            if (!leftover.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover.get(0));
                player.sendMessage(plugin.getMessage("general.inventory-full-drop"));
            }
        } else {
            String command = (reward.getCommand() != null) ? reward.getCommand().replace("%player%", player.getName()) : "";
            if (!command.isEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } else {
                plugin.getLogger().warning("Reward command is null or empty for a reward given to " + player.getName());
            }
        }
    }

    /**
     * Analyzes the damage map to find the player with the highest damage.
     * @param damageMap A <PlayerUUID, TotalDamage> map based on the UUID of the boss.
     * @return The online Player object that hit the most damage, or null if no one is available.
     */
    private Player getTopDamager(Map<UUID, Double> damageMap) {
        if (damageMap == null || damageMap.isEmpty()) {
            return null;
        }

        Optional<Map.Entry<UUID, Double>> topEntry = damageMap.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue());

        if (topEntry.isPresent()) {
            UUID topDamagerUUID = topEntry.get().getKey();
            Player player = Bukkit.getPlayer(topDamagerUUID);
            if (player != null && player.isOnline()) {
                return player;
            }
        }
        return null;
    }

    /**
     * Returns a list of all players on the damage map.
     * This is used to award a prize to a “random damage dealer”.
     * @param damageMap A <PlayerUUID, TotalDamage> map based on the UUID of the boss.
     * @return A list of all online players who hit damage.
     */
    private List<Player> getAllDamagers(Map<UUID, Double> damageMap) {
        List<Player> damagers = new ArrayList<>();
        if (damageMap == null || damageMap.isEmpty()) {
            return damagers;
        }

        for (UUID playerUUID : damageMap.keySet()) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                damagers.add(player);
            }
        }
        return damagers;
    }
}