package org.zkaleejoo.managers.storage;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageProvider {
    void init();
    void close();

    void logHistory(UUID uuid, String name, String type, String reason, String staff, String duration);
    int getHistoryCount(UUID uuid, String type);
    List<String> getHistoryDetails(UUID uuid, String type);
    void resetHistory(UUID uuid, String type);
    boolean takeHistory(UUID uuid, String type, int amount);

    void saveMute(UUID uuid, String reason, long expiry, String staff);
    void removeMute(UUID uuid);
    CompletableFuture<Long> getMuteExpiry(UUID uuid);

    void saveIP(UUID uuid, String ip);
    String getIP(UUID uuid);
    List<UUID> getAltsByIP(String ip);
}