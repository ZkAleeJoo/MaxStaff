package org.zkaleejoo.utils;

import org.bukkit.entity.Player;

import java.util.regex.Pattern;

public final class IPUtils {

    private static final Pattern IPV4_LITERAL_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$"
    );
    private static final Pattern IPV6_LITERAL_PATTERN = Pattern.compile("^[0-9a-fA-F:]+$");

    private IPUtils() {
    }

    public static String resolvePlayerIp(Player player) {
        if (player == null) {
            return null;
        }
        var socketAddress = player.getAddress();
        if (socketAddress == null || socketAddress.getAddress() == null) {
            return null;
        }
        return normalizeIp(socketAddress.getAddress().getHostAddress());
    }

    public static String normalizeIp(String ip) {
        if (ip == null) {
            return null;
        }
        String normalized = ip.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.toLowerCase();
    }

    public static boolean isLiteralIp(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if (IPV4_LITERAL_PATTERN.matcher(value).matches()) {
            return true;
        }
        return value.contains(":") && IPV6_LITERAL_PATTERN.matcher(value).matches();
    }
}
