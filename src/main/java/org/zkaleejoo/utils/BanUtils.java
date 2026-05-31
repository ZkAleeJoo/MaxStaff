package org.zkaleejoo.utils;

import io.papermc.paper.ban.BanListType;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.BanEntry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.ban.IpBanList;
import org.bukkit.ban.ProfileBanList;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.zkaleejoo.managers.ActivePunishmentRecord;

public final class BanUtils {

    private BanUtils() {
    }

    public static void banPlayerName(String targetName, String reason, Date expiry, String source) {
        PlayerProfile profile = playerProfile(targetName);
        if (profile == null) {
            return;
        }

        profileBanList().addBan(profile, reason, expiry, source);
    }

    @SuppressWarnings("deprecation")
    public static void pardonPlayerName(String targetName) {
        if (targetName == null || targetName.isBlank()) {
            return;
        }

        ProfileBanList bans = profileBanList();
        removeMatchingPlayerEntries(bans, targetName);
        PlayerProfile profile = playerProfile(targetName);
        if (profile != null) {
            bans.pardon(profile);
        }
        bans.pardon(targetName);
    }

    @SuppressWarnings("deprecation")
    public static void pardonPlayer(UUID uuid, String lastKnownName) {
        if (uuid == null) {
            return;
        }

        ProfileBanList bans = profileBanList();
        removeMatchingPlayerEntries(bans, uuid);

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        PlayerProfile profile = offlinePlayer.getPlayerProfile();
        if (profile == null || profile.getId() == null) {
            profile = Bukkit.createProfile(uuid, lastKnownName);
        }
        bans.pardon(profile);

        if (lastKnownName != null && !lastKnownName.isBlank()) {
            removeMatchingPlayerEntries(bans, lastKnownName);
            bans.pardon(lastKnownName);
        }
    }

    public static void pardonPlayer(UUID uuid) {
        pardonPlayer(uuid, null);
    }

    public static String getPlayerBanReason(String targetName) {
        if (targetName == null || targetName.isBlank()) {
            return null;
        }

        BanEntry<?> entry = findActiveMatchingPlayerEntry(profileBanList(), targetName);
        return entry == null ? null : entry.getReason();
    }

    @SuppressWarnings("deprecation")
    public static boolean isPlayerNameBanned(String targetName) {
        if (targetName == null || targetName.isBlank()) {
            return false;
        }

        ProfileBanList bans = profileBanList();
        PlayerProfile profile = playerProfile(targetName);
        return (profile != null && bans.isBanned(profile))
                || bans.isBanned(targetName)
                || findActiveMatchingPlayerEntry(bans, targetName) != null;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getBannedPlayerNames() {
        List<String> names = new ArrayList<>();
        for (BanEntry<?> entry : (java.util.Set<BanEntry<?>>) (java.util.Set<?>) profileBanList().getEntries()) {
            if (removeIfExpired(entry)) {
                continue;
            }
            String name = getEntryPlayerName(entry);
            if (name != null) {
                names.add(name);
                continue;
            }

            UUID uuid = getEntryPlayerUuid(entry);
            if (uuid != null) {
                names.add(uuid.toString());
            }
        }
        return names;
    }

    @SuppressWarnings("unchecked")
    public static List<ActivePunishmentRecord> getActivePlayerBans() {
        List<ActivePunishmentRecord> records = new ArrayList<>();
        for (BanEntry<?> entry : (java.util.Set<BanEntry<?>>) (java.util.Set<?>) profileBanList().getEntries()) {
            if (removeIfExpired(entry)) {
                continue;
            }

            String name = getEntryPlayerName(entry);
            if (name == null || name.isBlank()) {
                UUID uuid = getEntryPlayerUuid(entry);
                if (uuid == null) {
                    continue;
                }
                name = uuid.toString();
            }

            records.add(new ActivePunishmentRecord(
                    ActivePunishmentRecord.Type.BAN,
                    name,
                    safeText(entry.getSource(), "Unknown"),
                    safeText(entry.getReason(), "No reason"),
                    expirationMillis(entry)));
        }
        return records;
    }

    @SuppressWarnings("unchecked")
    public static List<ActivePunishmentRecord> getActiveIpBans() {
        List<ActivePunishmentRecord> records = new ArrayList<>();
        for (BanEntry<?> entry : (java.util.Set<BanEntry<?>>) (java.util.Set<?>) ipBanList().getEntries()) {
            if (removeIfExpired(entry)) {
                continue;
            }

            String target = getEntryIpTarget(entry);
            if (target == null) {
                continue;
            }

            records.add(new ActivePunishmentRecord(
                    ActivePunishmentRecord.Type.IP_BAN,
                    target,
                    safeText(entry.getSource(), "Unknown"),
                    safeText(entry.getReason(), "No reason"),
                    expirationMillis(entry)));
        }
        return records;
    }

    public static void banIp(String ip, String reason, Date expiry, String source) {
        ipBanList().addBan(inetAddress(ip), reason, expiry, source);
    }

    @SuppressWarnings("deprecation")
    public static void pardonIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }

