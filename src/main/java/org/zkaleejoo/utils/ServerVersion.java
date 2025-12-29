package org.zkaleejoo.utils;

import org.bukkit.Bukkit;

public enum ServerVersion {
    v1_8_R1, v1_8_R2, v1_8_R3, v1_9_R1, v1_9_R2, v1_10_R1, v1_11_R1, v1_12_R1, 
    v1_13_R1, v1_13_R2, v1_14_R1, v1_15_R1, v1_16_R1, v1_16_R2, v1_16_R3, 
    v1_17_R1, v1_18_R1, v1_18_R2, v1_19_R1, v1_19_R2, v1_19_R3, v1_20_R1, 
    v1_20_R2, v1_20_R3, v1_20_R4, v1_21_R1, v1_21_R2, v1_21_R3;

    private static ServerVersion currentVersion;

    public static ServerVersion getVersion() {
        if (currentVersion != null) return currentVersion;
        
        String pkg = Bukkit.getServer().getClass().getPackage().getName();
        String versionString = pkg.substring(pkg.lastIndexOf('.') + 1);

        try {
            currentVersion = ServerVersion.valueOf(versionString);
        } catch (IllegalArgumentException e) {
            currentVersion = v1_21_R3;
        }
        return currentVersion;
    }

    public static boolean isAtLeast(ServerVersion version) {
        return getVersion().ordinal() >= version.ordinal();
    }
}