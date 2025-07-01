package com.wattbreak.promobboss.commands.subcommands;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.commands.SubCommand;
import com.wattbreak.promobboss.mob.Mob;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MobCommand extends SubCommand {

    private final ProMobBoss plugin = ProMobBoss.getInstance();

    @Override public String getName() { return "mob"; }
    @Override public String getDescriptionKey() { return "commands.mob.description"; }
    @Override public String getSyntaxKey() { return "commands.mob.syntax"; }
    @Override public String getPermission() { return "promb.mob"; }
    @Override public boolean isPlayerOnly() { return false; }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessage("invalid-usage", "%usage%", "/mb mob <add|remove|spawn> <isim>"));
            return;
        }

        String action = args[0].toLowerCase();
        String mobName = args[1];

        switch (action) {
            case "add":
                handleAdd(sender, mobName);
                break;
            case "remove":
                handleRemove(sender, mobName);
                break;
            case "spawn":
                handleSpawn(sender, mobName);
                break;
            default:
                sender.sendMessage(plugin.getMessage("invalid-usage", "%usage%", "/mb mob <add|remove|spawn> <isim>"));
                break;
        }
    }

    private void handleAdd(CommandSender sender, String mobName) {
        if (!sender.hasPermission("promb.mob.add")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return;
        }
        if (plugin.getMobManager().createNewMob(mobName)) {
            sender.sendMessage(plugin.getMessage("system.mob-created", "%name%", mobName));
        } else {
            sender.sendMessage(plugin.getMessage("system.mob-already-exists", "%name%", mobName));
        }
    }

    private void handleRemove(CommandSender sender, String mobName) {



        if (!sender.hasPermission("promb.mob.remove")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return;
        }
        Player player = (Player) sender;
        Mob mob = plugin.getMobManager().getMob(mobName);
        if (mob == null) {
            sender.sendMessage(plugin.getMessage("system.mob-not-found", "%name%", mobName));
            return;
        }
        if (plugin.getMobManager().removeMob(mob)) {
            sender.sendMessage(plugin.getMessage("system.mob-removed", "%name%", mobName));
        }

        // Bu satırı aktive et
        plugin.getMobSpawner().spawnSingleMob(mob, player.getLocation());

        sender.sendMessage(plugin.getMessage("system.mob-spawn-success", "%mob_name%", mob.getDisplayName()));

    }

    private void handleSpawn(CommandSender sender, String mobName) {
        if (!sender.hasPermission("promb.mob.spawn")) { /* ... */ return; }
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("player-only-command"));
            return;
        }
        Player player = (Player) sender;

        Mob mob = plugin.getMobManager().getMob(mobName);
        if (mob == null) {
            sender.sendMessage(plugin.getMessage("system.mob-not-found", "%name%", mobName));
            return;
        }

        plugin.getMobSpawner().spawnSingleMob(mob, player.getLocation());

        sender.sendMessage(plugin.getMessage("system.mob-spawn-success", "%mob_name%", mob.getDisplayName()));
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("add", "remove", "spawn").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("spawn"))) {
            return plugin.getMobManager().getAllMobs().stream()
                    .map(Mob::getConfigName)
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}