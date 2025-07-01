package com.wattbreak.promobboss.listeners;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.boss.Boss;
import com.wattbreak.promobboss.mob.Mob;
import com.wattbreak.promobboss.utility.NBTUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class EggListener implements Listener {

    private final ProMobBoss plugin;

    public EggListener(ProMobBoss plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEggUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack itemInHand = event.getItem();
        if (itemInHand == null || itemInHand.getType() == org.bukkit.Material.AIR) return;

        String eggType = NBTUtil.getString(itemInHand.getItemMeta(), "promb-egg-type");
        if (eggType == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        String entityName = NBTUtil.getString(itemInHand.getItemMeta(), "promb-egg-name");
        if (entityName == null) return;

        Block clickedBlock = event.getClickedBlock();
        Location spawnLocation = clickedBlock.getRelative(event.getBlockFace()).getLocation().add(0.5, 0, 0.5);

        if ("boss".equals(eggType)) {
            Boss boss = plugin.getBossManager().getBoss(entityName);
            if (boss != null) {
                boss.setSpawnLocation(spawnLocation);
                plugin.getBossScheduler().spawnBoss(boss);
                player.sendMessage(plugin.getMessage("system.boss-spawn-success", "%boss_name%", boss.getDisplayName()));
            }
        } else if ("mob".equals(eggType)) {
            Mob mob = plugin.getMobManager().getMob(entityName);
            if (mob != null) {
                plugin.getMobSpawner().spawnSingleMob(mob, spawnLocation);
                player.sendMessage(plugin.getMessage("system.mob-spawn-success", "%mob_name%", mob.getDisplayName()));
            }
        }

        if(player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            itemInHand.setAmount(itemInHand.getAmount() - 1);
        }
    }
}