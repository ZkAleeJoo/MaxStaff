package org.zkaleejoo.managers;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.zkaleejoo.MaxStaff;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InventorySnapshotManager {

    private static final int STORAGE_SIZE = 36;
    private static final int ARMOR_SIZE = 4;

    private final MaxStaff plugin;
    private final File dataFolder;
    private final ConcurrentMap<String, IndexedSnapshotFile> indexedFilesByPath = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, IndexedSnapshotFile> indexedFilesByUuid = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, IndexedSnapshotFile> snapshotEntriesByName = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, IndexedSnapshotFile> deathEntriesByName = new ConcurrentHashMap<>();

    public InventorySnapshotManager(MaxStaff plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "invsee-cache");

        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create invsee-cache folder at " + dataFolder.getAbsolutePath());
        }

        rebuildIndex();
    }

    public void rebuildIndex() {
        indexedFilesByPath.clear();
        indexedFilesByUuid.clear();
        snapshotEntriesByName.clear();
        deathEntriesByName.clear();

        for (File file : getSnapshotFiles()) {
            indexFile(file, YamlConfiguration.loadConfiguration(file));
        }
    }

    public void saveSnapshot(Player player) {
        if (player == null) {
            return;
        }

        File file = resolveFile(player.getUniqueId());
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ItemStack[] storage = cloneArray(player.getInventory().getStorageContents(), STORAGE_SIZE);
        ItemStack[] armor = cloneArray(player.getInventory().getArmorContents(), ARMOR_SIZE);
        ItemStack offhand = cloneItem(player.getInventory().getItemInOffHand());
        ItemStack mainHand = cloneItem(player.getInventory().getItemInMainHand());

        config.set("player.uuid", player.getUniqueId().toString());
        config.set("player.name", player.getName());

        if (!hasAnyItems(storage, armor, offhand, mainHand)) {
            config.set("snapshot", null);
        } else {
            config.set("snapshot.updated-at", System.currentTimeMillis());
            config.set("snapshot.storage", storage);
            config.set("snapshot.armor", armor);
            config.set("snapshot.offhand", offhand);
            config.set("snapshot.mainhand", mainHand);
        }

        persistAndReindex(file, config, "inventory snapshot for " + player.getName());
    }

    public void saveDeathSnapshot(Player player, String deathCause) {
        if (player == null) {
            return;
        }

        File file = resolveFile(player.getUniqueId());
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ItemStack[] storage = cloneArray(player.getInventory().getStorageContents(), STORAGE_SIZE);
        ItemStack[] armor = cloneArray(player.getInventory().getArmorContents(), ARMOR_SIZE);
        ItemStack mainHand = cloneItem(player.getInventory().getItemInMainHand());
        ItemStack offhand = cloneItem(player.getInventory().getItemInOffHand());

        config.set("player.uuid", player.getUniqueId().toString());
        config.set("player.name", player.getName());

        if (!hasAnyItems(storage, armor, offhand, mainHand)) {
            config.set("death", null);
        } else {
            config.set("death.updated-at", System.currentTimeMillis());
            config.set("death.cause", deathCause == null || deathCause.isBlank() ? "Unknown" : deathCause);
            config.set("death.storage", storage);
            config.set("death.armor", armor);
            config.set("death.offhand", offhand);
            config.set("death.xp.level", player.getLevel());
            config.set("death.xp.total", player.getTotalExperience());
            config.set("death.xp.progress", player.getExp());
        }

        persistAndReindex(file, config, "death snapshot for " + player.getName());
    }

    public Optional<InventorySnapshot> loadSnapshotByName(String playerName) {
        IndexedSnapshotFile entry = snapshotEntriesByName.get(normalizeName(playerName));
        if (entry == null) {
            return Optional.empty();
        }

        File file = entry.file();
        if (!file.exists()) {
            removeIndexedEntry(file.getAbsolutePath());
            return Optional.empty();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("snapshot")) {
            indexFile(file, config);
            return Optional.empty();
        }

        indexFile(file, config);
        return Optional.of(buildSnapshot(config));
    }

    @SuppressWarnings("null")
    public List<String> getCachedPlayerNames() {
        return snapshotEntriesByName.values().stream()
                .map(IndexedSnapshotFile::playerName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @SuppressWarnings("null")
    public List<DeathSnapshot> getDeathSnapshots() {
        List<DeathSnapshot> snapshots = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (IndexedSnapshotFile entry : new ArrayList<>(deathEntriesByName.values())) {
            File file = entry.file();
            if (!file.exists()) {
                removeIndexedEntry(file.getAbsolutePath());
                continue;
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (!config.contains("death")) {
                indexFile(file, config);
                continue;
            }
            if (isDeathSnapshotExpired(config, now) && clearDeathSnapshot(file, config)) {
                continue;
            }

            indexFile(file, config);
            snapshots.add(buildDeathSnapshot(config));
        }

        snapshots.sort(Comparator.comparingLong(DeathSnapshot::updatedAt).reversed());
        return snapshots;
    }

    public Optional<DeathSnapshot> loadDeathSnapshotByName(String playerName) {
        IndexedSnapshotFile entry = deathEntriesByName.get(normalizeName(playerName));
        if (entry == null) {
            return Optional.empty();
        }

        File file = entry.file();
        if (!file.exists()) {
            removeIndexedEntry(file.getAbsolutePath());
            return Optional.empty();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("death")) {
            indexFile(file, config);
            return Optional.empty();
        }

        long now = System.currentTimeMillis();
        if (isDeathSnapshotExpired(config, now)) {
            clearDeathSnapshot(file, config);
            return Optional.empty();
        }

        indexFile(file, config);
        return Optional.of(buildDeathSnapshot(config));
    }

    public Optional<DeathSnapshot> consumeDeathSnapshotByName(String playerName) {
        IndexedSnapshotFile entry = deathEntriesByName.get(normalizeName(playerName));
        if (entry == null) {
            return Optional.empty();
        }

        File file = entry.file();
        if (!file.exists()) {
            removeIndexedEntry(file.getAbsolutePath());
            return Optional.empty();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("death")) {
            indexFile(file, config);
            return Optional.empty();
        }

        long now = System.currentTimeMillis();
        if (isDeathSnapshotExpired(config, now)) {
            clearDeathSnapshot(file, config);
            return Optional.empty();
        }

        DeathSnapshot snapshot = buildDeathSnapshot(config);
        config.set("death", null);

        if (!persistAndReindex(file, config, "death snapshot consumption for " + snapshot.playerName())) {
            return Optional.empty();
        }

        return Optional.of(snapshot);
    }

    public int cleanupExpiredDeathSnapshots() {
        int removed = 0;
        long now = System.currentTimeMillis();

        for (IndexedSnapshotFile entry : new ArrayList<>(deathEntriesByName.values())) {
            File file = entry.file();
            if (!file.exists()) {
                removeIndexedEntry(file.getAbsolutePath());
                continue;
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (!config.contains("death")) {
                indexFile(file, config);
                continue;
            }
            if (isDeathSnapshotExpired(config, now) && clearDeathSnapshot(file, config)) {
                removed++;
            }
        }

        return removed;
    }

    private InventorySnapshot buildSnapshot(FileConfiguration config) {
        String name = config.getString("player.name", "Unknown");
        long updatedAt = config.getLong("snapshot.updated-at", 0L);
        UUID uuid = parseUuid(config.getString("player.uuid"));

        ItemStack[] storage = normalizeArray(readItemList(config, "snapshot.storage"), STORAGE_SIZE);
        ItemStack[] armor = normalizeArray(readItemList(config, "snapshot.armor"), ARMOR_SIZE);
        ItemStack offhand = cloneItem(config.getItemStack("snapshot.offhand"));
        boolean hasMainHandData = config.contains("snapshot.mainhand");
        ItemStack mainHand = cloneItem(config.getItemStack("snapshot.mainhand"));

        return new InventorySnapshot(uuid, name, updatedAt, storage, armor, offhand, mainHand, hasMainHandData);
    }

    private DeathSnapshot buildDeathSnapshot(FileConfiguration config) {
        String name = config.getString("player.name", "Unknown");
        long updatedAt = config.getLong("death.updated-at", 0L);
        UUID uuid = parseUuid(config.getString("player.uuid"));
        String cause = config.getString("death.cause", "Unknown");

        ItemStack[] storage = normalizeArray(readItemList(config, "death.storage"), STORAGE_SIZE);
        ItemStack[] armor = normalizeArray(readItemList(config, "death.armor"), ARMOR_SIZE);
        ItemStack offhand = cloneItem(config.getItemStack("death.offhand"));
        int xpLevel = config.getInt("death.xp.level", 0);
        int xpTotal = config.getInt("death.xp.total", 0);
        float xpProgress = (float) config.getDouble("death.xp.progress", 0.0D);

        return new DeathSnapshot(uuid, name, updatedAt, cause, storage, armor, offhand, xpLevel, xpTotal, xpProgress);
    }

    private List<ItemStack> readItemList(FileConfiguration config, String path) {
        List<?> raw = config.getList(path);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }

        List<ItemStack> items = new ArrayList<>(raw.size());
        for (Object entry : raw) {
            items.add(entry instanceof ItemStack item ? cloneItem(item) : null);
        }
        return items;
    }

    private File[] getSnapshotFiles() {
        if (!dataFolder.exists()) {
            return new File[0];
        }

        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        return files == null ? new File[0] : files;
    }

    private File resolveFile(UUID uuid) {
        IndexedSnapshotFile indexedFile = indexedFilesByUuid.get(uuid);
        if (indexedFile != null) {
            return indexedFile.file();
        }
        return new File(dataFolder, uuid + ".yml");
    }

    private boolean hasAnyItems(ItemStack[] storage, ItemStack[] armor, ItemStack offhand, ItemStack mainHand) {
        ItemStack[] safeStorage = storage == null ? new ItemStack[0] : storage;
        ItemStack[] safeArmor = armor == null ? new ItemStack[0] : armor;
        return Arrays.stream(safeStorage).anyMatch(this::isValidItem)
                || Arrays.stream(safeArmor).anyMatch(this::isValidItem)
                || isValidItem(offhand)
                || isValidItem(mainHand);
    }

    private boolean isValidItem(ItemStack item) {
        return item != null && item.getType().isItem() && item.getType() != Material.AIR && item.getAmount() > 0;
    }

    private boolean isDeathSnapshotExpired(FileConfiguration config, long now) {
        long updatedAt = config.getLong("death.updated-at", 0L);
        long maxAgeMillis = Math.max(1L, plugin.getMainConfigManager().getInventoryDeathSnapshotMaxAgeMinutes())
                * 60_000L;
        return updatedAt <= 0 || now - updatedAt > maxAgeMillis;
    }

    private boolean clearDeathSnapshot(File file, FileConfiguration config) {
        config.set("death", null);
        return persistAndReindex(file, config, "expired death snapshot in " + file.getName());
    }

    private boolean persistAndReindex(File file, FileConfiguration config, String operationLabel) {
        try {
            if (!config.contains("snapshot") && !config.contains("death")) {
                removeIndexedEntry(file.getAbsolutePath());
                if (file.exists() && !file.delete()) {
                    plugin.getLogger().warning("Could not delete empty snapshot file " + file.getName() + ".");
                    config.save(file);
                    indexFile(file, config);
                }
                return true;
            }

            config.save(file);
            indexFile(file, config);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not persist " + operationLabel + ": " + exception.getMessage());
            return false;
        }
    }

    private void indexFile(File file, FileConfiguration config) {
        String absolutePath = file.getAbsolutePath();
        removeIndexedEntry(absolutePath);

        boolean hasSnapshot = config.contains("snapshot");
        boolean hasDeath = config.contains("death");
        if (!hasSnapshot && !hasDeath) {
            return;
        }

        UUID uuid = parseUuid(config.getString("player.uuid"));
        if (uuid == null) {
            uuid = parseUuid(stripExtension(file.getName()));
        }

        String playerName = config.getString("player.name");
        String lowerName = normalizeName(playerName);
        IndexedSnapshotFile indexedFile = new IndexedSnapshotFile(file, uuid, playerName, lowerName, hasSnapshot,
                hasDeath);

        indexedFilesByPath.put(absolutePath, indexedFile);
        if (uuid != null) {
            indexedFilesByUuid.put(uuid, indexedFile);
        }
        if (hasSnapshot && lowerName != null) {
            snapshotEntriesByName.put(lowerName, indexedFile);
        }
        if (hasDeath && lowerName != null) {
            deathEntriesByName.put(lowerName, indexedFile);
        }
    }

    private void removeIndexedEntry(String absolutePath) {
        IndexedSnapshotFile previous = indexedFilesByPath.remove(absolutePath);
        if (previous == null) {
            return;
        }

        if (previous.uuid() != null) {
            indexedFilesByUuid.remove(previous.uuid(), previous);
        }
        if (previous.hasSnapshot() && previous.lowerName() != null) {
            snapshotEntriesByName.remove(previous.lowerName(), previous);
        }
        if (previous.hasDeath() && previous.lowerName() != null) {
            deathEntriesByName.remove(previous.lowerName(), previous);
        }
    }

    private String normalizeName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return null;
        }
        return playerName.toLowerCase(Locale.ROOT);
    }

    private String stripExtension(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex < 0 ? fileName : fileName.substring(0, extensionIndex);
    }

    private UUID parseUuid(String rawUuid) {
        if (rawUuid == null || rawUuid.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(rawUuid);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private ItemStack[] normalizeArray(List<ItemStack> items, int expectedSize) {
        ItemStack[] normalized = new ItemStack[expectedSize];
        if (items == null || items.isEmpty()) {
            return normalized;
        }

        int limit = Math.min(expectedSize, items.size());
        for (int index = 0; index < limit; index++) {
            normalized[index] = cloneItem(items.get(index));
        }
        return normalized;
    }

    private ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private ItemStack[] cloneArray(ItemStack[] source, int expectedSize) {
        ItemStack[] cloned = new ItemStack[expectedSize];
        if (source == null || source.length == 0) {
            return cloned;
        }

        int limit = Math.min(expectedSize, source.length);
        for (int index = 0; index < limit; index++) {
            cloned[index] = cloneItem(source[index]);
        }
        return cloned;
    }

    private record IndexedSnapshotFile(File file,
            UUID uuid,
            String playerName,
            String lowerName,
            boolean hasSnapshot,
            boolean hasDeath) {
    }

    public record InventorySnapshot(UUID uuid,
            String playerName,
            long updatedAt,
            ItemStack[] storage,
            ItemStack[] armor,
            ItemStack offhand,
            ItemStack mainHand,
            boolean hasMainHandData) {
    }

    public record DeathSnapshot(UUID uuid,
            String playerName,
            long updatedAt,
            String deathCause,
            ItemStack[] storage,
            ItemStack[] armor,
            ItemStack offhand,
            int xpLevel,
            int xpTotal,
            float xpProgress) {
    }
}
