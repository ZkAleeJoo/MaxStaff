package org.zkaleejoo.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class CompatibilityUtil {

    public static ItemStack getPlayerHead() {
        Material type = Material.getMaterial("PLAYER_HEAD");
        if (type == null) {
            type = Material.valueOf("SKULL_ITEM");
            return new ItemStack(type, 1, (short) 3);
        }
        return new ItemStack(type);
    }


    public static String getSound(String oldSound, String newSound) {
        if (ServerVersion.isAtLeast(ServerVersion.V1_19)) {
            return newSound;
        }
        return oldSound;
    }
}