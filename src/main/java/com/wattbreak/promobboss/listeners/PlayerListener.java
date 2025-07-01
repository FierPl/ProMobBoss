package com.wattbreak.promobboss.listeners;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.data.IDataSource;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * This class listens for events related to the player.
 * Specifically manages the actions to be taken when a player joins or leaves the server.
 */
public class PlayerListener implements Listener {

    private final ProMobBoss plugin;
    private final IDataSource dataSource;

    public PlayerListener(ProMobBoss plugin) {
        this.plugin = plugin;
        this.dataSource = plugin.getDataSource();
    }

    /**
     * This method is triggered when a player joins the server.
     * Our goal is to create a new record for the player if he is not registered in the data source.
     * This avoids possible errors on the database or file system.
     * @param event PlayerJoinEvent event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (dataSource == null) {
            return;
        }

        dataSource.ensurePlayerExists(player);

        if (plugin.getUpdateChecker() != null) {
            plugin.getUpdateChecker().notifyPlayer(player);
        }
    }

    /**
     * This method is triggered when a player leaves the server.
     * If we add a player data cache system in the future,
     * This method can be used to clear the player's data from the cache.
     * This is important to use server memory efficiently.
     * @param event PlayerQuitEvent event.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
    }
}