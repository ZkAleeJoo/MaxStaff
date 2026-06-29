package org.zkaleejoo.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class CommandContextUtil {

    private CommandContextUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Player requirePlayer(CommandSender sender, MainConfigManager configManager) {
        if (!(sender instanceof Player)) {
            sendPrefixed(sender, configManager.getPrefix() + configManager.getMsgConsole());
            return null;
        }
        return (Player) sender;
    }

    public static boolean requirePermission(CommandSender sender, String permission, MainConfigManager configManager) {
        if (hasPermissionOrAdmin(sender, permission)) {
            return true;
        }

        sendPrefixed(sender, configManager.getPrefix() + configManager.getNoPermission());
        return false;
    }

    public static boolean hasPermissionOrAdmin(CommandSender sender, String permission) {
        return sender.hasPermission("maxstaff.admin") || sender.hasPermission(permission);
    }

    public static void sendPrefixed(CommandSender sender, String message) {
        sender.sendMessage(MessageUtils.getColoredMessage(message));
    }

    @SuppressWarnings("null")
    public static List<String> filterOnlinePlayerNamesByPrefix(String prefix) {
        if (prefix == null) {
            return Collections.emptyList();
        }

        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
                .collect(Collectors.toList());
    }
}