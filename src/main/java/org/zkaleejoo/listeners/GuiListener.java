package org.zkaleejoo.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MaxStaffHolder;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.commands.CommandContextUtil;
import org.zkaleejoo.utils.InspectionInventoryBuilder;
import java.util.Objects;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class GuiListener implements Listener {

    private final MaxStaff plugin;
    private final NamespacedKey reasonKey;
    private final NamespacedKey durationKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey reviveTargetKey;
    private final NamespacedKey targetPlayerKey;

    public GuiListener(MaxStaff plugin) {
        this.plugin = plugin;
        this.reasonKey = new NamespacedKey(plugin, "reason_id");
        this.durationKey = new NamespacedKey(plugin, "duration");
        this.actionKey = new NamespacedKey(plugin, "gui_action");
        this.reviveTargetKey = new NamespacedKey(plugin, "revive_target");
        this.targetPlayerKey = new NamespacedKey(plugin, "target_player");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (!(event.getInventory().getHolder() instanceof MaxStaffHolder holder))
            return;

        if (isEditableOnlineInspection(holder, player)) {
            handleEditableInspectionClick(event, player, holder);
            return;
        }

        if (event.getClickedInventory() == null)
            return;

        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR)
            return;

        MainConfigManager config = plugin.getMainConfigManager();
        if (item.getType() == config.getBorderMaterial())
            return;

        handleMenuAction(player, item, holder, config);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof MaxStaffHolder holder)) {
            return;
        }
        if (!isEditableOnlineInspection(holder, player)) {
            event.setCancelled(true);
            return;
        }

        Player target = getEditableInspectionTarget(holder);
        if (target == null) {
            event.setCancelled(true);
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix()
                    + plugin.getMainConfigManager().getGuiPlayerOffline()));
            return;
        }

        int topSize = event.getInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= topSize || !isEditableInspectionSlot(rawSlot, event.getInventory())) {
                event.setCancelled(true);
                return;
            }
        }

        scheduleInspectionSync(holder, event.getInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof MaxStaffHolder holder)) {
            return;
        }
        if (isEditableOnlineInspection(holder, player)) {
            ItemStack cursorRemainder = returnCursorToInspectionInventory(player, event.getInventory());
            syncEditableInspectionInventory(holder, event.getInventory());
            returnCursorRemainderToTarget(holder, cursorRemainder);
        }
    }

    private void handleEditableInspectionClick(InventoryClickEvent event, Player player, MaxStaffHolder holder) {
        Player target = getEditableInspectionTarget(holder);
        if (target == null) {
            event.setCancelled(true);
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix()
                    + plugin.getMainConfigManager().getGuiPlayerOffline()));
            return;
        }

        int rawSlot = event.getRawSlot();
        int topSize = event.getInventory().getSize();
        if (event.getClickedInventory() == null || rawSlot < 0) {
            event.setCancelled(true);
            return;
        }
        if (isBlockedInspectionClick(event)) {
            event.setCancelled(true);
            return;
        }
        if (rawSlot < topSize && !isEditableInspectionSlot(rawSlot, event.getInventory())) {
            event.setCancelled(true);
            return;
        }
        if (rawSlot < topSize && InspectionInventoryBuilder.isInspectionPlaceholder(event.getCurrentItem())) {
            handleInspectionPlaceholderClick(event, holder);
            return;
        }

        scheduleInspectionSync(holder, event.getInventory());
    }

    private void handleInspectionPlaceholderClick(InventoryClickEvent event, MaxStaffHolder holder) {
        event.setCancelled(true);

        ItemStack cursor = cloneInventoryItem(event.getCursor());
        if (cursor == null) {
            return;
        }

        event.getInventory().setItem(event.getRawSlot(), cursor);
        event.getWhoClicked().setItemOnCursor(null);
        scheduleInspectionSync(holder, event.getInventory());
    }

    private boolean isEditableOnlineInspection(MaxStaffHolder holder, Player viewer) {
        String menuType = holder.getMenuType();
        if (!"INVSEE_ONLINE".equals(menuType) && !"INSPECT_ONLINE".equals(menuType)) {
            return false;
        }

        Object editable = holder.getData("editable");
        if (!Boolean.TRUE.equals(editable)) {
            return false;
        }

        return switch (menuType) {
            case "INVSEE_ONLINE" -> viewer.hasPermission("maxstaff.invsee");
            case "INSPECT_ONLINE" -> viewer.hasPermission("maxstaff.revive");
            default -> false;
        };
    }

    private Player getEditableInspectionTarget(MaxStaffHolder holder) {
        Object rawUuid = holder.getData("targetUuid");
        if (rawUuid instanceof UUID uuid) {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null && target.isOnline()) {
                return target;
            }
        }

        return Bukkit.getPlayerExact(holder.getTargetName());
    }

    private boolean isEditableInspectionSlot(int slot, Inventory inventory) {
        MainConfigManager config = plugin.getMainConfigManager();
        if (slot >= 0 && slot < 36) {
            return true;
        }
        int armorStartSlot = InspectionInventoryBuilder.getArmorStartSlot(config, inventory);
        if (slot >= armorStartSlot && slot < armorStartSlot + 4) {
            return true;
        }
        return slot == InspectionInventoryBuilder.getOffhandSlot(config, inventory);
    }

    @SuppressWarnings("removal")
    private boolean isBlockedInspectionClick(InventoryClickEvent event) {
        ClickType click = event.getClick();
        if (click == ClickType.NUMBER_KEY
                || click == ClickType.SWAP_OFFHAND
                || click == ClickType.DOUBLE_CLICK
                || click == ClickType.DROP
                || click == ClickType.CONTROL_DROP
                || click == ClickType.MIDDLE
                || click == ClickType.CREATIVE
                || click == ClickType.UNKNOWN) {
            return true;
        }

        InventoryAction action = event.getAction();
        return action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || action == InventoryAction.HOTBAR_SWAP
                || action == InventoryAction.HOTBAR_MOVE_AND_READD
                || action == InventoryAction.COLLECT_TO_CURSOR
                || action == InventoryAction.DROP_ALL_CURSOR
                || action == InventoryAction.DROP_ONE_CURSOR
                || action == InventoryAction.DROP_ALL_SLOT
                || action == InventoryAction.DROP_ONE_SLOT
                || action == InventoryAction.CLONE_STACK
                || action == InventoryAction.UNKNOWN;
    }

    private void scheduleInspectionSync(MaxStaffHolder holder, Inventory inventory) {
        Bukkit.getScheduler().runTask(plugin, () -> syncEditableInspectionInventory(holder, inventory));
    }

    private void syncEditableInspectionInventory(MaxStaffHolder holder, Inventory inventory) {
        Player target = getEditableInspectionTarget(holder);
        if (target == null) {
            return;
        }

        MainConfigManager config = plugin.getMainConfigManager();
        PlayerInventory targetInventory = target.getInventory();

        ItemStack[] storage = new ItemStack[36];
        for (int slot = 0; slot < storage.length; slot++) {
            storage[slot] = cloneInventoryItem(inventory.getItem(slot));
        }
        targetInventory.setStorageContents(storage);

        ItemStack[] armor = new ItemStack[4];
        int armorStartSlot = InspectionInventoryBuilder.getArmorStartSlot(config, inventory);
        for (int index = 0; index < armor.length; index++) {
            armor[index] = cloneInventoryItem(inventory.getItem(armorStartSlot + index));
        }
        targetInventory.setArmorContents(armor);

        targetInventory.setItemInOffHand(
                cloneInventoryItem(inventory.getItem(InspectionInventoryBuilder.getOffhandSlot(config, inventory))));
        target.updateInventory();
    }

    private ItemStack returnCursorToInspectionInventory(Player player, Inventory inventory) {
        ItemStack cursor = cloneInventoryItem(player.getItemOnCursor());
        if (cursor == null) {
            return null;
        }

        ItemStack remainder = cursor.clone();
        for (int slot = 0; slot < inventory.getSize() && remainder != null; slot++) {
            if (!isEditableInspectionSlot(slot, inventory)) {
                continue;
            }

            ItemStack existing = inventory.getItem(slot);
            if (existing == null || existing.getType() == Material.AIR) {
                inventory.setItem(slot, remainder);
                remainder = null;
                break;
            }

            if (!existing.isSimilar(remainder) || existing.getAmount() >= existing.getMaxStackSize()) {
                continue;
            }

            int transfer = Math.min(remainder.getAmount(), existing.getMaxStackSize() - existing.getAmount());
            existing.setAmount(existing.getAmount() + transfer);
            remainder.setAmount(remainder.getAmount() - transfer);
            if (remainder.getAmount() <= 0) {
                remainder = null;
            }
        }

        player.setItemOnCursor(null);
        return remainder;
    }

    private void returnCursorRemainderToTarget(MaxStaffHolder holder, ItemStack remainder) {
        if (remainder == null) {
            return;
        }

        Player target = getEditableInspectionTarget(holder);
        if (target == null) {
            return;
        }

        for (ItemStack leftover : target.getInventory().addItem(remainder).values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), leftover);
        }
        target.updateInventory();
    }

    private ItemStack cloneInventoryItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || InspectionInventoryBuilder.isInspectionPlaceholder(item)) {
            return null;
        }
        return item.clone();
    }

    private void handleMenuAction(Player player, ItemStack item, MaxStaffHolder holder, MainConfigManager config) {
        String menuType = holder.getMenuType();
        String targetName = holder.getTargetName();

        switch (menuType) {
            case "INFO" -> handleInfoMenu(player, item, targetName, config);
            case "PLAYERS" -> handlePlayersMenu(player, item, config);
            case "XRAY" -> handleXrayMenu(player, item, config);
            case "SANCTIONS" -> handleSanctionMenu(player, item, targetName, config);
            case "REASONS" -> handleReasonsMenu(player, item, holder, config);
            case "HISTORY" -> handleHistoryMenu(player, item, targetName, config);
            case "GAMEMODE" -> handleGameModeMenu(player, item, config);
            case "DETAILED_HISTORY" -> handleDetailedHistoryMenu(player, item, targetName, config);
            case "CONFIRM" -> handleConfirmMenu(player, item, holder, config);
            case "REVIVE" -> handleReviveMenu(player, item, config);
            case "PERMISSIONS" -> handlePermissionsMenu(player, item, holder, config);
            case "ACTIVE_PUNISHMENTS" -> handleActivePunishmentsMenu(player, item, holder, config);
        }
    }

    private void handleInfoMenu(Player player, ItemStack item, String targetName, MainConfigManager config) {
        Material mat = item.getType();

        if (mat == config.getGuiInfoActionMat()) {
            plugin.getGuiManager().openSanctionMenu(player, targetName);
        } else if (mat == config.getGuiInfoHistoryMat()) {
            if (checkPerm(player, "maxstaff.history"))
                plugin.getGuiManager().openHistoryMenu(player, targetName);
        } else if (mat == config.getGuiInfoAltsMat()) {
            plugin.getGuiManager().openAltsMenu(player, targetName);
        } else if (mat == config.getGuiInfoPermissionsMat()) {
            plugin.getGuiManager().openPermissionsMenu(player, targetName, 0);
        } else if (mat == config.getGuiInfoInvMat()) {
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                clickSound(player, Sound.BLOCK_CHEST_OPEN);
                player.openInventory(Objects.requireNonNull(InspectionInventoryBuilder.createOnlineInspection(
                        "INSPECT_ONLINE",
                        target,
                        config.getInvseeInspectionOnlineTitle().replace("{player}", target.getName()),
                        config,
                        player.hasPermission("maxstaff.revive"))));
                return;
            } else {
                player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getGuiPlayerOffline()));
            }
        }
        clickSound(player, Sound.UI_BUTTON_CLICK);
    }

    private void handlePlayersMenu(Player player, ItemStack item, MainConfigManager config) {
        if (!item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        String action = meta.getPersistentDataContainer().get(Objects.requireNonNull(actionKey),
                Objects.requireNonNull(PersistentDataType.STRING));

        if ("random_tp_player".equals(action)) {
            List<? extends Player> candidates = Bukkit.getOnlinePlayers().stream()
                    .filter(target -> target != null && !target.getUniqueId().equals(player.getUniqueId()))
                    .toList();

            if (candidates.isEmpty()) {
                clickSound(player, Sound.BLOCK_NOTE_BLOCK_BASS);
                player.sendMessage(
                        MessageUtils.getColoredMessage(config.getPrefix() + config.getMsgRandomTpNoTargets()));
                return;
            }

            Player target = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            if (target == null)
                return;
            player.teleport(target);
            clickSound(player, Sound.ENTITY_ENDERMAN_TELEPORT);
            player.sendMessage(MessageUtils
                    .getColoredMessage(
                            config.getPrefix() + config.getMsgRandomTp().replace("{player}", target.getName())));
            player.closeInventory();
            return;
        }

        if ("tp_player".equals(action) && item.getType() == Material.PLAYER_HEAD) {
            String targetUuidRaw = meta.getPersistentDataContainer().get(Objects.requireNonNull(targetPlayerKey),
                    Objects.requireNonNull(PersistentDataType.STRING));
            Player target = null;
            if (targetUuidRaw != null) {
                try {
                    target = Bukkit.getPlayer(UUID.fromString(targetUuidRaw));
                } catch (IllegalArgumentException ignored) {
                    target = null;
                }
            }
            if (target == null) {
                String cleanName = meta.hasDisplayName()
                        ? PlainTextComponentSerializer.plainText().serialize(meta.displayName())
                        : "";
                target = Bukkit.getPlayer(cleanName);
            }

            if (target != null) {
                clickSound(player, Sound.ENTITY_ENDERMAN_TELEPORT);
                player.teleport(target);
                player.sendMessage(
                        MessageUtils.getColoredMessage(config.getMsgTeleport().replace("{player}", target.getName())));
            }
            player.closeInventory();
        }
    }

    private void handleXrayMenu(Player player, ItemStack item, MainConfigManager config) {
        if (!item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        String action = meta.getPersistentDataContainer().get(Objects.requireNonNull(actionKey),
                Objects.requireNonNull(PersistentDataType.STRING));
        if (!"xray_tp_player".equals(action) || item.getType() != Material.PLAYER_HEAD) {
            return;
        }

        String targetUuidRaw = meta.getPersistentDataContainer().get(Objects.requireNonNull(targetPlayerKey),
                Objects.requireNonNull(PersistentDataType.STRING));
        Player target = null;
        if (targetUuidRaw != null) {
            try {
                target = Bukkit.getPlayer(UUID.fromString(targetUuidRaw));
            } catch (IllegalArgumentException ignored) {
                target = null;
            }
        }

        if (target == null) {
            clickSound(player, Sound.BLOCK_NOTE_BLOCK_BASS);
            player.sendMessage(
                    MessageUtils.getColoredMessage(config.getPrefix() + config.getGuiXrayTargetOfflineMessage()));
            return;
        }

        clickSound(player, Sound.ENTITY_ENDERMAN_TELEPORT);
        player.teleport(target);
        player.sendMessage(MessageUtils.getColoredMessage(
                config.getPrefix() + config.getGuiXrayTeleportMessage().replace("{player}", target.getName())));
        player.closeInventory();
    }

    private void handleSanctionMenu(Player player, ItemStack item, String targetName, MainConfigManager config) {
        Material mat = item.getType();
        clickSound(player, Sound.UI_BUTTON_CLICK);

        if (mat == config.getNavBackMat()) {
            Optional.ofNullable(Bukkit.getPlayer(targetName))
                    .ifPresent(target -> plugin.getGuiManager().openUserInfoMenu(player, target));
        } else if (mat == Material.IRON_SWORD) {
            if (checkPerm(player, "maxstaff.punish.ban"))
                plugin.getGuiManager().openReasonsMenu(player, targetName, "BAN", 0);
        } else if (mat == Material.PAPER) {
            if (checkPerm(player, "maxstaff.punish.mute"))
                plugin.getGuiManager().openReasonsMenu(player, targetName, "MUTE", 0);
        } else if (mat == Material.FEATHER) {
            if (checkPerm(player, "maxstaff.punish.kick"))
                plugin.getGuiManager().openReasonsMenu(player, targetName, "KICK", 0);
        }
    }

    private void handleReasonsMenu(Player player, ItemStack item, MaxStaffHolder holder, MainConfigManager config) {
        String targetName = holder.getTargetName();
        String type = (String) holder.getData("type");
        int page = (int) holder.getData("page");

        if (!item.hasItemMeta())
            return;
        ItemMeta meta = item.getItemMeta();

        String action = meta.getPersistentDataContainer().get(Objects.requireNonNull(actionKey),
                Objects.requireNonNull(PersistentDataType.STRING));
        if (action == null)
            return;

        switch (action) {
            case "back_sanction" -> {
                clickSound(player, Sound.UI_BUTTON_CLICK);
                plugin.getGuiManager().openSanctionMenu(player, targetName);
            }
            case "next_page" -> {
                plugin.getGuiManager().openReasonsMenu(player, targetName, type, page + 1);
                clickSound(player, Sound.UI_BUTTON_CLICK);
            }
            case "prev_page" -> {
                plugin.getGuiManager().openReasonsMenu(player, targetName, type, page - 1);
                clickSound(player, Sound.UI_BUTTON_CLICK);
            }
            case "apply_punish" -> {
                String reasonId = meta.getPersistentDataContainer().get(Objects.requireNonNull(reasonKey),
                        Objects.requireNonNull(PersistentDataType.STRING));
                String duration = meta.getPersistentDataContainer().get(Objects.requireNonNull(durationKey),
                        Objects.requireNonNull(PersistentDataType.STRING));
                if (reasonId == null || duration == null) {
                    return;
                }

                if (!checkPerm(player, "maxstaff.punish." + type.toLowerCase())) {
                    player.closeInventory();
                    return;
                }
                if (!validateDurationLimit(player, type.toLowerCase(), duration, config)) {
                    clickSound(player, Sound.ENTITY_VILLAGER_NO);
                    return;
                }

                clickSound(player, Sound.UI_BUTTON_CLICK);
                plugin.getGuiManager().openConfirmMenu(player, targetName, type, reasonId, duration, page);
            }
            default -> {
            }
        }
    }

    private void handleConfirmMenu(Player player, ItemStack item, MaxStaffHolder holder, MainConfigManager config) {
        if (!item.hasItemMeta())
            return;
        ItemMeta meta = item.getItemMeta();

        String action = meta.getPersistentDataContainer().get(Objects.requireNonNull(actionKey),
                Objects.requireNonNull(PersistentDataType.STRING));
        if (action == null)
            return;

        String targetName = holder.getTargetName();
        String type = (String) holder.getData("type");
        String reasonId = (String) holder.getData("reasonId");
        String duration = (String) holder.getData("duration");
        int page = (int) holder.getData("page");

        if ("confirm_yes".equals(action)) {
            if (!checkPerm(player, "maxstaff.punish." + type.toLowerCase())) {
                player.closeInventory();
                return;
            }
            if (!validateDurationLimit(player, type.toLowerCase(), duration, config)) {
                clickSound(player, Sound.ENTITY_VILLAGER_NO);
                player.closeInventory();
                return;
            }
            String reasonName = config.getReasonName(type, reasonId);
            executePunishment(player, targetName, type, reasonName, duration);
            clickSound(player, Sound.UI_BUTTON_CLICK);
            player.closeInventory();
        } else if ("confirm_no".equals(action)) {
            clickSound(player, Sound.ENTITY_VILLAGER_NO);
            plugin.getGuiManager().openReasonsMenu(player, targetName, type, page);
        }
    }

    private void handleHistoryMenu(Player player, ItemStack item, String targetName, MainConfigManager config) {
        if (!item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        String action = meta.getPersistentDataContainer().get(Objects.requireNonNull(actionKey),
                Objects.requireNonNull(PersistentDataType.STRING));
        if (action == null) {
            return;
        }

        switch (action) {
            case "history_bans" -> {
                clickSound(player, Sound.UI_BUTTON_CLICK);
                plugin.getGuiManager().openDetailedHistoryMenu(player, targetName, "BAN");
            }
            case "history_mutes" -> {
                clickSound(player, Sound.UI_BUTTON_CLICK);
                plugin.getGuiManager().openDetailedHistoryMenu(player, targetName, "MUTE");
            }
            case "history_warns" -> {
                clickSound(player, Sound.UI_BUTTON_CLICK);
                plugin.getGuiManager().openDetailedHistoryMenu(player, targetName, "WARN");
            }
            case "history_kicks" -> {
                clickSound(player, Sound.UI_BUTTON_CLICK);
                plugin.getGuiManager().openDetailedHistoryMenu(player, targetName, "KICK");
            }
            case "back_info" -> {
                clickSound(player, Sound.UI_BUTTON_CLICK);
                Optional.ofNullable(Bukkit.getPlayer(targetName))
                        .ifPresent(target -> plugin.getGuiManager().openUserInfoMenu(player, target));
            }
            default -> {
            }
        }
    }

    private void handleGameModeMenu(Player player, ItemStack item, MainConfigManager config) {
        org.bukkit.GameMode mode = null;
        String modeName = "";

        if (item.getType() == config.getGuiGmSurvivalMat()) {
            mode = org.bukkit.GameMode.SURVIVAL;
            modeName = "Survival";
        } else if (item.getType() == config.getGuiGmCreativeMat()) {
            mode = org.bukkit.GameMode.CREATIVE;
            modeName = "Creative";
        } else if (item.getType() == config.getGuiGmAdventureMat()) {
            mode = org.bukkit.GameMode.ADVENTURE;
            modeName = "Adventure";
        } else if (item.getType() == config.getGuiGmSpectatorMat()) {
            mode = org.bukkit.GameMode.SPECTATOR;
            modeName = "Spectator";
        }

        if (mode != null) {
            clickSound(player, Sound.UI_BUTTON_CLICK);
            player.setGameMode(mode);
            if (plugin.getStaffManager().isInStaffMode(player)) {
                plugin.getStaffManager().updateSavedGameMode(player, mode);
            }
            player.sendMessage(MessageUtils
                    .getColoredMessage(config.getPrefix() + config.getGuiGmFeedback().replace("{mode}", modeName)));
            player.closeInventory();
        }
    }

    private void handleDetailedHistoryMenu(Player player, ItemStack item, String targetName, MainConfigManager config) {
        if (item.getType() == config.getNavBackMat()) {
            clickSound(player, Sound.UI_BUTTON_CLICK);
            plugin.getGuiManager().openHistoryMenu(player, targetName);
        }
    }

    private void handlePermissionsMenu(Player player, ItemStack item, MaxStaffHolder holder, MainConfigManager config) {
        String targetName = holder.getTargetName();
        int page = (int) holder.getData("page");

        if (!item.hasItemMeta())
            return;
        ItemMeta meta = item.getItemMeta();

        String action = meta.getPersistentDataContainer().get(Objects.requireNonNull(actionKey),
                Objects.requireNonNull(PersistentDataType.STRING));
        if (action == null)
            return;

        switch (action) {
            case "back_info" -> {
                Optional.ofNullable(Bukkit.getPlayer(targetName))
                        .ifPresent(target -> plugin.getGuiManager().openUserInfoMenu(player, target));
                clickSound(player, Sound.UI_BUTTON_CLICK);
            }
            case "next_page" -> {
                plugin.getGuiManager().openPermissionsMenu(player, targetName, page + 1);
                clickSound(player, Sound.UI_BUTTON_CLICK);
            }
            case "prev_page" -> {
                plugin.getGuiManager().openPermissionsMenu(player, targetName, page - 1);
                clickSound(player, Sound.UI_BUTTON_CLICK);
            }
            default -> {
            }
        }
    }

    private void handleActivePunishmentsMenu(Player player, ItemStack item, MaxStaffHolder holder,
            MainConfigManager config) {
        if (!item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        String action = meta.getPersistentDataContainer().get(Objects.requireNonNull(actionKey),
                Objects.requireNonNull(PersistentDataType.STRING));
        if (action == null) {
            return;
        }

        int page = holder.getData("page") instanceof Integer currentPage ? currentPage : 0;
        switch (action) {
            case "active_next_page" -> {
                plugin.getGuiManager().openActivePunishmentsMenu(player, page + 1);
                clickSound(player, Sound.UI_BUTTON_CLICK);
            }
            case "active_prev_page" -> {
                plugin.getGuiManager().openActivePunishmentsMenu(player, page - 1);
                clickSound(player, Sound.UI_BUTTON_CLICK);
            }
            default -> {
            }
        }
    }

    private void handleReviveMenu(Player player, ItemStack item, MainConfigManager config) {
        if (!item.hasItemMeta())
            return;
        ItemMeta meta = item.getItemMeta();

        String action = meta.getPersistentDataContainer().get(Objects.requireNonNull(actionKey),
                Objects.requireNonNull(PersistentDataType.STRING));
        MaxStaffHolder holder = (MaxStaffHolder) player.getOpenInventory().getTopInventory().getHolder();
        int page = holder != null && holder.getData("page") instanceof Integer ? (int) holder.getData("page") : 0;

        if ("revive_next_page".equals(action)) {
            plugin.getGuiManager().openReviveMenu(player, page + 1);
            clickSound(player, Sound.UI_BUTTON_CLICK);
            return;
        }

        if ("revive_prev_page".equals(action)) {
            plugin.getGuiManager().openReviveMenu(player, page - 1);
            clickSound(player, Sound.UI_BUTTON_CLICK);
            return;
        }

        if (!"revive_apply".equals(action) || item.getType() != Material.PLAYER_HEAD) {
            return;
        }

        String targetName = meta.getPersistentDataContainer().get(Objects.requireNonNull(reviveTargetKey),
                Objects.requireNonNull(PersistentDataType.STRING));
        if ((targetName == null || targetName.isBlank()) && meta.hasDisplayName()) {
            targetName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }
        if (targetName == null || targetName.isBlank()) {
            return;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            clickSound(player, Sound.ENTITY_VILLAGER_NO);
            player.sendMessage(MessageUtils.getColoredMessage(
                    config.getPrefix() + config.getReviveTargetOffline().replace("{player}", targetName)));
            return;
        }

        org.zkaleejoo.managers.InventorySnapshotManager.DeathSnapshot snapshot = plugin.getGuiManager()
                .restoreLatestDeathInventory(target);
        if (snapshot == null) {
            clickSound(player, Sound.ENTITY_VILLAGER_NO);
            player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getReviveNoDeaths()));
            return;
        }

        clickSound(player, Sound.ENTITY_PLAYER_LEVELUP);
        player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getReviveRestored()
                .replace("{player}", target.getName())
                .replace("{cause}", snapshot.deathCause())));
        target.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getReviveRestored()
                .replace("{player}", target.getName())
                .replace("{cause}", snapshot.deathCause())));

        plugin.getGuiManager().openReviveMenu(player, page);
    }

    private void executePunishment(Player staff, String target, String type, String reason, String duration) {
        switch (type) {
            case "BAN" -> plugin.getPunishmentManager().banPlayer(staff, target, reason, duration);
            case "MUTE" -> plugin.getPunishmentManager().mutePlayer(staff, target, reason, duration);
            case "KICK" -> plugin.getPunishmentManager().kickPlayer(staff, target, reason);
        }
    }

    private void clickSound(Player player, Sound sound) {
        org.bukkit.Location loc = player.getLocation();
        if (loc != null) {
            player.playSound(loc, Objects.requireNonNull(sound), 1.0f, 1.0f);
        }
    }

    private boolean checkPerm(Player player, String permission) {
        if (!CommandContextUtil.hasPermissionOrAdmin(player, permission)) {
            plugin.getLogger().fine("[debug-perm] checkPerm failed in GuiListener: staff=" + player.getName()
                    + ", missingNode=" + permission);
            clickSound(player, Sound.ENTITY_VILLAGER_NO);
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()));
            return false;
        }
        return true;
    }

    private boolean validateDurationLimit(Player staff, String punishmentType, String durationToken,
            MainConfigManager config) {
        if (punishmentType.equalsIgnoreCase("kick")) {
            return true;
        }
        if (config.isPunishmentDurationAllowed(staff, punishmentType, durationToken)) {
            return true;
        }

        long maxLimit = config.getPunishmentDurationLimit(staff, punishmentType);
        String maxDisplay = maxLimit == Long.MIN_VALUE
                ? config.getTimeUnitPermanent()
                : org.zkaleejoo.utils.TimeUtils.getDurationString(maxLimit, config);
        String durationDisplay = org.zkaleejoo.utils.TimeUtils
                .getDurationString(org.zkaleejoo.utils.TimeUtils.parseDuration(durationToken), config);
        String message = config.getMsgPunishmentLimitExceeded()
                .replace("{type}", punishmentType.toUpperCase())
                .replace("{duration}", durationDisplay)
                .replace("{max}", maxDisplay);
        staff.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + message));
        return false;
    }
}
