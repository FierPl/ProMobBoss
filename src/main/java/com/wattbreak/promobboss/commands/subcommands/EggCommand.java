package com.wattbreak.promobboss.commands.subcommands;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.commands.SubCommand;
import com.wattbreak.promobboss.utility.ItemBuilder;
import com.wattbreak.promobboss.utility.NBTUtil;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EggCommand extends SubCommand {

    private final ProMobBoss plugin = ProMobBoss.getInstance();

    @Override public String getName() { return "egg"; }
    @Override public String getDescriptionKey() { return "commands.egg.description"; }
    @Override public String getSyntaxKey() { return "commands.egg.syntax"; }
    @Override public String getPermission() { return "promb.egg"; }
    @Override public boolean isPlayerOnly() { return true; } // Sadece oyuncular yumurta alabilir

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessage("invalid-usage", "%usage%", "/mb egg <boss|mob> <isim>"));
            return;
        }

        Player player = (Player) sender;
        String type = args[0].toLowerCase();
        String name = args[1];

        ItemStack egg = null;
        if ("boss".equals(type)) {
            if (plugin.getBossManager().getBoss(name) != null) {
                egg = new ItemBuilder(Material.DRAGON_EGG)
                        .setName("&c&lBoss Egg: &e" + name)
                        .setLore("&7Using this egg", "&e" + name + " &7you can summon the boss named.")
                        .setNbt("promb-egg-type", "boss")
                        .setNbt("promb-egg-name", name)
                        .setGlowing(true)
                        .build();
            }
        } else if ("mob".equals(type)) {
            if (plugin.getMobManager().getMob(name) != null) {
                egg = new ItemBuilder(Material.ZOMBIE_SPAWN_EGG)
                        .setName("&a&lMob Egg: &e" + name)
                        .setLore("&7Using this egg", "&e" + name + " &7you can summon a special mob called")
                        .setNbt("promb-egg-type", "mob")
                        .setNbt("promb-egg-name", name)
                        .setGlowing(true)
                        .build();
            }
        }

        if (egg != null) {
            player.getInventory().addItem(egg);
            player.sendMessage(plugin.getMessage("system.egg-received", "%name%", name));
        } else {
            player.sendMessage(plugin.getMessage("system.entity-not-found", "%type%", type, "%name%", name));
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) { // /mb egg <...>
            return Arrays.asList("boss", "mob").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3) { // /mb egg boss/mob <...>
            String type = args[1].toLowerCase();
            if ("boss".equals(type)) {
                return plugin.getBossManager().getAllBosses().stream()
                        .map(b -> b.getConfigName())
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            } else if ("mob".equals(type)) {
                return plugin.getMobManager().getAllMobs().stream()
                        .map(m -> m.getConfigName())
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return null;
    }
}