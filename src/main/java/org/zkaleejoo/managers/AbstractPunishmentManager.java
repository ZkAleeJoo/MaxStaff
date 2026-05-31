package org.zkaleejoo.managers;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.BanUtils;
import org.zkaleejoo.utils.IPUtils;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractPunishmentManager implements IPunishmentManager {

    protected final MaxStaff plugin;
    private final Map<UUID, Long> muteCache = new ConcurrentHashMap<>();

    protected AbstractPunishmentManager(MaxStaff plugin) {
        this.plugin = plugin;
    }

    protected MainConfigManager cfg() {
        return plugin.getMainConfigManager();
    }

    protected String c(String message) {
        return MessageUtils.getColoredMessage(message);
    }

    protected String prefixed(String message) {
        return c(cfg().getPrefix() + message);
    }

    protected String getActorName(CommandSender staff) {
        if (staff == null || staff.getName() == null || staff.getName().trim().isEmpty()) {
            return "CONSOLE";
        }
        return staff.getName();
    }

    protected void logStaffPunishment(String action, String actor, String target) {
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&7[" + getStorageLogTag() + "] "
                + actor + " -> " + action + " -> " + target));
        if (plugin.getAntiXrayListener() != null) {
            plugin.getAntiXrayListener().removeSuspectByName(target);
        }
    }

    protected Map<UUID, Long> muteCache() {
        return muteCache;
    }

    protected abstract UUID getUniqueId(String targetName);

    protected abstract void saveMute(UUID uuid, String reason, String staffName, long expiry);

    protected abstract boolean removeMute(UUID uuid);

    protected boolean removeMuteByName(String targetName) {
        return false;
    }

    protected abstract void removeMuteAsync(UUID uuid);

    protected abstract String getCachedIP(UUID uuid);

    protected abstract void persistPlayerIdentity(UUID uuid, String name);

    protected abstract void executeAfterHistoryLog(Runnable action);

    protected abstract String getStorageLogTag();

    @Override
    public void kickPlayer(CommandSender staff, String targetName, String reason) {
        kickPlayerInternal(staff, targetName, reason, false);
    }

    @Override
    public void kickPlayerSilent(CommandSender staff, String targetName, String reason) {
        kickPlayerInternal(staff, targetName, reason, true);
    }

    private void kickPlayerInternal(CommandSender staff, String targetName, String reason, boolean silent) {
        if (isProtectedTarget(staff, targetName)) {
            return;
        }

        String actor = getActorName(staff);
        clearFreezeBeforeDisconnectPunishment(targetName, actor);

        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            logHistory(targetName, "KICK", reason, actor, "Ahora");

            String kickScreen = cfg().getScreenKick()
                    .replace("{staff}", actor)
                    .replace("{reason}", reason);
            target.kick(MessageUtils.toComponent(kickScreen));

            logStaffPunishment("KICK", actor, targetName);

            if (!silent) {
                String bcMsg = cfg().getBcKick()
                        .replace("{target}", target.getName())
                        .replace("{staff}", actor)
                        .replace("{reason}", reason);
                broadcast(bcMsg);
            }
        } else if (!silent) {
            staff.sendMessage(prefixed(cfg().getMsgOffline()));
        }

        if (staff instanceof Player) {
            org.bukkit.Location staffLoc = ((Player) staff).getLocation();
            if (staffLoc != null) {
                ((Player) staff).playSound(staffLoc, Objects.requireNonNull(Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR), 1.0f, 0.5f);
            }
        }

        plugin.getDiscordManager().sendWebhook("kick", targetName, actor, reason, "N/A", null);
    }

    @Override
    public void banPlayer(CommandSender staff, String targetName, String reason, String durationStr) {
        banPlayerInternal(staff, targetName, reason, durationStr, false);
    }

    @Override
    public void banPlayerSilent(CommandSender staff, String targetName, String reason, String durationStr) {
        banPlayerInternal(staff, targetName, reason, durationStr, true);
    }

    private void banPlayerInternal(CommandSender staff, String targetName, String reason, String durationStr,
            boolean silent) {
        if (isProtectedTarget(staff, targetName)) {
            return;
        }

        long duration = TimeUtils.parseDuration(durationStr);
        String timeDisplay = TimeUtils.getDurationString(duration, cfg());

        String actor = getActorName(staff);
        clearFreezeBeforeDisconnectPunishment(targetName, actor);
        logHistory(targetName, "BAN", reason, actor, timeDisplay);

        Date expiry = (duration == -1) ? null : new Date(System.currentTimeMillis() + duration);
        String banScreenTemplate = cfg().getScreenBan()
                .replace("{staff}", actor)
                .replace("{reason}", reason)
                .replace("{duration}", timeDisplay);

        String finalBanMessage = c(banScreenTemplate);
        BanUtils.banPlayerName(targetName, finalBanMessage, expiry, actor);

        logStaffPunishment("BAN", actor, targetName);

        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            target.kick(MessageUtils.toComponent(finalBanMessage));
        }

        if (!silent) {
            String bcMsg = cfg().getBcBan()
                    .replace("{target}", targetName)
                    .replace("{staff}", actor)
                    .replace("{duration}", timeDisplay)
                    .replace("{reason}", reason);
            broadcast(bcMsg);
        }

        if (staff instanceof Player) {
            org.bukkit.Location staffLoc = ((Player) staff).getLocation();
            if (staffLoc != null) {
                ((Player) staff).playSound(staffLoc, Objects.requireNonNull(Sound.ENTITY_LIGHTNING_BOLT_THUNDER), 0.6f, 1.0f);
            }
        }

        plugin.getDiscordManager().sendWebhook("ban", targetName, actor, reason, timeDisplay, null);
    }

    @Override
    public void unbanPlayer(CommandSender staff, String targetName) {
        UUID uuid = tryParseUuid(targetName);
        if (uuid != null) {
            BanUtils.pardonPlayer(uuid);
            staff.sendMessage(prefixed(cfg().getMsgUnbanSuccess().replace("{target}", uuid.toString())));
            return;
        }

        BanUtils.pardonPlayerName(targetName);
        staff.sendMessage(prefixed(cfg().getMsgUnbanSuccess().replace("{target}", targetName)));
    }

    private UUID tryParseUuid(String input) {
        if (input == null) {
            return null;
        }

        String trimmed = input.trim();
        if (trimmed.length() < 32) {
            return null;
        }

        try {
            return UUID.fromString(trimmed);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public void mutePlayer(CommandSender staff, String targetName, String reason, String durationStr) {
        mutePlayerInternal(staff, targetName, reason, durationStr, false);
    }

    @Override
    public void mutePlayerSilent(CommandSender staff, String targetName, String reason, String durationStr) {
        mutePlayerInternal(staff, targetName, reason, durationStr, true);
    }

    private void mutePlayerInternal(CommandSender staff, String targetName, String reason, String durationStr,
            boolean silent) {
        if (isProtectedTarget(staff, targetName)) {
            return;
        }

        long duration = TimeUtils.parseDuration(durationStr);
        String timeDisplay = TimeUtils.getDurationString(duration, cfg());
        String actor = getActorName(staff);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID uuid = getUniqueId(targetName);

            if (uuid == null) {
                if (!silent) {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> staff.sendMessage(prefixed(getPlayerNotFoundInCacheMessage(targetName))));
                }
                return;
            }

            long expiry = (duration == -1) ? -1 : System.currentTimeMillis() + duration;
            muteCache().put(uuid, expiry);
            saveMute(uuid, reason, actor, expiry);

            Bukkit.getScheduler().runTask(plugin, () -> {
                logHistory(targetName, "MUTE", reason, actor, timeDisplay);

                Player target = Bukkit.getPlayer(targetName);
                if (target != null) {
                    target.sendMessage(c(cfg().getScreenMute().replace("{staff}", actor)));
                }

                logStaffPunishment("MUTE", actor, targetName);

                if (!silent) {
                    String bcMsg = cfg().getBcMute().replace("{target}", targetName).replace("{staff}", actor)
                            .replace("{duration}", timeDisplay).replace("{reason}", reason);
                    broadcast(bcMsg);
                }

                if (staff instanceof Player) {
                    org.bukkit.Location staffLoc = ((Player) staff).getLocation();
                    if (staffLoc != null) {
                        ((Player) staff).playSound(staffLoc, Objects.requireNonNull(Sound.BLOCK_ANVIL_LAND), 0.8f, 1.2f);
                    }
                }

                plugin.getDiscordManager().sendWebhook("mute", targetName, actor, reason, timeDisplay, null);
            });
        });
    }

    @Override
    public void unmutePlayer(CommandSender staff, String targetName) {
        UUID uuid = getUniqueId(targetName);
        boolean removed = false;

        if (uuid != null) {
            boolean existedInCache = muteCache.remove(uuid) != null;
            boolean removedFromStorage = removeMute(uuid);
            removed = existedInCache || removedFromStorage;
        }

        if (!removed) {
            removed = removeMuteByName(targetName);
        }

        if (removed) {
            staff.sendMessage(prefixed(cfg().getMsgUnmuteSuccess().replace("{target}", targetName)));

            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                target.sendMessage(c(cfg().getScreenUnmute()));
            }
        } else {
            staff.sendMessage(prefixed(cfg().getMsgNotMuted()));
        }
    }

    @Override
    public boolean isMuted(UUID uuid) {
        if (!muteCache.containsKey(uuid)) {
            return false;
        }

        long expiry = muteCache.get(uuid);
        if (expiry != -1 && System.currentTimeMillis() > expiry) {
            muteCache.remove(uuid);
            removeMuteAsync(uuid);
            return false;
        }

        return true;
    }

    @Override
    public void warnPlayer(CommandSender staff, String targetName, String reason) {
        warnPlayerInternal(staff, targetName, reason, false);
    }

    @Override
    public void warnPlayerSilent(CommandSender staff, String targetName, String reason) {
        warnPlayerInternal(staff, targetName, reason, true);
    }

    private void warnPlayerInternal(CommandSender staff, String targetName, String reason, boolean silent) {
        if (isProtectedTarget(staff, targetName)) {
            return;
        }

        String actor = getActorName(staff);
        logHistory(targetName, "WARN", reason, actor, "N/A");

        executeAfterHistoryLog(() -> {
            int count = getHistoryCount(targetName, "WARN");

            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                target.sendMessage(prefixed(cfg().getMsgWarnReceived().replace("{reason}", reason)));
            }

            logStaffPunishment("WARN", actor, targetName);

            if (!silent) {
                String bcMsg = cfg().getBcWarn()
                        .replace("{target}", targetName)
                        .replace("{staff}", actor)
                        .replace("{count}", String.valueOf(count))
                        .replace("{reason}", reason);
                broadcast(bcMsg);
            }

            checkWarnThresholds(targetName, count);
            plugin.getDiscordManager().sendWebhook("warn", targetName, actor, reason, null, String.valueOf(count));
        });
    }

    @Override
    public List<ActivePunishmentRecord> getActivePunishments() {
        List<ActivePunishmentRecord> records = new ArrayList<>(BanUtils.getActivePlayerBans());
        records.addAll(BanUtils.getActiveIpBans());
        for (String name : getMutedPlayerNames()) {
            records.add(new ActivePunishmentRecord(ActivePunishmentRecord.Type.MUTE, name, "Unknown", "Muted", -1L));
        }
        return records;
    }

    @Override
    public List<String> getBannedPlayerNames() {
        return BanUtils.getBannedPlayerNames();
    }

    @Override
    public List<String> getMutedPlayerNames() {
        List<String> names = new ArrayList<>();
        for (UUID uuid : muteCache.keySet()) {
            if (isMuted(uuid)) {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                if (name != null) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    @Override
    public void savePlayerIP(UUID uuid, String ip) {
        persistPlayerIP(uuid, ip);
    }

    @Override
    public void savePlayerIdentity(UUID uuid, String name) {
        persistPlayerIdentity(uuid, name);
    }

    protected String getPlayerNotFoundInCacheMessage(String targetName) {
        return "&cPlayer not found in cache: &f" + targetName;
    }

    protected abstract void persistPlayerIP(UUID uuid, String ip);

    @Override
    public String getPlayerIP(String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            String resolvedIp = resolveIp(target);
            if (resolvedIp != null) {
                return resolvedIp;
            }
            plugin.getLogger().warning(
                    "Could not resolve IP for online player " + target.getName() + "; falling back to cached lookup.");
        }

        UUID uuid = getUniqueId(targetName);
        if (uuid == null) {
            return null;
        }
        return getCachedIP(uuid);
    }

    @Override
    public void banIPPlayer(CommandSender staff, String target, String reason, String durationStr) {
        banIPPlayerInternal(staff, target, reason, durationStr, false);
    }

    @Override
    public void banIPPlayerSilent(CommandSender staff, String target, String reason, String durationStr) {
        banIPPlayerInternal(staff, target, reason, durationStr, true);
    }

    private void banIPPlayerInternal(CommandSender staff, String target, String reason, String durationStr,
            boolean silent) {
        boolean literalIp = isLiteralIp(target);
        if (!literalIp && isProtectedTarget(staff, target)) {
            return;
        }

        String ip = literalIp ? target : getPlayerIP(target);
        if (ip == null || ip.isEmpty()) {
            plugin.getLogger().warning("/ban-ip could not find a cached IP for target " + target + ".");
            if (!silent) {
                staff.sendMessage(
                        prefixed(cfg().getMsgNoIPFound() + " &7(Target: &f" + target + "&7) &cUsa IP literal."));
            }
            return;
        }

        long duration = TimeUtils.parseDuration(durationStr);
        String timeDisplay = TimeUtils.getDurationString(duration, cfg());
        Date expiry = (duration == -1) ? null : new Date(System.currentTimeMillis() + duration);

        logHistory(target, "BAN-IP", reason, getActorName(staff), timeDisplay);

        String banMessage = c(cfg().getScreenBan()
                .replace("{staff}", getActorName(staff))
                .replace("{reason}", reason)
                .replace("{duration}", timeDisplay));

        BanUtils.banIp(ip, banMessage, expiry, getActorName(staff));

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null) {
                continue;
            }
            String onlineIp = resolveIp(online);
            if (onlineIp == null) {
                plugin.getLogger().warning(
                        "Could not resolve IP while applying IP-ban check for player " + online.getName() + ".");
                continue;
            }
            if (onlineIp.equals(ip)) {
                online.kick(MessageUtils.toComponent(banMessage));
            }
        }

        if (!silent) {
            String bcMsg = cfg().getBcBanIP()
                    .replace("{target}", target)
                    .replace("{staff}", getActorName(staff))
                    .replace("{duration}", timeDisplay);
            broadcast(bcMsg);
        }

        plugin.getDiscordManager().sendWebhook("ban", target + " (IP)", getActorName(staff), reason, timeDisplay, null);
    }

    @Override
    public void unbanIPPlayer(CommandSender staff, String target) {
        String resolvedIp = isLiteralIp(target) ? target : getPlayerIP(target);
        String ip = normalizeIp(resolvedIp);
        if (ip == null || ip.isEmpty()) {
            staff.sendMessage(prefixed(cfg().getMsgInvalidIP().replace("{target}", target) + " &cUsa IP literal."));
            return;
        }

        BanUtils.pardonIp(ip);
        staff.sendMessage(prefixed(cfg().getMsgUnbanIPSuccess().replace("{ip}", ip)));
    }

    protected String resolveIp(Player player) {
        return IPUtils.resolvePlayerIp(player);
    }

    protected String normalizeIp(String ip) {
        return IPUtils.normalizeIp(ip);
    }

    protected boolean isLiteralIp(String value) {
        return IPUtils.isLiteralIp(value);
    }

    protected void clearFreezeBeforeDisconnectPunishment(String targetName, String actor) {
        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        if (onlineTarget == null || plugin.getFreezeManager() == null) {
            return;
        }

        if (plugin.getFreezeManager().isFrozen(onlineTarget)) {
            plugin.getFreezeManager().setFrozen(onlineTarget, false, actor);
        }
    }

    protected boolean isProtectedTarget(CommandSender staff, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            return false;
        }

        if (!target.hasPermission("maxstaff.punish.protected")) {
            return false;
        }

        if (staff.hasPermission("maxstaff.punish.override")) {
            return false;
        }

        staff.sendMessage(prefixed(cfg().getMsgPunishProtected().replace("{target}", target.getName())));
        return true;
    }

    protected void checkWarnThresholds(String targetName, int count) {
        ConfigurationSection thresholds = cfg().getWarnThresholds();
        if (thresholds == null) {
            return;
        }

        String key = String.valueOf(count);
        if (thresholds.contains(key)) {
            String command = thresholds.getString(key);
            if (command != null) {
                String finalCmd = command.replace("{target}", targetName);
                Bukkit.getScheduler().runTask(plugin,
                        () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd));
            }
        }
    }

    protected void broadcast(String msg) {
        if (cfg().isBroadcastEnabled()) {
            MessageUtils.broadcastToPlayersOnly(msg);
        }
    }
}
