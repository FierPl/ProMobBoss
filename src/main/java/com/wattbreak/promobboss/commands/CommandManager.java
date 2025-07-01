package com.wattbreak.promobboss.commands;

import com.google.common.collect.Lists;
import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.commands.subcommands.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final ProMobBoss plugin;
    private final List<SubCommand> subCommands = new ArrayList<>();

    public CommandManager(ProMobBoss plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("mb");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }

        // We save all our subcommands here
        subCommands.add(new HelpCommand(this));
        subCommands.add(new ReloadCommand());
        subCommands.add(new MenuCommand());
        subCommands.add(new BossCommand());
        subCommands.add(new MobCommand());
        subCommands.add(new EggCommand());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            // Run the help command if no argument is entered
            getSubCommand("help").perform(sender, args);
            return true;
        }

        SubCommand subCommand = getSubCommand(args[0]);
        if (subCommand == null) {
            sender.sendMessage(plugin.getMessage("unknown-command"));
            return true;
        }

        if (subCommand.isPlayerOnly() && !(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("player-only-command"));
            return true;
        }

        if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, args.length - 1);

        subCommand.perform(sender, newArgs);

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            // Suggest the names of all subcommands for the first argument
            List<String> completions = new ArrayList<>();
            for (SubCommand sub : subCommands) {
                if (sub.getPermission() == null || sender.hasPermission(sub.getPermission())) {
                    if (sub.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(sub.getName());
                    }
                }
            }
            return completions;
        } else if (args.length > 1) {
            SubCommand subCommand = getSubCommand(args[0]);
            if (subCommand != null && (subCommand.getPermission() == null || sender.hasPermission(subCommand.getPermission()))) {
                if (sender instanceof Player) {
                    return subCommand.getSubcommandArguments((Player) sender, args);
                }
            }
        }

        return Lists.newArrayList();
    }

    public SubCommand getSubCommand(String name) {
        for (SubCommand sub : subCommands) {
            if (sub.getName().equalsIgnoreCase(name)) {
                return sub;
            }
        }
        return null;
    }

    public List<SubCommand> getSubCommands() {
        return subCommands;
    }
}