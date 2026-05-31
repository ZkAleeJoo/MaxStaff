package org.zkaleejoo.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.CustomConfig;
import org.zkaleejoo.utils.BanUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentManager extends AbstractPunishmentManager {

    private final CustomConfig dataFile;
    private final Map<String, UUID> nameToUuidCache = new ConcurrentHashMap<>();

    public PunishmentManager(MaxStaff plugin) {
        super(plugin);
        this.dataFile = new CustomConfig("data.yml", null, plugin, true);
        this.dataFile.registerConfig();
        loadUuidCache();
        loadMuteCache();
    }

    private void loadUuidCache() {
        ConfigurationSection section = dataFile.getConfig().getConfigurationSection("uuid-cache");
        if (section == null) {
            return;
        }

        for (String cachedName : section.getKeys(false)) {
            String value = section.getString(cachedName);
            if (value == null || value.isBlank()) {
                continue;
            }

            try {
                nameToUuidCache.put(normalizeName(cachedName), UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void loadMuteCache() {
        ConfigurationSection section = dataFile.getConfig().getConfigurationSection("mutes");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long expiry = section.getLong(key + ".expiry");
                muteCache().put(uuid, expiry);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Override
    protected UUID getUniqueId(String targetName) {
        String normalizedName = normalizeName(targetName);
        Player onlineTarget = Bukkit.getPlayer(targetName);
        if (onlineTarget != null) {
            UUID uuid = onlineTarget.getUniqueId();
            updateNameCache(normalizedName, uuid);
            return uuid;
        }

        UUID cached = nameToUuidCache.get(normalizedName);
        if (cached != null) {
            return cached;
        }

        String cachedUUID = dataFile.getConfig().getString("uuid-cache." + normalizedName);
        if (cachedUUID == null || cachedUUID.isBlank()) {
            return null;
        }

        try {
            UUID resolved = UUID.fromString(cachedUUID);
            nameToUuidCache.put(normalizedName, resolved);
            return resolved;
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid cached UUID for player " + targetName + ": " + cachedUUID);
            return null;
        }
    }

    private void updateNameCache(String name, UUID uuid) {
        UUID cached = nameToUuidCache.get(name);
        if (uuid.equals(cached)) {
            return;
        }

        nameToUuidCache.put(name, uuid);
        dataFile.getConfig().set("uuid-cache." + name, uuid.toString());
        dataFile.saveConfig();
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    @Override
    protected String getStorageLogTag() {
        return "MaxStaff Log";
    }

    @Override
    protected void executeAfterHistoryLog(Runnable action) {
        action.run();
    }

    @Override
    protected void saveMute(UUID uuid, String reason, String staffName, long expiry) {
        FileConfiguration data = dataFile.getConfig();
        data.set("mutes." + uuid + ".reason", reason);
        data.set("mutes." + uuid + ".expiry", expiry);
        data.set("mutes." + uuid + ".staff", staffName);
        dataFile.saveConfig();
    }

    @Override
    protected boolean removeMute(UUID uuid) {
        FileConfiguration data = dataFile.getConfig();
        if (!data.contains("mutes." + uuid)) {
            return false;
        }

        data.set("mutes." + uuid, null);
        dataFile.saveConfig();
        return true;
    }

    @Override
    protected boolean removeMuteByName(String targetName) {
        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        if (onlineTarget != null && removeMute(onlineTarget.getUniqueId())) {
            return true;
        }

        FileConfiguration data = dataFile.getConfig();
        String cachedUUID = data.getString("uuid-cache." + targetName.toLowerCase());
        if (cachedUUID != null && !cachedUUID.isBlank()) {
            try {
                if (removeMute(UUID.fromString(cachedUUID))) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        ConfigurationSection cacheSection = data.getConfigurationSection("uuid-cache");
        if (cacheSection == null) {
            return false;
        }

        for (String cachedName : cacheSection.getKeys(false)) {
            if (!cachedName.equalsIgnoreCase(targetName)) {
                continue;
            }

            String uuidString = cacheSection.getString(cachedName);
            if (uuidString == null || uuidString.isBlank()) {
                continue;
            }

            try {
                if (removeMute(UUID.fromString(uuidString))) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        return false;
    }

    @Override
    protected void removeMuteAsync(UUID uuid) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            dataFile.getConfig().set("mutes." + uuid, null);
            dataFile.saveConfig();
        });
    }

    @Override
    public void logHistory(String targetName, String type, String reason, String staff, String duration) {
        UUID uuid = getUniqueId(targetName);
        if (uuid == null) {
            plugin.getLogger().warning("Skipping history log for unknown cached target: " + targetName);
            return;
        }

        FileConfiguration data = dataFile.getConfig();

        int current = data.getInt("history." + uuid + "." + type, 0);
        data.set("history." + uuid + "." + type, current + 1);

        String pathDetails = "history-details." + uuid + "." + type;
        List<String> details = data.getStringList(pathDetails);

        String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        String cleanReason = reason.replace("|", "-");
        String cleanDuration = (duration == null || duration.isEmpty()) ? "N/A" : duration;
        details.add(timestamp + "|" + staff + "|" + cleanReason + "|" + cleanDuration);

        data.set(pathDetails, details);
        dataFile.saveConfig();
    }

    @Override
    public int getHistoryCount(String targetName, String type) {
        UUID uuid = getUniqueId(targetName);
        if (uuid == null) {
            return 0;
        }
        return dataFile.getConfig().getInt("history." + uuid + "." + type.toUpperCase(), 0);
    }

    @Override
    public List<String> getHistoryDetails(String targetName, String type) {
        UUID uuid = getUniqueId(targetName);
        if (uuid == null) {
            return new ArrayList<>();
        }
        return dataFile.getConfig().getStringList("history-details." + uuid + "." + type.toUpperCase());
    }

    @Override
    public List<ActivePunishmentRecord> getActivePunishments() {
        List<ActivePunishmentRecord> records = new ArrayList<>(BanUtils.getActivePlayerBans());
        records.addAll(BanUtils.getActiveIpBans());

        ConfigurationSection section = dataFile.getConfig().getConfigurationSection("mutes");
        if (section == null) {
            return records;
        }

        long now = System.currentTimeMillis();
        boolean changed = false;
        for (String key : section.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            long expiry = section.getLong(key + ".expiry", -1L);
            if (expiry != -1L && expiry <= now) {
                muteCache().remove(uuid);
                dataFile.getConfig().set("mutes." + key, null);
                changed = true;
                continue;
            }

            records.add(new ActivePunishmentRecord(
                    ActivePunishmentRecord.Type.MUTE,
                    resolveCachedName(uuid),
                    section.getString(key + ".staff", "Unknown"),
                    section.getString(key + ".reason", "No reason"),
                    expiry));
        }

        if (changed) {
            dataFile.saveConfig();
        }
        return records;
    }

    private String resolveCachedName(UUID uuid) {
        ConfigurationSection cacheSection = dataFile.getConfig().getConfigurationSection("uuid-cache");
        if (cacheSection != null) {
            String uuidText = uuid.toString();
            for (String cachedName : cacheSection.getKeys(false)) {
                if (uuidText.equalsIgnoreCase(cacheSection.getString(cachedName))) {
                    return cachedName;
                }
            }
        }

        String offlineName = Bukkit.getOfflinePlayer(uuid).getName();
        return offlineName == null || offlineName.isBlank() ? uuid.toString() : offlineName;
    }

    @Override
    public void resetHistory(String targetName, String type) {
        UUID uuid = getUniqueId(targetName);
        if (uuid == null) {
            return;
        }
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

    @Override
    public boolean takeHistory(String targetName, String type, int amount) {
        UUID uuid = getUniqueId(targetName);
        if (uuid == null) {
            return false;
        }
        FileConfiguration data = dataFile.getConfig();
        String path = "history." + uuid + "." + type.toUpperCase();

        int current = data.getInt(path, 0);
        if (current <= 0) {
            return false;
        }

        data.set(path, Math.max(0, current - amount));
        dataFile.saveConfig();
        return true;
    }

    @Override
    protected void persistPlayerIdentity(UUID uuid, String name) {
        updateNameCache(normalizeName(name), uuid);
    }

    @Override
    protected void persistPlayerIP(UUID uuid, String ip) {
        dataFile.getConfig().set("ip-cache." + uuid, ip);
        dataFile.saveConfig();
    }

    @Override
    protected String getCachedIP(UUID uuid) {
        return dataFile.getConfig().getString("ip-cache." + uuid);
    }

    @Override
    public List<UUID> getAllAccountsByIP(String ip) {
        List<UUID> alts = new ArrayList<>();
        ConfigurationSection section = dataFile.getConfig().getConfigurationSection("ip-cache");
        if (section == null) {
            return alts;
        }

        for (String uuidStr : section.getKeys(false)) {
            if (ip.equals(section.getString(uuidStr))) {
                try {
                    alts.add(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return alts;
    }
}
