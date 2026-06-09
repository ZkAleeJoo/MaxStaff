package org.zkaleejoo.utils;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InspectionInventoryBuilder {

    private static final int INVENTORY_SIZE = 45;
    private static final int STORAGE_SIZE = 36;
    private static final int DEFAULT_ARMOR_START_SLOT = 36;
    private static final int DEFAULT_OFFHAND_SLOT = 40;
    private static final int DEFAULT_MAINHAND_SLOT = 41;
    private static final int DEFAULT_STATUS_SLOT = 44;
    private static final String PLACEHOLDER_KEY = "inspection_placeholder";
    private static final Set<String> WARNED_SLOT_KEYS = ConcurrentHashMap.newKeySet();

    private InspectionInventoryBuilder() {
    }

    public static int getInventorySize() {
        return INVENTORY_SIZE;
    }

    public static Inventory createOnlineInspection(String menuType, Player target, String title,
            MainConfigManager config) {
        return createOnlineInspection(menuType, target, title, config, false);
    }

    public static Inventory createOnlineInspection(String menuType, Player target, String title,
            MainConfigManager config, boolean editable) {
        MaxStaffHolder holder = new MaxStaffHolder(menuType, target.getName());
        holder.setData("targetUuid", target.getUniqueId());
        holder.setData("editable", editable);
        Inventory inventory = Bukkit.createInventory(
                holder,
                INVENTORY_SIZE,
                LegacyComponentSerializer.legacySection().deserialize(MessageUtils.getColoredMessage(title)));
        holder.setInventory(inventory);

        fillStorage(inventory, target.getInventory().getStorageContents());
        fillArmor(inventory, target.getInventory().getArmorContents(), config, true);
        setEditableItemSlot(inventory, getOffhandSlot(config, inventory), target.getInventory().getItemInOffHand(),
                config.getInspectOffhandLabel(), config, "invsee-inspection.layout.offhand-slot", true);
        setEditableItemSlot(inventory, getMainhandSlot(config, inventory), target.getInventory().getItemInMainHand(),
                config.getInspectMainhandLabel(), config, "invsee-inspection.layout.mainhand-slot", true);
        setStatusItem(inventory, target, config);
        return inventory;
    }

    public static void fillStorage(Inventory inventory, ItemStack[] storage) {
        for (int slot = 0; slot < Math.min(storage.length, 36); slot++) {
            ItemStack item = storage[slot];
            if (isValidItem(item)) {
                inventory.setItem(slot, item.clone());
            }
        }
    }

    public static void fillArmor(Inventory inventory, ItemStack[] armor, MainConfigManager config) {
        fillArmor(inventory, armor, config, true);
    }

    public static void fillArmor(Inventory inventory, ItemStack[] armor, MainConfigManager config,
            boolean showPlaceholders) {
        int armorStartSlot = getArmorStartSlot(config, inventory);
        for (int index = 0; index < Math.min(armor.length, 4); index++) {
            setEditableItemSlot(
                    inventory,
                    armorStartSlot + index,
                    armor[index],
                    armorSlotName(index, config),
                    config,
                    "invsee-inspection.layout.armor-start-slot",
                    showPlaceholders);
        }
    }

    public static void setEditableItemSlot(Inventory inventory,
            int slot,
            ItemStack item,
            String label,
            MainConfigManager config,
            String configPath,
            boolean showPlaceholder) {
        if (showPlaceholder) {
            setItemOrPlaceholder(inventory, slot, item, label, config, configPath);
            return;
        }

        if (!validateSlot(slot, inventory, configPath)) {
            return;
        }
        if (isValidItem(item)) {
            inventory.setItem(slot, item.clone());
        }
    }

    @SuppressWarnings("deprecation")
    public static void setItemOrPlaceholder(Inventory inventory,
            int slot,
            ItemStack item,
            String label,
            MainConfigManager config,
            String configPath) {
        if (!validateSlot(slot, inventory, configPath)) {
            return;
        }
        if (isValidItem(item)) {
            inventory.setItem(slot, item.clone());
            return;
        }

        ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtils.getColoredMessage(label + " " + config.getInspectNoItemSuffix()));
            meta.getPersistentDataContainer().set(getPlaceholderKey(), PersistentDataType.BYTE, (byte) 1);
            placeholder.setItemMeta(meta);
        }
        inventory.setItem(slot, placeholder);
    }

    @SuppressWarnings("null")
    public static boolean isInspectionPlaceholder(ItemStack item) {
        if (!isValidItem(item) || !item.hasItemMeta()) {
            return false;
        }

        return item.getItemMeta().getPersistentDataContainer().has(getPlaceholderKey(), PersistentDataType.BYTE);
    }

    @SuppressWarnings("deprecation")
    public static void setStatusItem(Inventory inventory, Player target, MainConfigManager config) {
        int statusSlot = getStatusSlot(config, inventory);
        if (!validateSlot(statusSlot, inventory, "invsee-inspection.layout.status-slot")) {
            return;
        }

        ItemStack item = new ItemStack(config.getInspectStatusMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            inventory.setItem(statusSlot, item);
            return;
        }

        meta.setDisplayName(MessageUtils.getColoredMessage(
                config.getInspectStatusTitle().replace("{player}", target.getName())));
        var location = target.getLocation();
        List<String> lore = new ArrayList<>();
        for (String line : config.getInspectStatusLore()) {
            lore.add(MessageUtils.getColoredMessage(line
                    .replace("{player}", target.getName())
                    .replace("{health}", formatHealth(target.getHealth()))
                    .replace("{food}", String.valueOf(target.getFoodLevel()))
                    .replace("{level}", String.valueOf(target.getLevel()))
                    .replace("{gamemode}", target.getGameMode().name())
                    .replace("{world}", target.getWorld().getName())
                    .replace("{x}", location != null ? String.valueOf(location.getBlockX()) : "?")
                    .replace("{y}", location != null ? String.valueOf(location.getBlockY()) : "?")
                    .replace("{z}", location != null ? String.valueOf(location.getBlockZ()) : "?")));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        inventory.setItem(statusSlot, item);
    }

    public static int getArmorStartSlot(MainConfigManager config, Inventory inventory) {
        int configured = config.getInspectArmorStartSlot();
        if (configured >= STORAGE_SIZE && configured + 3 < inventory.getSize()) {
            return configured;
        }
        warnSlotFallback("invsee-inspection.layout.armor-start-slot", configured, DEFAULT_ARMOR_START_SLOT,
                "Armor slots must be outside player storage slots 0-" + (STORAGE_SIZE - 1)
                        + " and need 4 consecutive slots.");
        return DEFAULT_ARMOR_START_SLOT;
    }

    public static int getOffhandSlot(MainConfigManager config, Inventory inventory) {
        int armorStart = getArmorStartSlot(config, inventory);
        return resolveSpecialSlot(config.getInspectOffhandSlot(), DEFAULT_OFFHAND_SLOT,
                "invsee-inspection.layout.offhand-slot", inventory, armorStart, armorStart + 3);
    }

    public static int getMainhandSlot(MainConfigManager config, Inventory inventory) {
        int armorStart = getArmorStartSlot(config, inventory);
        int offhandSlot = getOffhandSlot(config, inventory);
        return resolveSpecialSlot(config.getInspectMainhandSlot(), DEFAULT_MAINHAND_SLOT,
                "invsee-inspection.layout.mainhand-slot", inventory, armorStart, armorStart + 3, offhandSlot,
                offhandSlot);
    }

    public static int getStatusSlot(MainConfigManager config, Inventory inventory) {
        int armorStart = getArmorStartSlot(config, inventory);
        int offhandSlot = getOffhandSlot(config, inventory);
        int mainhandSlot = getMainhandSlot(config, inventory);
        return resolveSpecialSlot(config.getInspectStatusSlot(), DEFAULT_STATUS_SLOT,
                "invsee-inspection.layout.status-slot", inventory, armorStart, armorStart + 3, offhandSlot,
                offhandSlot, mainhandSlot, mainhandSlot);
    }

    private static int resolveSpecialSlot(int configured,
            int fallback,
            String configPath,
            Inventory inventory,
            int... reservedRanges) {
        if (configured >= STORAGE_SIZE && configured < inventory.getSize()
                && !isReserved(configured, reservedRanges)) {
            return configured;
        }

        warnSlotFallback(configPath, configured, fallback,
                "Special inspection slots must be outside player storage slots 0-" + (STORAGE_SIZE - 1)
                        + " and cannot overlap armor/offhand/mainhand slots.");
        return fallback;
    }

    private static boolean isReserved(int slot, int... reservedRanges) {
        for (int index = 0; index + 1 < reservedRanges.length; index += 2) {
            if (slot >= reservedRanges[index] && slot <= reservedRanges[index + 1]) {
                return true;
            }
        }
        return false;
    }

    private static void warnSlotFallback(String configPath, int configured, int fallback, String reason) {
        String warningKey = configPath + ":" + configured + ":fallback";
        if (WARNED_SLOT_KEYS.add(warningKey)) {
            Bukkit.getLogger().warning("Invalid slot in '" + configPath + "' = " + configured + ". " + reason
                    + " Falling back to " + fallback + ".");
        }
    }

    private static NamespacedKey getPlaceholderKey() {
        return new NamespacedKey(JavaPlugin.getPlugin(MaxStaff.class), PLACEHOLDER_KEY);
    }

    private static String armorSlotName(int index, MainConfigManager config) {
        return switch (index) {
            case 0 -> config.getInspectArmorBootsLabel();
            case 1 -> config.getInspectArmorLeggingsLabel();
            case 2 -> config.getInspectArmorChestplateLabel();
            case 3 -> config.getInspectArmorHelmetLabel();
            default -> "&bArmor";
        };
    }

    private static String formatHealth(double health) {
        return String.format(java.util.Locale.ROOT, "%.1f", health);
    }

    private static boolean isValidItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    private static boolean validateSlot(int slot, Inventory inventory, String configPath) {
        if (slot >= 0 && slot < inventory.getSize()) {
            return true;
        }

        String warningKey = configPath + ":" + slot;
        if (WARNED_SLOT_KEYS.add(warningKey)) {
            Bukkit.getLogger().warning("Invalid slot in '" + configPath + "' = " + slot
                    + ". Valid range: 0-" + (inventory.getSize() - 1) + ". This item will not be shown.");
        }
        return false;
    }
}
