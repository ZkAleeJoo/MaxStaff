package org.zkaleejoo.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.jetbrains.annotations.NotNull;

public class MaxStaffExpansion extends PlaceholderExpansion {

    private final MaxStaff plugin;

    public MaxStaffExpansion(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getAuthor() {
        return "ZkAleeJoo";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "maxstaff";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; 
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) return "";
        
        Player onlinePlayer = player.getPlayer();

        //%maxstaff_in_staff_mode%
        if (params.equalsIgnoreCase("in_staff_mode")) {
            return plugin.getStaffManager().isInStaffMode(onlinePlayer) ? "Yes" : "No";
        }

        //%maxstaff_vanished%
        if (params.equalsIgnoreCase("vanished")) {
            return plugin.getStaffManager().isVanished(onlinePlayer) ? "Yes" : "No";
        }

        //%maxstaff_frozen%
        if (params.equalsIgnoreCase("frozen")) {
            return plugin.getFreezeManager().isFrozen(onlinePlayer) ? "Yes" : "No";
        }

        //%maxstaff_warn_count%
        if (params.equalsIgnoreCase("warn_count")) {
            return String.valueOf(plugin.getPunishmentManager().getHistoryCount(player.getName(), "WARN"));
        }

        //%maxstaff_total_punishments%
        if (params.equalsIgnoreCase("total_punishments")) {
            int total = plugin.getPunishmentManager().getHistoryCount(player.getName(), "BAN")
                      + plugin.getPunishmentManager().getHistoryCount(player.getName(), "MUTE")
                      + plugin.getPunishmentManager().getHistoryCount(player.getName(), "KICK");
            return String.valueOf(total);
        }

        return null; 
    }
}