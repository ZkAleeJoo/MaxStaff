package org.zkaleejoo.utils;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {
    
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String getColoredMessage(String message) {
        if (message == null || message.isEmpty()) return "";

        if (ServerVersion.isAtLeast(ServerVersion.v1_16_R1)) {
            Matcher matcher = HEX_PATTERN.matcher(message);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String color = matcher.group(1);
                matcher.appendReplacement(sb, net.md_5.bungee.api.ChatColor.of("#" + color).toString());
            }
            message = matcher.appendTail(sb).toString();
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }
}