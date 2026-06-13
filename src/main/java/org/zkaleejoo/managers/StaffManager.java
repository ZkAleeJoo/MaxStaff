package org.zkaleejoo.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.CustomConfig;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class StaffManager {

    private final MaxStaff plugin;
    private final CustomConfig staffData;
    private final Map<UUID, ItemStack[]> savedInventory = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private final Map<UUID, GameMode> savedGameMode = new HashMap<>();
    private final Map<UUID, Boolean> staffModePlayers = new HashMap<>();
    private final Set<UUID> vanishedPlayers = new HashSet<>();
    private final Set<UUID> persistentVanishedPlayers = new HashSet<>();
    private final Set<UUID> commandSpyPlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> actionBarTasks = new HashMap<>();
    private final Set<String> dirtySections = new HashSet<>();
    private final AtomicBoolean flushInProgress = new AtomicBoolean(false);
    private final AtomicBoolean flushRequestedWhileRunning = new AtomicBoolean(false);
    private final AtomicLong dataVersion = new AtomicLong(0L);
    private int flushTaskId = -1;
    private final Map<String, ItemStack> cachedStaffTools = new HashMap<>();
    private final NamespacedKey vanishedKey;

    public StaffManager(MaxStaff plugin) {
        this.plugin = plugin;
        this.staffData = new CustomConfig("staff_data.yml", null, plugin, true);
        this.vanishedKey = new NamespacedKey(plugin, "vanished");
        this.staffData.registerConfig();

        cacheStaffItems();
        startFlushTask();
    }

    public void cacheStaffItems() {
        cachedStaffTools.clear();
        MainConfigManager config = plugin.getMainConfigManager();

        cachedStaffTools.put("punish_tool",
                createItem(config.getMatPunish(), config.getItemNamePunish(), "punish_tool"));
        cachedStaffTools.put("freeze_tool",
                createItem(config.getMatFreeze(), config.getItemNameFreeze(), "freeze_tool"));
        cachedStaffTools.put("players_tool",
                createItem(config.getMatPlayers(), config.getItemNamePlayers(), "players_tool"));
        cachedStaffTools.put("random_tp_tool",
                createItem(config.getMatRandomTp(), config.getItemNameRandomTp(), "random_tp_tool"));
        cachedStaffTools.put("wall_compass_tool",
                createItem(config.getMatWallCompass(), config.getItemNameWallCompass(), "wall_compass_tool"));
        cachedStaffTools.put("inspect_tool",
                createItem(config.getMatInspect(), config.getItemNameInspect(), "inspect_tool"));
        cachedStaffTools.put("vanish_tool",
                createItem(config.getMatVanish(), config.getItemNameVanish(), "vanish_tool"));
    }

    private void startFlushTask() {
        flushTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::flushDirtySections, 60L, 60L);
    }

    private synchronized void markDirty(String section) {
        dirtySections.add(section);
        dataVersion.incrementAndGet();
    }

    private void flushDirtySections() {
        final Set<String> snapshotSections;
        final String snapshotYaml;
        final long snapshotVersion;
        final Path filePath = plugin.getDataFolder().toPath().resolve(staffData.getPath());

        synchronized (this) {
            if (dirtySections.isEmpty()) {
                return;
            }

            if (!flushInProgress.compareAndSet(false, true)) {
                flushRequestedWhileRunning.set(true);
                return;
            }

            snapshotSections = new HashSet<>(dirtySections);
            snapshotYaml = staffData.getConfig().saveToString();
            snapshotVersion = dataVersion.get();
            dirtySections.removeAll(snapshotSections);
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (snapshotVersion != dataVersion.get()) {
                    return;
                }

                Files.writeString(filePath, snapshotYaml, StandardCharsets.UTF_8);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not flush staff_data.yml: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    synchronized (StaffManager.this) {
                        dirtySections.addAll(snapshotSections);
                    }
                });
            } finally {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    flushInProgress.set(false);
                    boolean hasPendingChanges;
                    synchronized (StaffManager.this) {
                        hasPendingChanges = !dirtySections.isEmpty();
                    }

                    if (flushRequestedWhileRunning.getAndSet(false) || hasPendingChanges) {
                        flushDirtySections();
                    }
                });
            }
        });
    }

    public synchronized void forceFlush() {
        if (flushTaskId != -1) {
            Bukkit.getScheduler().cancelTask(flushTaskId);
            flushTaskId = -1;
        }

        dataVersion.incrementAndGet();
        staffData.saveConfig();
        dirtySections.clear();
        flushRequestedWhileRunning.set(false);
    }

    public void toggleStaffMode(Player player) {
        if (isInStaffMode(player)) {
            disableStaffMode(player);
        } else {
            enableStaffMode(player);
        }
    }

    public void enableStaffMode(Player player) {
        MainConfigManager config = plugin.getMainConfigManager();
        UUID uuid = player.getUniqueId();

        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();
        String gm = player.getGameMode().name();
        int foodLevel = player.getFoodLevel();

        savedInventory.put(uuid, contents);
        savedArmor.put(uuid, armor);

        staffData.getConfig().set("data." + uuid + ".inventory", contents);
        staffData.getConfig().set("data." + uuid + ".armor", armor);
        staffData.getConfig().set("data." + uuid + ".gamemode", gm);
        staffData.getConfig().set("data." + uuid + ".food", foodLevel);
        markDirty("data");

        player.getInventory().clear();
        player.setFoodLevel(20);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);

        giveStaffItems(player);
        staffModePlayers.put(uuid, true);

        setVanish(player, true, false);
        player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getMsgVanishOn()));
        player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getStaffModeEnabled()));
        player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getInventorySaved()));

        plugin.getDiscordManager().sendWebhook("staff_mode", player.getName(), player.getName(), "**Activated** ✅",
                null, null);
    }

    public void disableStaffMode(Player player) {
        MainConfigManager config = plugin.getMainConfigManager();
        UUID uuid = player.getUniqueId();

        player.getInventory().clear();

        ItemStack[] toRestore = null;
        ItemStack[] armorRestore = null;

        if (savedInventory.containsKey(uuid)) {
            toRestore = savedInventory.remove(uuid);
            armorRestore = savedArmor.remove(uuid);
        } else if (staffData.getConfig().contains("data." + uuid)) {
            toRestore = deserializeItemStacks("data." + uuid + ".inventory");
            armorRestore = deserializeItemStacks("data." + uuid + ".armor");
        }

        if (toRestore != null) {
            player.getInventory().setContents(toRestore);
            if (armorRestore != null) {
                player.getInventory().setArmorContents(armorRestore);
            }

            String gmName = staffData.getConfig().getString("data." + uuid + ".gamemode", "SURVIVAL");
            try {
                GameMode originalMode = GameMode.valueOf(gmName);
                player.setGameMode(originalMode);
                player.setAllowFlight(originalMode == GameMode.CREATIVE || originalMode == GameMode.SPECTATOR);
            } catch (IllegalArgumentException e) {
                player.setGameMode(GameMode.SURVIVAL);
            }

            int originalFood = staffData.getConfig().getInt("data." + uuid + ".food", 20);
            player.setFoodLevel(originalFood);
        }

        if (toRestore != null) {
            staffData.getConfig().set("data." + uuid, null);
            staffData.saveConfig();
        } else if (staffData.getConfig().contains("data." + uuid)) {
            plugin.getLogger().warning("Could not restore inventory for " + player.getName()
                    + "; staff_data.yml entry was kept at data." + uuid + " for manual recovery.");
        }

        GameMode currentMode = player.getGameMode();
        if (currentMode == GameMode.CREATIVE || currentMode == GameMode.SPECTATOR) {
            player.setFlying(true);
        } else {
            player.setFlying(false);
        }
        player.setInvulnerable(false);
        staffModePlayers.remove(uuid);

        if (isVanished(player)) {
            setVanish(player, false, true);
        } else {
            refreshActionBar(player);
        }

        player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getStaffModeDisabled()));
        player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getInventoryRestored()));
        plugin.getDiscordManager().sendWebhook("staff_mode", player.getName(), player.getName(), "**Deactivated** ❌",
                null, null);
    }

    private ItemStack[] deserializeItemStacks(String path) {
        Object raw = staffData.getConfig().get(path);
        if (raw instanceof ItemStack[] itemArray) {
            return itemArray;
        }

        if (raw instanceof List<?> rawList) {
            List<ItemStack> itemStacks = new ArrayList<>(rawList.size());
            for (Object entry : rawList) {
                if (entry == null) {
                    itemStacks.add(null);
                    continue;
                }
                if (entry instanceof ItemStack itemStack) {
                    itemStacks.add(itemStack);
                    continue;
                }

                return null;
            }

            return itemStacks.toArray(new ItemStack[0]);
        }

        return null;
    }

    public boolean isInStaffMode(Player player) {
        return staffModePlayers.containsKey(player.getUniqueId());
    }

    private void giveStaffItems(Player player) {
        MainConfigManager config = plugin.getMainConfigManager();
        Map<String, Integer> preferredSlots = new LinkedHashMap<>();
        Map<String, Integer> defaultSlots = new LinkedHashMap<>();

        preferredSlots.put("punish_tool", config.getStaffPunishSlot());
        preferredSlots.put("freeze_tool", config.getStaffFreezeSlot());
        preferredSlots.put("players_tool", config.getStaffPlayersSlot());
        preferredSlots.put("random_tp_tool", config.getStaffRandomTpSlot());
        preferredSlots.put("wall_compass_tool", config.getStaffWallCompassSlot());
        preferredSlots.put("inspect_tool", config.getStaffInspectSlot());
        preferredSlots.put("vanish_tool", config.getStaffVanishSlot());

        defaultSlots.put("punish_tool", 0);
        defaultSlots.put("freeze_tool", 1);
        defaultSlots.put("players_tool", 4);
        defaultSlots.put("random_tp_tool", 5);
        defaultSlots.put("wall_compass_tool", 6);
        defaultSlots.put("inspect_tool", 7);
        defaultSlots.put("vanish_tool", 8);

        Map<String, Integer> resolvedSlots = resolveStaffToolSlots(preferredSlots, defaultSlots);

        if (plugin.isModuleEnabled("sanctions-gui") && cachedStaffTools.containsKey("punish_tool")) {
            player.getInventory().setItem(resolvedSlots.get("punish_tool"),
                    cachedStaffTools.get("punish_tool").clone());
        }
        if (plugin.isModuleEnabled("freeze") && cachedStaffTools.containsKey("freeze_tool")) {
            player.getInventory().setItem(resolvedSlots.get("freeze_tool"),
                    cachedStaffTools.get("freeze_tool").clone());
        }
        if (plugin.isModuleEnabled("staff-mode")) {
            player.getInventory().setItem(resolvedSlots.get("players_tool"),
                    cachedStaffTools.get("players_tool").clone());
            player.getInventory().setItem(resolvedSlots.get("random_tp_tool"),
                    cachedStaffTools.get("random_tp_tool").clone());
            player.getInventory().setItem(resolvedSlots.get("wall_compass_tool"),
                    cachedStaffTools.get("wall_compass_tool").clone());
            player.getInventory().setItem(resolvedSlots.get("inspect_tool"),
                    cachedStaffTools.get("inspect_tool").clone());
            player.getInventory().setItem(resolvedSlots.get("vanish_tool"),
                    cachedStaffTools.get("vanish_tool").clone());
        }
    }

    private Map<String, Integer> resolveStaffToolSlots(Map<String, Integer> preferredSlots,
            Map<String, Integer> defaultSlots) {
        Map<String, Integer> resolvedSlots = new HashMap<>();
        Set<Integer> occupiedSlots = new HashSet<>();

        for (Map.Entry<String, Integer> entry : preferredSlots.entrySet()) {
            String toolKey = entry.getKey();
            int preferredSlot = entry.getValue();
            int defaultSlot = defaultSlots.getOrDefault(toolKey, 0);

            int finalSlot = preferredSlot;
            if (preferredSlot < 0 || preferredSlot > 8) {
                plugin.getLogger().warning("Invalid slot " + preferredSlot + " for " + toolKey
                        + ". Valid hotbar slots: 0-8. Falling back to " + defaultSlot + ".");
                finalSlot = defaultSlot;
            }

            if (occupiedSlots.contains(finalSlot)) {
                int freeSlot = findFirstFreeHotbarSlot(occupiedSlots, defaultSlot);
                plugin.getLogger().warning("Duplicated staff item slot " + finalSlot + " for " + toolKey
                        + ". Reassigning to slot " + freeSlot + ".");
                finalSlot = freeSlot;
            }

            occupiedSlots.add(finalSlot);
            resolvedSlots.put(toolKey, finalSlot);
        }

        return resolvedSlots;
    }

    private int findFirstFreeHotbarSlot(Set<Integer> occupiedSlots, int preferredStart) {
        if (preferredStart >= 0 && preferredStart <= 8 && !occupiedSlots.contains(preferredStart)) {
            return preferredStart;
        }

        for (int slot = 0; slot <= 8; slot++) {
            if (!occupiedSlots.contains(slot)) {
                return slot;
            }
        }

        return 0;
    }

    private ItemStack createItem(Material material, String name, String toolKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtils.legacyToComponentNoItalic(name));
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

            NamespacedKey key = new NamespacedKey(plugin, "staff_tool");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, toolKey);

            item.setItemMeta(meta);
        }
        return item;
    }

    public void toggleVanish(Player player) {

        if (isVanished(player)) {
            setVanish(player, false);
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgVanishOff()));
        } else {
            setVanish(player, true);
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgVanishOn()));
        }
    }

    public void setVanish(Player player, boolean enable) {
        setVanish(player, enable, true);
    }

    public void setVanish(Player player, boolean enable, boolean persistState) {
        applyVanish(player, enable, true);

        if (persistState) {
            persistVanishState(player, enable);
        }
    }

    @SuppressWarnings("null")
    private void applyVanish(Player player, boolean enable, boolean playSound) {
        if (enable) {
            vanishedPlayers.add(player.getUniqueId());
            player.getPersistentDataContainer().set(vanishedKey, PersistentDataType.BOOLEAN, true);
            player.setCollidable(false);

            if (playSound) {
                var location = player.getLocation();
                if (location != null) {
                    player.playSound(location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
                }
            }

            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.hasPermission("maxstaff.see.vanish")) {
                    target.hidePlayer(plugin, player);
                    continue;
                }

                target.showPlayer(plugin, player);
            }
        } else {
            vanishedPlayers.remove(player.getUniqueId());
            player.getPersistentDataContainer().remove(vanishedKey);
            player.setCollidable(true);

            if (playSound) {
                var location = player.getLocation();
                if (location != null) {
                    player.playSound(location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
                }
            }

            for (Player target : Bukkit.getOnlinePlayers()) {
                target.showPlayer(plugin, player);
            }
        }

        refreshActionBar(player);
    }

    private void refreshActionBar(Player player) {
        StaffActionBarPolicy.ActionBarType type = StaffActionBarPolicy.select(
                isInStaffMode(player),
                isVanished(player));

        if (type == StaffActionBarPolicy.ActionBarType.NONE) {
            stopActionBar(player.getUniqueId(), player);
            return;
        }

        if (actionBarTasks.containsKey(player.getUniqueId())) {
            sendCurrentActionBar(player, type);
            return;
        }

        UUID uuid = player.getUniqueId();
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> updateActionBar(uuid), 0L, 20L);
        actionBarTasks.put(uuid, task);
    }

    private void updateActionBar(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            stopActionBar(uuid, null);
            return;
        }

        StaffActionBarPolicy.ActionBarType type = StaffActionBarPolicy.select(
                isInStaffMode(player),
                isVanished(player));
        if (type == StaffActionBarPolicy.ActionBarType.NONE) {
            stopActionBar(uuid, player);
            return;
        }

        sendCurrentActionBar(player, type);
    }

    private void sendCurrentActionBar(Player player, StaffActionBarPolicy.ActionBarType type) {
        MainConfigManager config = plugin.getMainConfigManager();
        String statusText = isVanished(player) ? config.getStatusEnabled() : config.getStatusDisabled();
        String template = type == StaffActionBarPolicy.ActionBarType.STAFF_MODE
                ? config.getMsgActionBar()
                : config.getVanishActionBar();
        String message = MessageUtils.getColoredMessage(template.replace("{status}", statusText));
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(message));
    }

    private void stopActionBar(UUID uuid, Player player) {
        BukkitTask task = actionBarTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        if (player != null && player.isOnline()) {
            player.sendActionBar(Component.empty());
        }
    }

    public void restoreVanishOnJoin(Player player) {
        MainConfigManager config = plugin.getMainConfigManager();
        if (!config.isVanishPersistenceEnabled()) {
            return;
        }

        if (plugin.getPunishmentManager() instanceof PunishmentManagerMysql mysqlManager) {
            UUID uuid = player.getUniqueId();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                boolean persisted = loadVanishStateFromDatabase(mysqlManager, uuid);
                if (!persisted) {
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player online = Bukkit.getPlayer(uuid);
                    if (online != null && online.isOnline()) {
                        persistentVanishedPlayers.add(uuid);
                        applyVanish(online, true, false);
                    }
                });
            });
            return;
        }

        if (staffData.getConfig().getBoolean("vanish." + player.getUniqueId() + ".enabled", false)) {
            persistentVanishedPlayers.add(player.getUniqueId());
            applyVanish(player, true, false);
        }
    }

    private boolean loadVanishStateFromDatabase(PunishmentManagerMysql mysqlManager, UUID uuid) {
        try (Connection conn = mysqlManager.getSqlConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT enabled FROM maxstaff_vanish_states WHERE uuid = ? LIMIT 1")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean("enabled");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not load vanish state for " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    private void persistVanishState(Player player, boolean enabled) {
        MainConfigManager config = plugin.getMainConfigManager();
        if (!config.isVanishPersistenceEnabled()) {
            return;
        }

        if (enabled) {
            persistentVanishedPlayers.add(player.getUniqueId());
        } else {
            persistentVanishedPlayers.remove(player.getUniqueId());
        }

        if (plugin.getPunishmentManager() instanceof PunishmentManagerMysql mysqlManager) {
            persistVanishStateInDatabase(mysqlManager, player.getUniqueId(), player.getName(), enabled);
            return;
        }

        String path = "vanish." + player.getUniqueId();
        if (enabled) {
            staffData.getConfig().set(path + ".enabled", true);
            staffData.getConfig().set(path + ".name", player.getName());
            staffData.getConfig().set(path + ".updated-at", System.currentTimeMillis());
        } else {
            staffData.getConfig().set(path, null);
        }
        markDirty("vanish");
    }

    private void persistVanishStateInDatabase(PunishmentManagerMysql mysqlManager, UUID uuid, String name,
            boolean enabled) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = mysqlManager.getSqlConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO maxstaff_vanish_states (uuid, name, enabled, updated_at, server_id) "
                                    + "VALUES (?, ?, ?, ?, ?) "
                                    + "ON DUPLICATE KEY UPDATE name = VALUES(name), enabled = VALUES(enabled), "
                                    + "updated_at = VALUES(updated_at), server_id = VALUES(server_id)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setBoolean(3, enabled);
                ps.setLong(4, System.currentTimeMillis());
                ps.setString(5, plugin.getMainConfigManager().getDbServerId());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Could not persist vanish state for " + uuid + ": " + e.getMessage());
            }
        });
    }

    public boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }

    public java.util.List<UUID> getVanishedPlayers() {
        return new java.util.ArrayList<>(vanishedPlayers);
    }

    public void disableAllStaff() {
        for (UUID uuid : new java.util.ArrayList<>(staffModePlayers.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                disableStaffMode(player);
            }
        }
    }

    public void toggleCommandSpy(Player player) {
        if (commandSpyPlayers.contains(player.getUniqueId())) {
            commandSpyPlayers.remove(player.getUniqueId());
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgCmdSpyDisabled()));
        } else {
            commandSpyPlayers.add(player.getUniqueId());
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgCmdSpyEnabled()));
        }
    }

    public boolean isSpying(Player player) {
        return commandSpyPlayers.contains(player.getUniqueId());
    }

    public void updateSavedGameMode(Player player, org.bukkit.GameMode newMode) {
        if (savedGameMode.containsKey(player.getUniqueId())) {
            savedGameMode.put(player.getUniqueId(), newMode);
        }
    }

    public boolean hasPersistentStaffData(UUID uuid) {
        return staffData.getConfig().contains("data." + uuid);
    }

    public void handlePlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();

        stopActionBar(uuid, null);
        savedInventory.remove(uuid);
        savedArmor.remove(uuid);
        savedGameMode.remove(uuid);
        staffModePlayers.remove(uuid);
        commandSpyPlayers.remove(uuid);

        vanishedPlayers.remove(uuid);
        persistentVanishedPlayers.remove(uuid);
    }
}
