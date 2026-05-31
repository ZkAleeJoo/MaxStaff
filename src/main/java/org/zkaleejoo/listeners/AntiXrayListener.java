package org.zkaleejoo.listeners;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager.ClickActionType;
import org.zkaleejoo.utils.MessageUtils;

public class AntiXrayListener implements Listener {

    private final MaxStaff plugin;
    private final Set<Material> alertBlocks;
    private final Map<UUID, EnumMap<Material, Integer>> minedRates = new ConcurrentHashMap<>();
    private final Map<UUID, EnumMap<Material, Deque<Long>>> recentMines = new ConcurrentHashMap<>();
    private final Map<UUID, XraySuspect> suspects = new ConcurrentHashMap<>();
    private final Map<UUID, Long> alertCooldowns = new ConcurrentHashMap<>();

    public AntiXrayListener(MaxStaff plugin) {
        this.plugin = plugin;
        this.alertBlocks = plugin.getMainConfigManager().getAntiXrayAlertBlocks();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || !plugin.getMainConfigManager().isAntiXrayEnabled()) {
            return;
        }

        Block block = event.getBlock();
        if (block == null) {
            return;
        }

        Material material = block.getType();
        if (!alertBlocks.contains(material)) {
            return;
        }

        if (plugin.getMainConfigManager().isAntiXrayIgnoreCreative()
                && player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        String bypassPermission = plugin.getMainConfigManager().getAntiXrayBypassPermission();
        if (bypassPermission != null && !bypassPermission.isBlank() && player.hasPermission(bypassPermission)) {
            return;
        }

        int rate = increaseRate(player.getUniqueId(), material);
        int windowRate = trackRecentRate(player.getUniqueId(), material);
        int windowTotal = getWindowTotal(player.getUniqueId());

        if (!isSuspicious(windowRate, windowTotal, rate)) {
            return;
        }

        XraySuspect suspect = updateSuspect(player, block, material, rate, windowRate, windowTotal);
        notifyStaff(player, block, material, suspect);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player != null) {
            minedRates.remove(player.getUniqueId());
            recentMines.remove(player.getUniqueId());
            alertCooldowns.remove(player.getUniqueId());
        }
    }

    public List<XraySuspect> getSuspects() {
        List<XraySuspect> sortedSuspects = new ArrayList<>(suspects.values());
        sortedSuspects.sort((first, second) -> Long.compare(second.getLastAlertMillis(), first.getLastAlertMillis()));
        return Collections.unmodifiableList(sortedSuspects);
    }

    public void removeSuspect(UUID uuid) {
        if (uuid != null) {
            suspects.remove(uuid);
        }
    }

    public void removeSuspectByName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return;
        }

        for (XraySuspect suspect : getSuspects()) {
            if (suspect.getPlayerName().equalsIgnoreCase(playerName)) {
                removeSuspect(suspect.getPlayerUuid());
            }
        }
    }

    private int increaseRate(UUID uuid, Material material) {
        EnumMap<Material, Integer> playerRates = minedRates.computeIfAbsent(uuid,
                ignored -> new EnumMap<>(Material.class));
        Integer current = playerRates.get(material);
        int rate = (current == null ? 0 : current) + 1;
        playerRates.put(material, rate);
        return rate;
    }

    private int trackRecentRate(UUID uuid, Material material) {
        long now = System.currentTimeMillis();
        long oldestAllowed = now - (plugin.getMainConfigManager().getAntiXrayRateWindowSeconds() * 1000L);
        EnumMap<Material, Deque<Long>> playerRates = recentMines.computeIfAbsent(uuid,
                ignored -> new EnumMap<>(Material.class));
        Deque<Long> timestamps = playerRates.computeIfAbsent(material, ignored -> new ArrayDeque<>());
        timestamps.addLast(now);
        pruneOldEntries(timestamps, oldestAllowed);
        return timestamps.size();
    }

    private int getWindowTotal(UUID uuid) {
        EnumMap<Material, Deque<Long>> playerRates = recentMines.get(uuid);
        if (playerRates == null) {
            return 0;
        }

        long oldestAllowed = System.currentTimeMillis()
                - (plugin.getMainConfigManager().getAntiXrayRateWindowSeconds() * 1000L);
        int total = 0;
        for (Deque<Long> timestamps : playerRates.values()) {
            pruneOldEntries(timestamps, oldestAllowed);
            total += timestamps.size();
        }
        return total;
    }

    private void pruneOldEntries(Deque<Long> timestamps, long oldestAllowed) {
        while (!timestamps.isEmpty() && timestamps.peekFirst() < oldestAllowed) {
            timestamps.removeFirst();
        }
    }

    private boolean isSuspicious(int windowRate, int windowTotal, int sessionRate) {
        return windowRate >= plugin.getMainConfigManager().getAntiXrayMaterialThreshold()
                || windowTotal >= plugin.getMainConfigManager().getAntiXrayTotalThreshold()
                || sessionRate >= plugin.getMainConfigManager().getAntiXraySessionThreshold();
    }

    private XraySuspect updateSuspect(Player player, Block block, Material material, int rate, int windowRate,
            int windowTotal) {
        long now = System.currentTimeMillis();
        Location location = block.getLocation();
        XraySuspect suspect = suspects.compute(player.getUniqueId(), (uuid, existing) -> {
            if (existing == null) {
                return new XraySuspect(player.getUniqueId(), player.getName(), material, rate, windowRate, windowTotal,
                        location, now, now);
            }
            existing.update(player.getName(), material, rate, windowRate, windowTotal, location, now);
            return existing;
        });
        return suspect;
    }

    private void notifyStaff(Player player, Block block, Material material, XraySuspect suspect) {
        if (isAlertOnCooldown(player.getUniqueId())) {
            return;
        }

        alertCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        String permission = plugin.getMainConfigManager().getAntiXrayNotifyPermission();
        String world = block.getWorld().getName();
        String x = String.valueOf(block.getX());
        String y = String.valueOf(block.getY());
        String z = String.valueOf(block.getZ());
        String mineral = plugin.getMainConfigManager().getAntiXrayDisplayName(material);
        int rate = suspect.getSessionRate();
        int windowRate = suspect.getWindowRate();
        int windowTotal = suspect.getWindowTotal();

        String message = replacePlaceholders(plugin.getMainConfigManager().getAntiXrayAlertMessage(),
                player.getName(), mineral, world, x, y, z, rate, windowRate, windowTotal, null);
        String clickAction = replacePlaceholders(plugin.getMainConfigManager().getAntiXrayClickCommandTemplate(),
                player.getName(), mineral, world, x, y, z, rate, windowRate, windowTotal, null);

        if (plugin.getDiscordManager() != null) {
            plugin.getDiscordManager().sendXrayAlertWebhook(player.getName(), mineral, world, block.getX(), block.getY(),
                    block.getZ(), rate, windowRate, windowTotal,
                    plugin.getMainConfigManager().getAntiXrayRateWindowSeconds());
        }

        if (!plugin.getMainConfigManager().isAntiXrayNotifyEnabled()) {
            return;
        }

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff == null || staff.equals(player)) {
                continue;
            }
            if (permission != null && !permission.isBlank() && !staff.hasPermission(permission)) {
                continue;
            }

            Component notification = MessageUtils.legacyToComponentNoItalic(message);
            String parsedAction = clickAction == null ? "" : clickAction.replace("{staff}", staff.getName());
            if (plugin.getMainConfigManager().isAntiXrayClickEnabled() && !parsedAction.isBlank()) {
                notification = attachClickAndHover(notification, parsedAction, staff, player, mineral, world, x, y, z,
                        rate, windowRate, windowTotal);
            }

            staff.sendMessage(notification);
        }
    }

    private boolean isAlertOnCooldown(UUID uuid) {
        Long lastAlert = alertCooldowns.get(uuid);
        if (lastAlert == null) {
            return false;
        }
        long cooldownMillis = plugin.getMainConfigManager().getAntiXrayAlertCooldownSeconds() * 1000L;
        return System.currentTimeMillis() - lastAlert < cooldownMillis;
    }

    private Component attachClickAndHover(Component notification, String parsedAction, Player staff, Player player,
            String mineral, String world, String x, String y, String z, int rate, int windowRate, int windowTotal) {
        if (plugin.getMainConfigManager().getAntiXrayClickActionType() == ClickActionType.RUN_COMMAND) {
            notification = notification.clickEvent(ClickEvent.runCommand(parsedAction));
        } else {
            notification = notification.clickEvent(ClickEvent.suggestCommand(parsedAction));
        }

        Component hover = Component.empty();
        boolean hasHoverLines = false;
        for (String hoverLine : plugin.getMainConfigManager().getAntiXrayAlertHover()) {
            String parsedHover = replacePlaceholders(hoverLine, player.getName(), mineral, world, x, y, z, rate,
                    windowRate, windowTotal, staff.getName());
            if (hasHoverLines) {
                hover = hover.append(Component.newline());
            }
            hover = hover.append(MessageUtils.legacyToComponentNoItalic(parsedHover));
            hasHoverLines = true;
        }

        return hasHoverLines ? notification.hoverEvent(HoverEvent.showText(hover)) : notification;
    }

    private String replacePlaceholders(String input, String player, String mineral, String world, String x, String y,
            String z, int rate, int windowRate, int windowTotal, String staff) {
        if (input == null) {
            return "";
        }

        String parsed = input
                .replace("{player}", player)
                .replace("{mineral}", mineral)
                .replace("{world}", world)
                .replace("{prefix}", plugin.getMainConfigManager().getPrefix())
                .replace("{x}", x)
                .replace("{y}", y)
                .replace("{z}", z)
                .replace("{rate}", String.valueOf(rate))
                .replace("{window_rate}", String.valueOf(windowRate))
                .replace("{window_total}", String.valueOf(windowTotal))
                .replace("{window_seconds}", String.valueOf(plugin.getMainConfigManager().getAntiXrayRateWindowSeconds()));
        return staff == null ? parsed : parsed.replace("{staff}", staff);
    }

    public static final class XraySuspect {
        private final UUID playerUuid;
        private String playerName;
        private Material lastMaterial;
        private int sessionRate;
        private int windowRate;
        private int windowTotal;
        private Location lastLocation;
        private final long firstDetectedMillis;
        private long lastAlertMillis;

        private XraySuspect(UUID playerUuid, String playerName, Material lastMaterial, int sessionRate, int windowRate,
                int windowTotal, Location lastLocation, long firstDetectedMillis, long lastAlertMillis) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.lastMaterial = lastMaterial;
            this.sessionRate = sessionRate;
            this.windowRate = windowRate;
            this.windowTotal = windowTotal;
            this.lastLocation = lastLocation;
            this.firstDetectedMillis = firstDetectedMillis;
            this.lastAlertMillis = lastAlertMillis;
        }

        private void update(String playerName, Material lastMaterial, int sessionRate, int windowRate, int windowTotal,
                Location lastLocation, long lastAlertMillis) {
            this.playerName = playerName;
            this.lastMaterial = lastMaterial;
            this.sessionRate = sessionRate;
            this.windowRate = windowRate;
            this.windowTotal = windowTotal;
            this.lastLocation = lastLocation;
            this.lastAlertMillis = lastAlertMillis;
        }

        public UUID getPlayerUuid() {
            return playerUuid;
        }

        public String getPlayerName() {
            return playerName;
        }

        public Material getLastMaterial() {
            return lastMaterial;
        }

        public int getSessionRate() {
            return sessionRate;
        }

        public int getWindowRate() {
            return windowRate;
        }

        public int getWindowTotal() {
            return windowTotal;
        }

        public Location getLastLocation() {
            return lastLocation == null ? null : lastLocation.clone();
        }

        public long getFirstDetectedMillis() {
            return firstDetectedMillis;
        }

        public long getLastAlertMillis() {
            return lastAlertMillis;
        }

        public OfflinePlayer getOfflinePlayer() {
            return Bukkit.getOfflinePlayer(playerUuid);
        }
    }
}