        InetAddress address = inetAddress(ip);
        IpBanList bans = ipBanList();
        removeMatchingIpEntries(bans, address);
        bans.pardon(address);
        bans.pardon(address.getHostAddress());
        Bukkit.unbanIP(address);
        Bukkit.unbanIP(address.getHostAddress());
    }

    public static String getIpBanReason(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }

        BanEntry<?> entry = findActiveMatchingIpEntry(ipBanList(), inetAddress(ip));
        return entry == null ? null : entry.getReason();
    }

    @SuppressWarnings("deprecation")
    public static boolean isIpBanned(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }

        InetAddress address = inetAddress(ip);
        IpBanList bans = ipBanList();
        return bans.isBanned(address)
                || bans.isBanned(address.getHostAddress())
                || findActiveMatchingIpEntry(bans, address) != null;
    }

    private static ProfileBanList profileBanList() {
        return Bukkit.getBanList(BanListType.PROFILE);
    }

    private static IpBanList ipBanList() {
        return Bukkit.getBanList(BanListType.IP);
    }

    private static PlayerProfile playerProfile(String targetName) {
        if (targetName == null || targetName.isBlank()) {
            return null;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(targetName);
        PlayerProfile profile = player.getPlayerProfile();
        if (profile.getId() == null) {
            Bukkit.getLogger().warning("[MaxStaff] Could not resolve UUID for player ban target: " + targetName);
            return null;
        }
        return profile;
    }

    private static InetAddress inetAddress(String ip) {
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("Invalid IP address: " + ip, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static void removeMatchingPlayerEntries(ProfileBanList bans, String targetName) {
        for (BanEntry<?> entry : new ArrayList<>((java.util.Set<BanEntry<?>>) (java.util.Set<?>) bans.getEntries())) {
            if (matchesPlayerEntry(entry, targetName)) {
                entry.remove();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void removeMatchingPlayerEntries(ProfileBanList bans, UUID uuid) {
        for (BanEntry<?> entry : new ArrayList<>((java.util.Set<BanEntry<?>>) (java.util.Set<?>) bans.getEntries())) {
            if (matchesPlayerEntry(entry, uuid)) {
                entry.remove();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static BanEntry<?> findActiveMatchingPlayerEntry(ProfileBanList bans, String targetName) {
        for (BanEntry<?> entry : (java.util.Set<BanEntry<?>>) (java.util.Set<?>) bans.getEntries()) {
            if (removeIfExpired(entry)) {
                continue;
            }
            if (matchesPlayerEntry(entry, targetName)) {
                return entry;
            }
        }
        return null;
    }

    private static boolean matchesPlayerEntry(BanEntry<?> entry, String targetName) {
        String entryName = getEntryPlayerName(entry);
        return entryName != null && entryName.equalsIgnoreCase(targetName);
    }

    private static boolean matchesPlayerEntry(BanEntry<?> entry, UUID uuid) {
        if (uuid == null) {
            return false;
        }
        UUID entryUuid = getEntryPlayerUuid(entry);
        return uuid.equals(entryUuid);
    }

    private static String getEntryPlayerName(BanEntry<?> entry) {
        Object target = entry.getBanTarget();
        if (target instanceof @SuppressWarnings("deprecation") org.bukkit.profile.PlayerProfile profile && profile.getName() != null) {
            return profile.getName();
        }

        @SuppressWarnings("deprecation")
        String targetText = entry.getTarget();
        return targetText == null || targetText.isBlank() ? null : targetText;
    }

    @SuppressWarnings("deprecation")
    private static UUID getEntryPlayerUuid(BanEntry<?> entry) {
        Object target = entry.getBanTarget();
        if (target instanceof org.bukkit.profile.PlayerProfile profile) {
            return profile.getUniqueId();
        }

        if (target instanceof PlayerProfile profile) {
            return profile.getId();
        }

        return null;
    }

    private static String getEntryIpTarget(BanEntry<?> entry) {
        Object target = entry.getBanTarget();
        if (target instanceof InetAddress address) {
            return address.getHostAddress();
        }

        @SuppressWarnings("deprecation")
        String targetText = normalizeIpText(entry.getTarget());
        return targetText.isBlank() ? null : targetText;
    }

    private static long expirationMillis(BanEntry<?> entry) {
        Date expiration = entry.getExpiration();
        return expiration == null ? -1L : expiration.getTime();
    }

    private static String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @SuppressWarnings("unchecked")
    private static void removeMatchingIpEntries(IpBanList bans, InetAddress address) {
        for (BanEntry<?> entry : new ArrayList<>((java.util.Set<BanEntry<?>>) (java.util.Set<?>) bans.getEntries())) {
            if (matchesIpEntry(entry, address)) {
                entry.remove();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static BanEntry<?> findActiveMatchingIpEntry(IpBanList bans, InetAddress address) {
        for (BanEntry<?> entry : (java.util.Set<BanEntry<?>>) (java.util.Set<?>) bans.getEntries()) {
            if (removeIfExpired(entry)) {
                continue;
            }
            if (matchesIpEntry(entry, address)) {
                return entry;
            }
        }
        return null;
    }

    private static boolean matchesIpEntry(BanEntry<?> entry, InetAddress address) {
        Object target = entry.getBanTarget();
        if (target instanceof InetAddress targetAddress && targetAddress.equals(address)) {
            return true;
        }

        @SuppressWarnings("deprecation")
        String targetText = normalizeIpText(entry.getTarget());
        String addressText = normalizeIpText(address.getHostAddress());
        return targetText.equals(addressText);
    }

    private static String normalizeIpText(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.toLowerCase();
    }

    private static boolean removeIfExpired(BanEntry<?> entry) {
        Date expiration = entry.getExpiration();
        if (expiration == null || expiration.after(new Date())) {
            return false;
        }

        entry.remove();
        return true;
    }
}
