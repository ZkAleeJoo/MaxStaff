package org.zkaleejoo.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

public class MaxStaffExpansion extends PlaceholderExpansion {

    private final MaxStaff plugin;

    public MaxStaffExpansion(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getAuthor() { return "ZkAleeJoo"; }

    @Override
    public @NotNull String getIdentifier() { return "maxstaff"; }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        
        String textTrue = MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPlaceholderTrue());
        String textFalse = MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPlaceholderFalse());

        Player online = player.isOnline() ? player.getPlayer() : null;

        //%maxstaff_in_staff_mode%
        if (params.equalsIgnoreCase("in_staff_mode")) {
            if (online == null) return textFalse;
            return plugin.getStaffManager().isInStaffMode(online) ? textTrue : textFalse;
        }

        //%maxstaff_vanished%
        if (params.equalsIgnoreCase("vanished")) {
            if (online == null) return textFalse;
            return plugin.getStaffManager().isVanished(online) ? textTrue : textFalse;
        }

        //%maxstaff_frozen%
        if (params.equalsIgnoreCase("frozen")) {
            if (online == null) return textFalse;
            return plugin.getFreezeManager().isFrozen(online) ? textTrue : textFalse;
        }

        //%maxstaff_warn_count%
        if (params.equalsIgnoreCase("warn_count")) {
            return String.valueOf(plugin.getPunishmentManager().getHistoryCount(player.getName(), "WARN"));
        }

        return null;
    }
}