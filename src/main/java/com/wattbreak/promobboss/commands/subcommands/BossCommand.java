package com.wattbreak.promobboss.commands.subcommands;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.boss.Boss;
import com.wattbreak.promobboss.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BossCommand extends SubCommand {

    private final ProMobBoss plugin = ProMobBoss.getInstance();

    @Override public String getName() { return "boss"; }
    @Override public String getDescriptionKey() { return "commands.boss.description"; }
    @Override public String getSyntaxKey() { return "commands.boss.syntax"; }
    @Override public String getPermission() { return "promb.boss"; }
    @Override public boolean isPlayerOnly() { return false; }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessage("invalid-usage", "%usage%", "/mb boss <add|remove|spawn> <isim>"));
            return;
        }

        String action = args[0].toLowerCase();
        String bossName = args[1];

        switch (action) {
            case "add":
                handleAdd(sender, bossName);
                break;
            case "remove":
                handleRemove(sender, bossName);
                break;
            case "spawn":
                handleSpawn(sender, bossName);
                break;
            default:
                sender.sendMessage(plugin.getMessage("invalid-usage", "%usage%", "/mb boss <add|remove|spawn> <isim>"));
                break;
        }
    }

    private void handleAdd(CommandSender sender, String bossName) {
        if (!sender.hasPermission("promb.boss.add")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return;
        }
        if (plugin.getBossManager().createNewBoss(bossName)) {
            sender.sendMessage(plugin.getMessage("system.boss-created", "%name%", bossName));
        } else {
            sender.sendMessage(plugin.getMessage("system.boss-already-exists", "%name%", bossName));
        }
    }

    private void handleRemove(CommandSender sender, String bossName) {
        if (!sender.hasPermission("promb.boss.remove")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return;
        }
        Boss boss = plugin.getBossManager().getBoss(bossName);
        if (boss == null) {
            sender.sendMessage(plugin.getMessage("system.boss-not-found", "%name%", bossName));
            return;
        }
        if (plugin.getBossManager().removeBoss(boss)) {
            sender.sendMessage(plugin.getMessage("system.boss-removed", "%name%", bossName));
        }
    }

    private void handleSpawn(CommandSender sender, String bossName) {
        if (!sender.hasPermission("promb.boss.spawn")) { /* ... */ return; }

        Boss boss = plugin.getBossManager().getBoss(bossName);
        if (boss == null) {
            sender.sendMessage(plugin.getMessage("system.boss-not-found", "%name%", bossName));
            return;
        }

        if (boss.getSpawnLocation() == null) {
            sender.sendMessage(plugin.getMessage("system.boss-location-not-set", "%boss_name%", boss.getDisplayName()));
            return;
        }

        plugin.getBossScheduler().spawnBoss(boss);

        sender.sendMessage(plugin.getMessage("system.boss-spawn-success", "%boss_name%", boss.getDisplayName()));
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) { // /mb boss <...>
            return Arrays.asList("add", "remove", "spawn").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("spawn"))) { // /mb boss remove/spawn <...>
            return plugin.getBossManager().getAllBosses().stream()
                    .map(Boss::getConfigName)
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}