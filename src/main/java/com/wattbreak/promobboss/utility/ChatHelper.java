package com.wattbreak.promobboss.utility;

import org.bukkit.ChatColor;
import java.util.List;
import java.util.stream.Collectors;

public class ChatHelper {

    /**
     * Converts color codes starting with ‘&’ in a text to valid color codes.
     * @param message Text to translate.
     * @return Colored text.
     */
    public static String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Translates the color codes of each element in a text list.
     * @param list Text list to translate.
     * @return Colored text list.
     */
    public static List<String> colorize(List<String> list) {
        return list.stream().map(ChatHelper::colorize).collect(Collectors.toList());
    }

    /**
     * --- NEWLY ADDED METHOD ---
     * Removes all color codes (both § and &) from a text.
     * This is useful for sending clean text to Discord or logs.
     * @param message Text to clean.
     * @return Color-coded text.
     */
    public static String stripColor(String message) {
        if (message == null) return "";
        return ChatColor.stripColor(message);
    }
}