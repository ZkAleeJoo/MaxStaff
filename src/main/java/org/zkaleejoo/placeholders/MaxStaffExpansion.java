package org.zkaleejoo.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
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

        switch (params.toLowerCase()) {
            
            //%maxstaff_in_staff_mode%
            case "in_staff_mode":
                return (online != null && plugin.getStaffManager().isInStaffMode(online)) ? textTrue : textFalse;
            
            //%maxstaff_vanished%  
            case "vanished":
                return (online != null && plugin.getStaffManager().isVanished(online)) ? textTrue : textFalse;
            
            //%maxstaff_frozen%
            case "frozen":
                return (online != null && plugin.getFreezeManager().isFrozen(online)) ? textTrue : textFalse;

            //%maxstaff_is_spy%
            case "is_spy":
                return (online != null && plugin.getStaffManager().isSpying(online)) ? textTrue : textFalse;

            //%maxstaff_warn_count%
            case "warn_count":
                return String.valueOf(plugin.getPunishmentManager().getHistoryCount(player.getName(), "WARN"));

            //%maxstaff_ban_count%
            case "ban_count":
                return String.valueOf(plugin.getPunishmentManager().getHistoryCount(player.getName(), "BAN"));

            //%maxstaff_mute_count% 
            case "mute_count":
                return String.valueOf(plugin.getPunishmentManager().getHistoryCount(player.getName(), "MUTE"));

            //%maxstaff_kick_count%
            case "kick_count":
                return String.valueOf(plugin.getPunishmentManager().getHistoryCount(player.getName(), "KICK"));

            //%maxstaff_total_punishments%
            case "total_punishments":
                int total = plugin.getPunishmentManager().getHistoryCount(player.getName(), "BAN") +
                            plugin.getPunishmentManager().getHistoryCount(player.getName(), "MUTE") +
                            plugin.getPunishmentManager().getHistoryCount(player.getName(), "KICK");
                return String.valueOf(total);

            //%maxstaff_playtime%
            case "playtime":
                if (online == null) return "0h 0m";
                long ticks = online.getStatistic(Statistic.PLAY_ONE_MINUTE);
                long hours = ticks / 72000;
                long minutes = (ticks % 72000) / 1200;
                
                return hours + "h " + minutes + "m";

            default:
                return null;
        }
    }
}