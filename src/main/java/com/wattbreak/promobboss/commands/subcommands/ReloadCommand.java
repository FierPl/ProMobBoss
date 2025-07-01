package com.wattbreak.promobboss.commands.subcommands;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ReloadCommand extends SubCommand {

    private final ProMobBoss plugin = ProMobBoss.getInstance();

    @Override public String getName() { return "reload"; }
    @Override public String getDescriptionKey() { return "commands.reload.description"; }
    @Override public String getSyntaxKey() { return "commands.reload.syntax"; }
    @Override public String getPermission() { return "promb.admin.reload"; }
    @Override public boolean isPlayerOnly() { return false; }

    @Override
    public void perform(CommandSender sender, String[] args) {
        sender.sendMessage(plugin.getMessage("general.reload-start"));

        plugin.getConfigManager().reloadConfigs();
        plugin.getLanguageManager().reloadLanguage();

        plugin.getBossManager().reloadBosses();
        plugin.getMobManager().loadAllMobs();

        if (plugin.getBossScheduler() != null) {
            plugin.getBossScheduler().reloadAllBossSchedules();
        }

        if (plugin.getLeaderboardCache() != null) {
            plugin.getLeaderboardCache().startCacheUpdateTask();
        }

        if (plugin.getUpdateChecker() != null) {
            plugin.getUpdateChecker().checkForUpdate();
        }

        sender.sendMessage(plugin.getMessage("general.reload-success"));
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null; // No argument
    }
}