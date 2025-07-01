package com.wattbreak.promobboss.utility;

import com.wattbreak.promobboss.ProMobBoss;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatInputHandler implements Listener {

    private final Map<UUID, Consumer<String>> inputTasks = new HashMap<>();

    public ChatInputHandler(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Saves a player to wait for the next chat input.
     * @param player Player whose input is expected.
     * @param onInput Code to run when input is received.
     */
    public void newListener(Player player, Consumer<String> onInput) {
        inputTasks.put(player.getUniqueId(), onInput);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (inputTasks.containsKey(playerUUID)) {
            event.setCancelled(true);

            Consumer<String> task = inputTasks.get(playerUUID);
            player.getServer().getScheduler().runTask(ProMobBoss.getInstance(), () -> {
                task.accept(event.getMessage());
            });

            inputTasks.remove(playerUUID);
        }
    }
}