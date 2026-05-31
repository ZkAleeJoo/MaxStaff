package org.zkaleejoo.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;

public class ClientTrackerManager implements Listener, PluginMessageListener {

    private static final String MODERN_CHANNEL = "minecraft:brand";
    private static final String LEGACY_CHANNEL = "MC|Brand";
    private static final String MODERN_FORGE_CHANNEL = "fml:handshake";
    private static final String LEGACY_FORGE_CHANNEL = "FML|HS";
    private static final String LEGACY_FORGE_ALT_CHANNEL = "FML";

    private static final Map<String, List<String>> DEFAULT_SIGNATURES = createDefaultSignatures();

    private final MaxStaff plugin;
    private final Map<UUID, BukkitTask> pendingTimeouts = new ConcurrentHashMap<>();
    private final Map<UUID, String> detectedClients = new ConcurrentHashMap<>();

    public ClientTrackerManager(MaxStaff plugin) {
        this.plugin = plugin;
    }

    public void registerChannels() {
        registerIncomingChannel(MODERN_CHANNEL);
        registerIncomingChannel(LEGACY_CHANNEL);
        registerIncomingChannel(MODERN_FORGE_CHANNEL);
        registerIncomingChannel(LEGACY_FORGE_CHANNEL);
        registerIncomingChannel(LEGACY_FORGE_ALT_CHANNEL);
        registerOutgoingChannel(MODERN_CHANNEL);
        registerOutgoingChannel(LEGACY_CHANNEL);
        registerOutgoingChannel(MODERN_FORGE_CHANNEL);
        registerOutgoingChannel(LEGACY_FORGE_CHANNEL);
        registerOutgoingChannel(LEGACY_FORGE_ALT_CHANNEL);
    }

    public void unregisterChannels() {
        unregisterIncomingChannel(MODERN_CHANNEL);
        unregisterIncomingChannel(LEGACY_CHANNEL);
        unregisterIncomingChannel(MODERN_FORGE_CHANNEL);
        unregisterIncomingChannel(LEGACY_FORGE_CHANNEL);
        unregisterIncomingChannel(LEGACY_FORGE_ALT_CHANNEL);    
        unregisterOutgoingChannel(MODERN_CHANNEL);
        unregisterOutgoingChannel(LEGACY_CHANNEL);
        unregisterOutgoingChannel(MODERN_FORGE_CHANNEL);
        unregisterOutgoingChannel(LEGACY_FORGE_CHANNEL);
        unregisterOutgoingChannel(LEGACY_FORGE_ALT_CHANNEL);
        pendingTimeouts.values().forEach(BukkitTask::cancel);
        pendingTimeouts.clear();
        detectedClients.clear();
    }

