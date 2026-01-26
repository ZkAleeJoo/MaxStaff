package org.zkaleejoo.managers.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.CustomConfig;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class YAMLStorage implements StorageProvider {

    private final MaxStaff plugin;
    private CustomConfig dataFile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public YAMLStorage(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        this.dataFile = new CustomConfig("data.yml", null, plugin, true);
        this.dataFile.registerConfig();
    }

    @Override
    public void logHistory(UUID uuid, String name, String type, String reason, String staff, String duration) {
        FileConfiguration data = dataFile.getConfig();
        
        int current = data.getInt("history." + uuid + "." + type.toUpperCase(), 0);
        data.set("history." + uuid + "." + type.toUpperCase(), current + 1);
        
        String pathDetails = "history-details." + uuid + "." + type.toUpperCase();
        List<String> details = data.getStringList(pathDetails);
        
        String timestamp = dateFormat.format(new Date());
        String cleanReason = reason.replace("|", "-");
        String record = timestamp + "|" + staff + "|" + cleanReason + "|" + duration;
        
        details.add(record);
        data.set(pathDetails, details);
        dataFile.saveConfig();
    }

    @Override
    public int getHistoryCount(UUID uuid, String type) {
        return dataFile.getConfig().getInt("history." + uuid + "." + type.toUpperCase(), 0);
    }

    @Override
    public List<String> getHistoryDetails(UUID uuid, String type) {
        return dataFile.getConfig().getStringList("history-details." + uuid + "." + type.toUpperCase());
    }

    @Override
    public void resetHistory(UUID uuid, String type) {
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
    public boolean takeHistory(UUID uuid, String type, int amount) {
        FileConfiguration data = dataFile.getConfig();
        String path = "history." + uuid + "." + type.toUpperCase();
        int current = data.getInt(path, 0);
        if (current <= 0) return false;

        data.set(path, Math.max(0, current - amount));
        dataFile.saveConfig();
        return true;
    }

    @Override
    public void saveMute(UUID uuid, String reason, long expiry, String staff) {
        FileConfiguration data = dataFile.getConfig();
        data.set("mutes." + uuid + ".reason", reason);
        data.set("mutes." + uuid + ".expiry", expiry);
        data.set("mutes." + uuid + ".staff", staff);
        dataFile.saveConfig();
    }

    @Override
    public void removeMute(UUID uuid) {
        dataFile.getConfig().set("mutes." + uuid, null);
        dataFile.saveConfig();
    }

    @Override
    public CompletableFuture<Long> getMuteExpiry(UUID uuid) {
        return CompletableFuture.completedFuture(dataFile.getConfig().getLong("mutes." + uuid + ".expiry", 0));
    }

    @Override
    public void saveIP(UUID uuid, String ip) {
        dataFile.getConfig().set("ip-cache." + uuid.toString(), ip);
        dataFile.saveConfig();
    }

    @Override
    public String getIP(UUID uuid) {
        return dataFile.getConfig().getString("ip-cache." + uuid.toString());
    }

    @Override
    public List<UUID> getAltsByIP(String ip) {
        List<UUID> alts = new ArrayList<>();
        org.bukkit.configuration.ConfigurationSection section = dataFile.getConfig().getConfigurationSection("ip-cache");
        if (section == null) return alts;

        for (String uuidStr : section.getKeys(false)) {
            if (ip.equals(section.getString(uuidStr))) {
                try {
                    alts.add(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return alts;
    }

    @Override
    public void close() {
    }
}