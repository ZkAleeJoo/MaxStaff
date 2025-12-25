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
import org.bukkit.Sound;
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

    private void logHistory(String targetName, String type) {
        UUID uuid = Bukkit.getOfflinePlayer(targetName).getUniqueId();
        FileConfiguration data = dataFile.getConfig();
        int current = data.getInt("history." + uuid + "." + type, 0);
        data.set("history." + uuid + "." + type, current + 1);
        dataFile.saveConfig();
    }

    public int getHistoryCount(String targetName, String type) {
        UUID uuid = Bukkit.getOfflinePlayer(targetName).getUniqueId();
        return dataFile.getConfig().getInt("history." + uuid + "." + type, 0);
    }

    // --- KICK ---
    public void kickPlayer(CommandSender staff, String targetName, String reason) {
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            logHistory(targetName, "KICK");
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
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgOffline()));
        }

        if (staff instanceof Player) {
        ((Player) staff).playSound(((Player) staff).getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 0.5f);
    }

    }

    // --- BAN ---
    public void banPlayer(CommandSender staff, String targetName, String reason, String durationStr) {
        logHistory(targetName, "BAN");
        long duration = TimeUtils.parseDuration(durationStr);
        Date expiry = (duration == -1) ? null : new Date(System.currentTimeMillis() + duration);
        
        String timeDisplay = TimeUtils.getDurationString(duration, plugin.getMainConfigManager());

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

        String bcMsg = plugin.getMainConfigManager().getBcBan()
                .replace("{target}", targetName)
                .replace("{staff}", staff.getName())
                .replace("{duration}", timeDisplay)
                .replace("{reason}", reason);
        broadcast(bcMsg);

        if (staff instanceof Player) {
        ((Player) staff).playSound(((Player) staff).getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.0f);
    }

    }

    public void unbanPlayer(CommandSender staff, String targetName) {
        Bukkit.getBanList(BanList.Type.NAME).pardon(targetName);
        staff.sendMessage(MessageUtils.getColoredMessage(
            plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgUnbanSuccess().replace("{target}", targetName)
        ));
    }

    // --- MUTE ---
    public void mutePlayer(CommandSender staff, String targetName, String reason, String durationStr) {
        Player target = Bukkit.getPlayer(targetName);
        UUID uuid = (target != null) ? target.getUniqueId() : Bukkit.getOfflinePlayer(targetName).getUniqueId();
        
        logHistory(targetName, "MUTE");
        long duration = TimeUtils.parseDuration(durationStr);
        long expiry = (duration == -1) ? -1 : System.currentTimeMillis() + duration;
        
        String timeDisplay = TimeUtils.getDurationString(duration, plugin.getMainConfigManager());

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

        if (staff instanceof Player) {
        ((Player) staff).playSound(((Player) staff).getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 1.2f);
    }

    }

    public void unmutePlayer(CommandSender staff, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        UUID uuid = (target != null) ? target.getUniqueId() : Bukkit.getOfflinePlayer(targetName).getUniqueId();

        FileConfiguration data = dataFile.getConfig();
        if (data.contains("mutes." + uuid)) {
            data.set("mutes." + uuid, null);
            dataFile.saveConfig();
            
            staff.sendMessage(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgUnmuteSuccess().replace("{target}", targetName)
            ));
            
            if (target != null) {
                target.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getScreenUnmute()));
            }
        } else {
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgNotMuted()));
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