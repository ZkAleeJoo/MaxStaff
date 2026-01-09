package org.zkaleejoo.managers;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.CustomConfig;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.TimeUtils;
import org.bukkit.Sound;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PunishmentManager {

    private final MaxStaff plugin;
    private CustomConfig dataFile;

    public PunishmentManager(MaxStaff plugin) {
        this.plugin = plugin;
        this.dataFile = new CustomConfig("data.yml", null, plugin, true);
        this.dataFile.registerConfig();
    }

    private UUID getUniqueId(String targetName) {
        Player onlineTarget = Bukkit.getPlayer(targetName);
        if (onlineTarget != null) {
            UUID uuid = onlineTarget.getUniqueId();
            updateNameCache(targetName, uuid);
            return uuid;
        }

        String cachedUUID = dataFile.getConfig().getString("uuid-cache." + targetName.toLowerCase());
        if (cachedUUID != null) {
            return UUID.fromString(cachedUUID);
        }

        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        UUID uuid = offlineTarget.getUniqueId();
        updateNameCache(targetName, uuid);
        return uuid;
    }

    private void updateNameCache(String name, UUID uuid) {
        dataFile.getConfig().set("uuid-cache." + name.toLowerCase(), uuid.toString());
        dataFile.saveConfig();
    }


    public void logHistory(String targetName, String type, String reason, String staff, String duration) {
        UUID uuid = getUniqueId(targetName);
        FileConfiguration data = dataFile.getConfig();
        
        int current = data.getInt("history." + uuid + "." + type, 0);
        data.set("history." + uuid + "." + type, current + 1);
        
        String pathDetails = "history-details." + uuid + "." + type;
        List<String> details = data.getStringList(pathDetails);
        
        String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        
        String cleanReason = reason.replace("|", "-");
        String cleanDuration = (duration == null || duration.isEmpty()) ? "N/A" : duration;

        String record = timestamp + "|" + staff + "|" + cleanReason + "|" + cleanDuration;
        details.add(record);
        
        data.set(pathDetails, details);
        dataFile.saveConfig();
    }

    public int getHistoryCount(String targetName, String type) {
        UUID uuid = getUniqueId(targetName);
        return dataFile.getConfig().getInt("history." + uuid + "." + type.toUpperCase(), 0);
    }

    public List<String> getHistoryDetails(String targetName, String type) {
        UUID uuid = getUniqueId(targetName);
        return dataFile.getConfig().getStringList("history-details." + uuid + "." + type.toUpperCase());
    }

    // --- KICK ---
    public void kickPlayer(CommandSender staff, String targetName, String reason) {
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            logHistory(targetName, "KICK", reason, staff.getName(), "Ahora");
            
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
        long duration = TimeUtils.parseDuration(durationStr);
        String timeDisplay = TimeUtils.getDurationString(duration, plugin.getMainConfigManager());
        
        logHistory(targetName, "BAN", reason, staff.getName(), timeDisplay);

        Date expiry = (duration == -1) ? null : new Date(System.currentTimeMillis() + duration);
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
        UUID uuid = getUniqueId(targetName);
        long duration = TimeUtils.parseDuration(durationStr);
        String timeDisplay = TimeUtils.getDurationString(duration, plugin.getMainConfigManager());
        
        logHistory(targetName, "MUTE", reason, staff.getName(), timeDisplay);

        long expiry = (duration == -1) ? -1 : System.currentTimeMillis() + duration;
        FileConfiguration data = dataFile.getConfig();
        data.set("mutes." + uuid + ".reason", reason);
        data.set("mutes." + uuid + ".expiry", expiry);
        data.set("mutes." + uuid + ".staff", staff.getName());
        dataFile.saveConfig();

        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            String muteScreen = plugin.getMainConfigManager().getScreenMute().replace("{staff}", staff.getName());
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
        UUID uuid = getUniqueId(targetName);
        FileConfiguration data = dataFile.getConfig();
        if (data.contains("mutes." + uuid)) {
            data.set("mutes." + uuid, null);
            dataFile.saveConfig();
            
            staff.sendMessage(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgUnmuteSuccess().replace("{target}", targetName)
            ));
            
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) target.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getScreenUnmute()));
        } else {
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgNotMuted()));
        }
    }

    // --- WARN ---
    public void warnPlayer(CommandSender staff, String targetName, String reason) {
        logHistory(targetName, "WARN", reason, staff.getName(), "N/A"); 
        int count = getHistoryCount(targetName, "WARN");
        
        Player target = Bukkit.getPlayer(targetName);
        MainConfigManager config = plugin.getMainConfigManager();

        if (target != null) {
            target.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getMsgWarnReceived().replace("{reason}", reason)));
        }

        String bcMsg = config.getBcWarn()
                .replace("{target}", targetName)
                .replace("{staff}", staff.getName())
                .replace("{count}", String.valueOf(count))
                .replace("{reason}", reason);
        broadcast(bcMsg);

        checkWarnThresholds(targetName, count);
    }

    private void checkWarnThresholds(String targetName, int count) {
        ConfigurationSection thresholds = plugin.getMainConfigManager().getWarnThresholds();
        if (thresholds == null) return;

        String key = String.valueOf(count);
        if (thresholds.contains(key)) {
            String command = thresholds.getString(key);
            if (command != null) {
                String finalCmd = command.replace("{target}", targetName);
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd));
            }
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

    public void resetHistory(String targetName, String type) {
        UUID uuid = getUniqueId(targetName);
        FileConfiguration data = dataFile.getConfig();
        
        if (type.equalsIgnoreCase("all")) {
            data.set("history." + uuid, null);
            data.set("history-details." + uuid, null);
        } else {
            data.set("history." + uuid + "." + type.toUpperCase(), null);
            data.set("history-details." + uuid + "." + type.toUpperCase(), null);
        }
        dataFile.saveConfig();
    }

    public boolean takeHistory(String targetName, String type, int amount) {
        UUID uuid = getUniqueId(targetName);
        FileConfiguration data = dataFile.getConfig();
        String path = "history." + uuid + "." + type.toUpperCase();
        
        int current = data.getInt(path, 0);
        if (current <= 0) return false;

        data.set(path, Math.max(0, current - amount));
        dataFile.saveConfig();
        return true;
    }

    public List<String> getBannedPlayerNames() {
        return Bukkit.getBanList(org.bukkit.BanList.Type.NAME).getBanEntries().stream()
                .map(org.bukkit.BanEntry::getTarget)
                .collect(Collectors.toList());
    }

    public List<String> getMutedPlayerNames() {
        List<String> names = new ArrayList<>();
        FileConfiguration data = dataFile.getConfig();
        if (data.getConfigurationSection("mutes") == null) return names;

        for (String uuidStr : data.getConfigurationSection("mutes").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                if (isMuted(uuid)) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    if (op.getName() != null) names.add(op.getName());
                }
            } catch (IllegalArgumentException ignored) {}
        }
        return names;
    }
}