package org.zkaleejoo.utils;

import org.bukkit.Bukkit;

public enum ServerVersion {
    V1_19, V1_20, V1_21, V1_22, UNKNOWN;

    public static ServerVersion CURRENT;

    static {
        String version = Bukkit.getServer().getBukkitVersion();
        if (version.contains("1.19")) CURRENT = V1_19;
        else if (version.contains("1.20")) CURRENT = V1_20;
        else if (version.contains("1.21")) CURRENT = V1_21;
        else CURRENT = UNKNOWN;
    }

    public static boolean isAtLeast(ServerVersion version) {
        return CURRENT.ordinal() >= version.ordinal();
    }
}