    private void registerIncomingChannel(String channel) {
        try {
            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, channel, this);
        } catch (IllegalArgumentException exception) {
        }
    }

    private void registerOutgoingChannel(String channel) {
        try {
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, channel);
        } catch (IllegalArgumentException exception) {
        }
    }

    private void unregisterIncomingChannel(String channel) {
        try {
            plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, channel, this);
        } catch (IllegalArgumentException exception) {
        }
    }

    private void unregisterOutgoingChannel(String channel) {
        try {
            plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, channel);
        } catch (IllegalArgumentException exception) {
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        MainConfigManager config = plugin.getMainConfigManager();
        if (!config.isClientTrackerEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        BukkitTask oldTask = pendingTimeouts.remove(uuid);
        if (oldTask != null) {
            oldTask.cancel();
        }

        if (detectedClients.containsKey(uuid)) {
            return;
        }

        try {
            String nativeBrand = player.getClientBrandName();
            if (nativeBrand != null && !nativeBrand.isEmpty()) {
                String normalizedClient = resolveClientName(nativeBrand, config);
                completeDetection(player, normalizedClient, nativeBrand);
                return; 
            }
        } catch (NoSuchMethodError ignored) {
        }

        int timeoutTicks = Math.max(20, config.getClientTrackerTimeoutTicks());
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> handleUnknownClient(player), timeoutTicks);
        pendingTimeouts.put(uuid, task);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearPlayerState(event.getPlayer().getUniqueId());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        MainConfigManager config = plugin.getMainConfigManager();
        if (!config.isClientTrackerEnabled()) {
            return;
        }

        if (MODERN_FORGE_CHANNEL.equals(channel) || LEGACY_FORGE_CHANNEL.equals(channel) || LEGACY_FORGE_ALT_CHANNEL.equals(channel)) {
            if (!detectedClients.containsKey(player.getUniqueId())) {
                completeDetection(player, "Forge", "forge-handshake");
            }
            return;
        }

        if (!MODERN_CHANNEL.equals(channel) && !LEGACY_CHANNEL.equals(channel)) {
            return;
        }

        String rawBrand = parseBrand(message);
        if (rawBrand == null || rawBrand.isBlank()) {
            return;
        }

        if (detectedClients.containsKey(player.getUniqueId())) {
            return;
        }

        String normalizedClient = resolveClientName(rawBrand, config);
        completeDetection(player, normalizedClient, rawBrand);
    }

    public String getDetectedClient(UUID uuid) {
        return detectedClients.getOrDefault(uuid, plugin.getMainConfigManager().getClientTrackerUnknownName());
    }

    private void handleUnknownClient(Player player) {
        if (!player.isOnline()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (detectedClients.containsKey(uuid)) {
            return;
        }

        completeDetection(player, plugin.getMainConfigManager().getClientTrackerUnknownName(), "unknown");
    }

    private void completeDetection(Player player, String normalizedClient, String rawBrand) {
        UUID uuid = player.getUniqueId();
        BukkitTask timeoutTask = pendingTimeouts.remove(uuid);
        if (timeoutTask != null) {
            timeoutTask.cancel();
        }

        detectedClients.put(uuid, normalizedClient);
        notifyStaff(player, normalizedClient, rawBrand);
    }

    private void notifyStaff(Player target, String normalizedClient, String rawBrand) {
        MainConfigManager config = plugin.getMainConfigManager();
        if (!config.isClientTrackerNotifyEnabled()) {
            return;
        }

        String base = config.getPrefix() + config.getClientTrackerJoinMessage()
                .replace("{player}", target.getName())
                .replace("{client}", normalizedClient)
                .replace("{raw_client}", rawBrand);

        String colored = MessageUtils.getColoredMessage(base);
        String permission = config.getClientTrackerNotifyPermission();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null) continue;
            if (permission == null || permission.isBlank() || online.hasPermission(permission)) {
                online.sendMessage(colored);
            }
        }
    }

    private String resolveClientName(String rawBrand, MainConfigManager config) {
        String normalized = normalizeBrand(rawBrand);
        if (normalized.isEmpty()) {
            return config.getClientTrackerUnknownName();
        }

        Map<String, List<String>> signatures = new LinkedHashMap<>(DEFAULT_SIGNATURES);
        signatures.putAll(config.getClientTrackerCustomMappings());

        for (Map.Entry<String, List<String>> entry : signatures.entrySet()) {
            String label = entry.getKey();
            for (String signature : entry.getValue()) {
                if (!signature.isBlank() && normalized.contains(signature.toLowerCase(Locale.ROOT))) {
                    return label;
                }
            }
        }

        return sanitizeRawLabel(rawBrand);
    }

    private static Map<String, List<String>> createDefaultSignatures() {
        Map<String, List<String>> defaults = new LinkedHashMap<>();
        defaults.put("Lunar Client", Collections.singletonList("lunarclient"));
        defaults.put("Badlion Client", Collections.singletonList("badlion"));
        defaults.put("Feather Client", Collections.singletonList("feather"));
        defaults.put("Forge", Arrays.asList("fml", "forge", "neoforge", "forgehandshake"));
        defaults.put("Fabric", Collections.singletonList("fabric"));
        defaults.put("LiteLoader", Collections.singletonList("liteloader"));
        defaults.put("CheatBreaker", Collections.singletonList("cheatbreaker"));
        defaults.put("Vanilla", Arrays.asList("vanilla", "minecraft"));
        return Collections.unmodifiableMap(defaults);
    }

    private String parseBrand(byte[] message) {
        if (message == null || message.length == 0) {
            return null;
        }

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(message))) {
            String legacy = input.readUTF();
            if (legacy != null && !legacy.isBlank()) {
                return legacy;
            }
        } catch (IOException ignored) {
        }

        int[] indexRef = {0};
        int strLen = readVarInt(message, indexRef);
        if (strLen > 0 && indexRef[0] + strLen <= message.length) {
            return new String(message, indexRef[0], strLen, StandardCharsets.UTF_8);
        }

        return new String(message, StandardCharsets.UTF_8);
    }

    private int readVarInt(byte[] input, int[] indexRef) {
        int numRead = 0;
        int result = 0;
        byte read;

        do {
            if (indexRef[0] >= input.length) {
                return -1;
            }

            read = input[indexRef[0]++];
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 5) {
                return -1;
            }
        } while ((read & 0b10000000) != 0);

        return result;
    }

    private String normalizeBrand(String value) {
        String withoutControlChars = value.replaceAll("\\p{Cntrl}", "");
        return withoutControlChars.toLowerCase(Locale.ROOT).replace(" ", "").trim();
    }

    private String sanitizeRawLabel(String rawBrand) {
        String cleaned = rawBrand.replaceAll("\\p{Cntrl}", "").trim();
        return cleaned.isEmpty() ? plugin.getMainConfigManager().getClientTrackerUnknownName() : cleaned;
    }

    private void clearPlayerState(UUID uuid) {
        BukkitTask task = pendingTimeouts.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        detectedClients.remove(uuid);
    }
}