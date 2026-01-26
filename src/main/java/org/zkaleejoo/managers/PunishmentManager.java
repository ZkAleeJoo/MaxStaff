package org.zkaleejoo.managers;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.managers.storage.StorageProvider;
import org.zkaleejoo.managers.storage.YAMLStorage;
import org.zkaleejoo.managers.storage.MySQLStorage;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.TimeUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class PunishmentManager {

    private final MaxStaff plugin;
    private StorageProvider storage;
    private final Map<UUID, Long> muteCache = new ConcurrentHashMap<>();

    public PunishmentManager(MaxStaff plugin) {
        this.plugin = plugin;
        setupStorage();
        loadMuteCache();
    }

    private void setupStorage() {
        String type = plugin.getMainConfigManager().getStorageType();
        if (type.equalsIgnoreCase("MYSQL")) {
            this.storage = new MySQLStorage(plugin);
        } else {
            this.storage = new YAMLStorage(plugin);
        }
        this.storage.init();
    }

    private void loadMuteCache() {
        plugin.getLogger().info("Sistema de mutes inicializado.");
    }

    private UUID getUniqueId(String targetName) {
        Player onlineTarget = Bukkit.getPlayer(targetName);
        if (onlineTarget != null) {
            UUID uuid = onlineTarget.getUniqueId();
            storage.saveIP(uuid, onlineTarget.getAddress().getAddress().getHostAddress());
            return uuid;
        }
        return Bukkit.getOfflinePlayer(targetName).getUniqueId();
    }

    // --- GESTIÓN DE HISTORIAL ---

    public void logHistory(String targetName, String type, String reason, String staff, String duration) {
        storage.logHistory(getUniqueId(targetName), targetName, type, reason, staff, duration);
    }

    public int getHistoryCount(String targetName, String type) {
        return storage.getHistoryCount(getUniqueId(targetName), type);
    }

    public List<String> getHistoryDetails(String targetName, String type) {
        return storage.getHistoryDetails(getUniqueId(targetName), type);
    }

    public void resetHistory(String targetName, String type) {
        storage.resetHistory(getUniqueId(targetName), type);
    }

    public boolean takeHistory(String targetName, String type, int amount) {
        return storage.takeHistory(getUniqueId(targetName), type, amount);
    }

    // --- LÓGICA DE SILENCIOS (MUTE) ---

    public void mutePlayer(CommandSender staff, String targetName, String reason, String durationStr) {
        UUID uuid = getUniqueId(targetName);
        long duration = TimeUtils.parseDuration(durationStr);
        String timeDisplay = TimeUtils.getDurationString(duration, plugin.getMainConfigManager());
        long expiry = (duration == -1) ? -1 : System.currentTimeMillis() + duration;
        
        storage.saveMute(uuid, reason, expiry, staff.getName());
        logHistory(targetName, "MUTE", reason, staff.getName(), timeDisplay);
        muteCache.put(uuid, expiry);

        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            target.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getScreenMute().replace("{staff}", staff.getName())));
        }

        broadcast(plugin.getMainConfigManager().getBcMute()
                .replace("{target}", targetName)
                .replace("{staff}", staff.getName())
                .replace("{duration}", timeDisplay)
                .replace("{reason}", reason));

        if (staff instanceof Player) {
            ((Player) staff).playSound(((Player) staff).getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 1.2f);
        }
        plugin.getDiscordManager().sendWebhook("mute", targetName, staff.getName(), reason, timeDisplay, null);
    }

    public void unmutePlayer(CommandSender staff, String targetName) {
        UUID uuid = getUniqueId(targetName);
        muteCache.remove(uuid);
        storage.removeMute(uuid);
        
        staff.sendMessage(MessageUtils.getColoredMessage(
            plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgUnmuteSuccess().replace("{target}", targetName)
        ));
        
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) target.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getScreenUnmute()));
    }

    public boolean isMuted(UUID uuid) {
        if (!muteCache.containsKey(uuid)) return false;
        long expiry = muteCache.get(uuid);
        if (expiry != -1 && System.currentTimeMillis() > expiry) {
            muteCache.remove(uuid);
            storage.removeMute(uuid);
            return false;
        }
        return true;
    }

    // --- LÓGICA DE EXPULSIÓN (KICK) ---

    public void kickPlayer(CommandSender staff, String targetName, String reason) {
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            logHistory(targetName, "KICK", reason, staff.getName(), "Ahora");
            String kickScreen = plugin.getMainConfigManager().getScreenKick()
                    .replace("{staff}", staff.getName()).replace("{reason}", reason);
            target.kickPlayer(MessageUtils.getColoredMessage(kickScreen));
            broadcast(plugin.getMainConfigManager().getBcKick().replace("{target}", target.getName()).replace("{staff}", staff.getName()).replace("{reason}", reason));
        }
        plugin.getDiscordManager().sendWebhook("kick", targetName, staff.getName(), reason, "N/A", null);
    }

    // --- LÓGICA DE BANEO (BAN) ---

    public void banPlayer(CommandSender staff, String targetName, String reason, String durationStr) {
        long duration = TimeUtils.parseDuration(durationStr);
        String timeDisplay = TimeUtils.getDurationString(duration, plugin.getMainConfigManager());
        
        logHistory(targetName, "BAN", reason, staff.getName(), timeDisplay);

        Date expiry = (duration == -1) ? null : new Date(System.currentTimeMillis() + duration);
        String banMsg = MessageUtils.getColoredMessage(plugin.getMainConfigManager().getScreenBan()
                .replace("{staff}", staff.getName())
                .replace("{reason}", reason)
                .replace("{duration}", timeDisplay));
        
        Bukkit.getBanList(BanList.Type.NAME).addBan(targetName, banMsg, expiry, staff.getName());

        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            Bukkit.getScheduler().runTask(plugin, () -> target.kickPlayer(banMsg));
        }

        broadcast(plugin.getMainConfigManager().getBcBan()
                .replace("{target}", targetName)
                .replace("{staff}", staff.getName())
                .replace("{duration}", timeDisplay)
                .replace("{reason}", reason));

        if (staff instanceof Player) {
            ((Player) staff).playSound(((Player) staff).getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.0f);
        }
        plugin.getDiscordManager().sendWebhook("ban", targetName, staff.getName(), reason, timeDisplay, null);
    }

    public void unbanPlayer(CommandSender staff, String targetName) {
        Bukkit.getBanList(BanList.Type.NAME).pardon(targetName);
        staff.sendMessage(MessageUtils.getColoredMessage(
            plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgUnbanSuccess().replace("{target}", targetName)
        ));
    }

    // --- LÓGICA DE ADVERTENCIA (WARN) ---

    public void warnPlayer(CommandSender staff, String targetName, String reason) {
        logHistory(targetName, "WARN", reason, staff.getName(), "N/A");
        int count = getHistoryCount(targetName, "WARN");
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            target.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgWarnReceived().replace("{reason}", reason)));
        }
        broadcast(plugin.getMainConfigManager().getBcWarn().replace("{target}", targetName).replace("{staff}", staff.getName()).replace("{count}", String.valueOf(count)).replace("{reason}", reason));
        checkWarnThresholds(targetName, count);
        plugin.getDiscordManager().sendWebhook("warn", targetName, staff.getName(), reason, null, String.valueOf(count));
    }

    private void checkWarnThresholds(String targetName, int count) {
        ConfigurationSection thresholds = plugin.getMainConfigManager().getWarnThresholds();
        if (thresholds == null) return;
        String command = thresholds.getString(String.valueOf(count));
        if (command != null) {
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{target}", targetName)));
        }
    }

    // --- LÓGICA DE BANEO POR IP ---

    public void banIPPlayer(CommandSender staff, String target, String reason, String durationStr) {
        String ip = target.contains(".") ? target : storage.getIP(getUniqueId(target));
        if (ip == null) {
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgNoIPFound()));
            return;
        }
        long duration = TimeUtils.parseDuration(durationStr);
        Date expiry = (duration == -1) ? null : new Date(System.currentTimeMillis() + duration);

        Bukkit.getBanList(BanList.Type.IP).addBan(ip, reason, expiry, staff.getName());
        logHistory(target, "BAN-IP", reason, staff.getName(), TimeUtils.getDurationString(duration, plugin.getMainConfigManager()));
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getAddress().getAddress().getHostAddress().equals(ip)) {
                online.kickPlayer(MessageUtils.getColoredMessage(reason));
            }
        }
    }

    public void unbanIPPlayer(CommandSender staff, String target) {
        String ip = target.contains(".") ? target : storage.getIP(getUniqueId(target));
        if (ip == null) {
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgInvalidIP().replace("{target}", target)));
            return;
        }
        Bukkit.unbanIP(ip);
        staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgUnbanIPSuccess().replace("{ip}", ip)));
    }

    // --- UTILIDADES ---

    private void broadcast(String msg) {
        if (plugin.getMainConfigManager().isBroadcastEnabled()) {
            MessageUtils.broadcastToPlayersOnly(msg);
        }
    }

    public String getPlayerIP(String targetName) {
        return storage.getIP(getUniqueId(targetName));
    }

    public List<UUID> getAllAccountsByIP(String ip) {
        return storage.getAltsByIP(ip);
    }

    public List<String> getBannedPlayerNames() {
        return Bukkit.getBanList(BanList.Type.NAME).getBanEntries().stream()
                .map(entry -> entry.getTarget())
                .collect(Collectors.toList());
    }

    public void savePlayerIP(UUID uuid, String ip) {
        storage.saveIP(uuid, ip);
    }
}