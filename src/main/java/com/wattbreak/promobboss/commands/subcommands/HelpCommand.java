package com.wattbreak.promobboss.commands.subcommands;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.commands.CommandManager;
import com.wattbreak.promobboss.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class HelpCommand extends SubCommand {

    private final ProMobBoss plugin = ProMobBoss.getInstance();
    private final CommandManager commandManager;

    public HelpCommand(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @Override public String getName() { return "help"; }
    @Override public String getDescriptionKey() { return "commands.help.description"; }
    @Override public String getSyntaxKey() { return "commands.help.syntax"; }
    @Override public String getPermission() { return null; } // Herkes kullanabilir
    @Override public boolean isPlayerOnly() { return false; }

    @Override
    public void perform(CommandSender sender, String[] args) {
        String header = plugin.getMessage("commands.help.header");
        String format = plugin.getLanguageManager().getRawMessage("commands.help.format", "&3%command% &8- &7%description%");

        sender.sendMessage(header);

        for (SubCommand sub : commandManager.getSubCommands()) {
            if (sub.getPermission() == null || sender.hasPermission(sub.getPermission())) {

                String syntax = plugin.getLanguageManager().getRawMessage(sub.getSyntaxKey(), "Syntax not found");
                String description = plugin.getLanguageManager().getRawMessage(sub.getDescriptionKey(), "Description not found");

                // Now formatting and sending
                String line = format
                        .replace("%command%", syntax)
                        .replace("%description%", description);

                sender.sendMessage(plugin.getMessage("dummy", "%message%", line));
            }
        }
        String footer = plugin.getMessage("commands.help.footer");
        sender.sendMessage(footer);
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if(args.length == 2) {
            return commandManager.getSubCommands().stream()
                    .map(SubCommand::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}