package com.wattbreak.promobboss.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class SubCommand {

    // Name of the subcommand (e.g. “reload”, “menu”)
    public abstract String getName();

    // Path to the description of the command in the language file
    public abstract String getDescriptionKey();

    // Path to the command's usage format in the language file
    public abstract String getSyntaxKey();

    // Authorization required by the command (permission)
    public abstract String getPermission();

    // Is the command only available to the player?
    public abstract boolean isPlayerOnly();

    // Method that executes the main function of the command
    public abstract void perform(CommandSender sender, String[] args);

    // Method that returns tab completion suggestions for this command
    public abstract List<String> getSubcommandArguments(Player player, String[] args);

}