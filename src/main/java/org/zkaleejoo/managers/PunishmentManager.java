package org.zkaleejoo.managers;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.CustomConfig;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.TimeUtils;

import java.util.Date;
import java.util.UUID;

public class PunishmentManager {

    private final MaxStaff plugin;
    private CustomConfig dataFile;

    public PunishmentManager(MaxStaff plugin) {
        this.plugin = plugin;
        this.dataFile = new CustomConfig("data.yml", null, plugin, true);
        this.dataFile.registerConfig();
    }

    // --- KICK ---
    public void kickPlayer(CommandSender staff, String targetName, String reason) {
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            String kickScreen = plugin.getMainConfigManager().getScreenKick()
                    .replace("{staff}", staff.getName())
                    .replace("{reason}", reason);
            target.kickPlayer(MessageUtils.getColoredMessage(kickScreen));
            
            String bcMsg = plugin.getMainConfigManager().getBcKick()
                .replace("{target}", target.getName())
                .replace("{staff}", staff.getName())
                .replace("{reason}", reason);
            broadcast(bcMsg);
        } else {
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getMsgOffline()));
        }
    }

    // --- BAN ---
    public void banPlayer(CommandSender staff, String targetName, String reason, String durationStr) {
        long duration = TimeUtils.parseDuration(durationStr);
        Date expiry = (duration == -1) ? null : new Date(System.currentTimeMillis() + duration);
        String timeDisplay = (duration == -1) ? "Permanent" : TimeUtils.getDurationString(duration);

        String banScreenTemplate = plugin.getMainConfigManager().getScreenBan()
                .replace("{staff}", staff.getName())
                .replace("{reason}", reason)
                .replace("{duration}", timeDisplay);
        
        String finalBanMessage = MessageUtils.getColoredMessage(banScreenTemplate);

        Bukkit.getBanList(BanList.Type.NAME).addBan(targetName, finalBanMessage, expiry, staff.getName());
        
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            target.kickPlayer(finalBanMessage);
        }

        //MENSAJE AL SERVER
        String bcMsg = plugin.getMainConfigManager().getBcBan()
                .replace("{target}", targetName)
                .replace("{staff}", staff.getName())
                .replace("{duration}", timeDisplay)
                .replace("{reason}", reason);
        broadcast(bcMsg);
    }

    public void unbanPlayer(CommandSender staff, String targetName) {
        Bukkit.getBanList(BanList.Type.NAME).pardon(targetName);
        staff.sendMessage(MessageUtils.getColoredMessage(
            plugin.getMainConfigManager().getMsgUnbanSuccess().replace("{target}", targetName)
        ));
    }

    // --- MUTE ---
    public void mutePlayer(CommandSender staff, String targetName, String reason, String durationStr) {
        Player target = Bukkit.getPlayer(targetName);
        UUID uuid = (target != null) ? target.getUniqueId() : Bukkit.getOfflinePlayer(targetName).getUniqueId();
        
        long duration = TimeUtils.parseDuration(durationStr);
        long expiry = (duration == -1) ? -1 : System.currentTimeMillis() + duration;
        String timeDisplay = (duration == -1) ? "Permanent" : TimeUtils.getDurationString(duration);

        FileConfiguration data = dataFile.getConfig();
        data.set("mutes." + uuid + ".reason", reason);
        data.set("mutes." + uuid + ".expiry", expiry);
        data.set("mutes." + uuid + ".staff", staff.getName());
        dataFile.saveConfig();

        if (target != null) {
            String muteScreen = plugin.getMainConfigManager().getScreenMute()
                    .replace("{staff}", staff.getName());
            target.sendMessage(MessageUtils.getColoredMessage(muteScreen));
        }

        String bcMsg = plugin.getMainConfigManager().getBcMute()
                .replace("{target}", targetName)
                .replace("{staff}", staff.getName())
                .replace("{duration}", timeDisplay)
                .replace("{reason}", reason);
        broadcast(bcMsg);
    }

    public void unmutePlayer(CommandSender staff, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        UUID uuid = (target != null) ? target.getUniqueId() : Bukkit.getOfflinePlayer(targetName).getUniqueId();

        FileConfiguration data = dataFile.getConfig();
        if (data.contains("mutes." + uuid)) {
            data.set("mutes." + uuid, null);
            dataFile.saveConfig();
            
            staff.sendMessage(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getMsgUnmuteSuccess().replace("{target}", targetName)
            ));
            
            if (target != null) {
                target.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getScreenUnmute()));
            }
        } else {
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getMsgNotMuted()));
        }
    }

    public boolean isMuted(UUID uuid) {
        FileConfiguration data = dataFile.getConfig();
        if (!data.contains("mutes." + uuid)) return false;

        long expiry = data.getLong("mutes." + uuid + ".expiry");
        if (expiry != -1 && System.currentTimeMillis() > expiry) {
            data.set("mutes." + uuid, null);
            dataFile.saveConfig();
            return false;
        }
        return true;
    }

    private void broadcast(String msg) {
        if (plugin.getMainConfigManager().isBroadcastEnabled()) {
            Bukkit.broadcastMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + msg));
        }
    }
}