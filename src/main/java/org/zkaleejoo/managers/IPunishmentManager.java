package org.zkaleejoo.managers;

import org.bukkit.command.CommandSender;
import java.util.List;
import java.util.UUID;

public interface IPunishmentManager {
    void logHistory(String targetName, String type, String reason, String staff, String duration);
    int getHistoryCount(String targetName, String type);
    List<String> getHistoryDetails(String targetName, String type);
    
    void kickPlayer(CommandSender staff, String targetName, String reason);
    void banPlayer(CommandSender staff, String targetName, String reason, String durationStr);
    void unbanPlayer(CommandSender staff, String targetName);
    
    void mutePlayer(CommandSender staff, String targetName, String reason, String durationStr);
    void unmutePlayer(CommandSender staff, String targetName);
    boolean isMuted(UUID uuid);
    
    void warnPlayer(CommandSender staff, String targetName, String reason);
    
    void resetHistory(String targetName, String type);
    boolean takeHistory(String targetName, String type, int amount);
    
    List<String> getBannedPlayerNames();
    List<String> getMutedPlayerNames();
    
    void savePlayerIP(UUID uuid, String ip);
    String getPlayerIP(String targetName);
    
    void banIPPlayer(CommandSender staff, String target, String reason, String durationStr);
    void unbanIPPlayer(CommandSender staff, String target);
    
    List<UUID> getAllAccountsByIP(String ip);
    
    default void close() {} 
}