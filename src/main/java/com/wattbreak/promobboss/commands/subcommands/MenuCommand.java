package com.wattbreak.promobboss.commands.subcommands;

import com.wattbreak.promobboss.commands.SubCommand;
import com.wattbreak.promobboss.gui.menus.MainMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;

public class MenuCommand extends SubCommand {
    @Override public String getName() { return "menu"; }
    @Override public String getDescriptionKey() { return "commands.menu.description"; }
    @Override public String getSyntaxKey() { return "commands.menu.syntax"; }
    @Override public String getPermission() { return "promb.menu"; }
    @Override public boolean isPlayerOnly() { return true; } // Bu komut sadece oyuncular için

    @Override
    public void perform(CommandSender sender, String[] args) {
        new MainMenu((Player) sender).open();
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}