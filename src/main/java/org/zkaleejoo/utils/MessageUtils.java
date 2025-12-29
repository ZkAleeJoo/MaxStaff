package org.zkaleejoo.utils;

import org.bukkit.ChatColor;

public class MessageUtils {
    
    public static String getColoredMessage(String message) {
        if (message == null) {
            return ""; 
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}