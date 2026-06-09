package org.zkaleejoo.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.BanUtils;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.MaxStaffHolder;
import org.zkaleejoo.utils.TimeUtils;
import org.zkaleejoo.listeners.AntiXrayListener.XraySuspect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.stream.Collectors;

public class GuiManager {

    private final MaxStaff plugin;
    private final NamespacedKey actionKey;
    private final NamespacedKey reviveTargetKey;
    private final NamespacedKey reasonKey;
    private final NamespacedKey durationKey;
    private final NamespacedKey targetPlayerKey;
    private final Map<UUID, BukkitTask> activePunishmentRefreshTasks = new HashMap<>();
    private ItemStack cachedBorderItem;

    public GuiManager(MaxStaff plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "gui_action");
        this.reasonKey = new NamespacedKey(plugin, "reason_id");
        this.durationKey = new NamespacedKey(plugin, "duration");
        this.reviveTargetKey = new NamespacedKey(plugin, "revive_target");
        this.targetPlayerKey = new NamespacedKey(plugin, "target_player");
        updateCachedItems();
    }

    public void updateCachedItems() {
        cachedBorderItem = createItem(plugin.getMainConfigManager().getBorderMaterial(), " ", null, "border");
    }

    private void setupBorder(Inventory inv) {
        int size = inv.getSize();

        for (int i = 0; i < 9; i++)
            inv.setItem(i, cachedBorderItem);
        for (int i = size - 9; i < size; i++)
            inv.setItem(i, cachedBorderItem);
        for (int i = 9; i < size - 9; i += 9) {
            inv.setItem(i, cachedBorderItem);
            inv.setItem(i + 8, cachedBorderItem);
        }
    }

    public void openUserInfoMenu(Player staff, Player target) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int bans = plugin.getPunishmentManager().getHistoryCount(target.getName(), "BAN");
            int mutes = plugin.getPunishmentManager().getHistoryCount(target.getName(), "MUTE");
            int kicks = plugin.getPunishmentManager().getHistoryCount(target.getName(), "KICK");
            int warns = plugin.getPunishmentManager().getHistoryCount(target.getName(), "WARN");
            int total = bans + mutes + kicks;

            long ticks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
            long hours = ticks / 72000;
            long minutes = (ticks % 72000) / 1200;
            String playtime = hours + "h " + minutes + "m";

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!staff.isOnline())
                    return;

                MainConfigManager config = plugin.getMainConfigManager();
                String title = MessageUtils
                        .getColoredMessage(config.getGuiInfoTitle().replace("{target}", target.getName()));

                MaxStaffHolder holder = new MaxStaffHolder("INFO", target.getName());
                Inventory gui = Bukkit.createInventory(holder, 45, MessageUtils.legacyToComponentNoItalic(title));
                holder.setInventory(gui);

                setupBorder(gui);

                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta headMeta = (SkullMeta) head.getItemMeta();
                if (headMeta != null) {
                    headMeta.setOwningPlayer(target);
                    headMeta.displayName(MessageUtils.legacyToComponentNoItalic(
                            config.getGuiInfoHeadName().replace("{target}", target.getName())));

                    String statusText = target.isOnline() ? config.getStatusOnline() : config.getStatusOffline();
                    java.net.InetSocketAddress addr = target.getAddress();
                    String ip = (addr != null) ? addr.getAddress().getHostAddress() : "Offline";

                    List<String> headLore = config.getGuiInfoHeadLore().stream()
                            .map(line -> line.replace("{status}", statusText)
                                    .replace("{health}", String.valueOf((int) target.getHealth()))
                                    .replace("{food}", String.valueOf(target.getFoodLevel()))
                                    .replace("{gm}", target.getGameMode().name())
                                    .replace("{ip}", ip))
                            .collect(Collectors.toList());

                    headMeta.lore(headLore.stream().map(MessageUtils::getColoredMessage)
                            .map(MessageUtils::legacyToComponentNoItalic).collect(Collectors.toList()));
                    headMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "info_head");
                    head.setItemMeta(headMeta);
                }
                gui.setItem(13, head);

                List<String> statsLore = config.getGuiInfoStatsLore().stream()
                        .map(line -> line.replace("{target}", target.getName())
                                .replace("{uuid}", target.getUniqueId().toString())
                                .replace("{playtime}", playtime)
                                .replace("{total_punishments}", String.valueOf(total)))
                        .collect(Collectors.toList());
                gui.setItem(20,
                        createItem(config.getGuiInfoStatsMat(), config.getGuiInfoStatsName(), statsLore, "info_stats"));

                List<String> historyLore = config.getGuiInfoHistoryLore().stream()
                        .map(line -> line.replace("{bans}", String.valueOf(bans))
                                .replace("{mutes}", String.valueOf(mutes))
                                .replace("{kicks}", String.valueOf(kicks))
                                .replace("{warns}", String.valueOf(warns)))
                        .collect(Collectors.toList());
                gui.setItem(21, createItem(config.getGuiInfoHistoryMat(), config.getGuiInfoHistoryName(), historyLore,
                        "open_history"));

                gui.setItem(22, createItem(config.getGuiInfoActionMat(), config.getGuiInfoActionName(),
                        config.getGuiInfoActionLore(), "open_sanction"));
                gui.setItem(23, createItem(config.getGuiInfoAltsMat(), config.getGuiInfoAltsName(),
                        config.getGuiInfoAltsLore(), "open_alts"));
                gui.setItem(24, createItem(config.getGuiInfoInvMat(), config.getGuiInfoInvName(),
                        config.getGuiInfoInvLore(), "open_inv"));
                gui.setItem(31, createItem(config.getGuiInfoPermissionsMat(), config.getGuiInfoPermissionsName(),
                        config.getGuiInfoPermissionsLore(), "open_permissions"));

                staff.openInventory(Objects.requireNonNull(gui));
            });
        });
    }

    public void openPlayersMenu(Player player) {
        MaxStaffHolder holder = new MaxStaffHolder("PLAYERS", null);

        Inventory gui = Bukkit.createInventory(holder, 54,
                MessageUtils.legacyToComponentNoItalic(plugin.getMainConfigManager().getGuiPlayersTitle()));
        holder.setInventory(gui);

        setupBorder(gui);
        for (Player target : Bukkit.getOnlinePlayers()) {
            gui.addItem(createPlayerHead(target));
        }
        gui.setItem(49, createItem(plugin.getMainConfigManager().getGuiPlayersRandomTpMat(),
                plugin.getMainConfigManager().getGuiPlayersRandomTpName(),
                plugin.getMainConfigManager().getGuiPlayersRandomTpLore(), "random_tp_player"));
        player.openInventory(Objects.requireNonNull(gui));
    }


    public void openXrayMenu(Player staff) {
        MaxStaffHolder holder = new MaxStaffHolder("XRAY", null);
        Inventory gui = Bukkit.createInventory(holder, 54,
                MessageUtils.legacyToComponentNoItalic(plugin.getMainConfigManager().getGuiXrayTitle()));
        holder.setInventory(gui);

        setupBorder(gui);

        if (plugin.getAntiXrayListener() == null || plugin.getAntiXrayListener().getSuspects().isEmpty()) {
            gui.setItem(22, createItem(plugin.getMainConfigManager().getGuiXrayEmptyMaterial(),
                    plugin.getMainConfigManager().getGuiXrayEmptyName(),
                    plugin.getMainConfigManager().getGuiXrayEmptyLore(), "xray_empty"));
        } else {
            for (XraySuspect suspect : plugin.getAntiXrayListener().getSuspects()) {
                gui.addItem(createXraySuspectHead(suspect));
            }
        }

        staff.openInventory(Objects.requireNonNull(gui));
    }

    public void openPermissionsMenu(Player staff, String targetName, int page) {
        MainConfigManager config = plugin.getMainConfigManager();
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            staff.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getGuiPlayerOffline()));
            return;
        }

        List<String> permissions = target.getEffectivePermissions().stream()
                .filter(PermissionAttachmentInfo::getValue)
                .map(PermissionAttachmentInfo::getPermission)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        List<Integer> contentSlots = getBorderContentSlots(54);
        int pageSize = contentSlots.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) permissions.size() / pageSize));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));
        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, permissions.size());

        String title = MessageUtils.getColoredMessage(config.getGuiPermissionsTitle()
                .replace("{target}", targetName)
                .replace("{page}", String.valueOf(currentPage + 1))
                .replace("{total}", String.valueOf(totalPages)));

        MaxStaffHolder holder = new MaxStaffHolder("PERMISSIONS", targetName);
        holder.setData("page", currentPage);
        Inventory gui = Bukkit.createInventory(holder, 54, MessageUtils.legacyToComponentNoItalic(title));
        holder.setInventory(gui);

        setupBorder(gui);

        if (permissions.isEmpty()) {
            gui.setItem(22, createItem(Material.BARRIER, config.getGuiPermissionsEmptyName(),
                    config.getGuiPermissionsEmptyLore(), "perm_empty"));
        } else {
            int slotIndex = 0;
            for (int i = start; i < end; i++) {
                String permission = permissions.get(i);
                List<String> lore = List.of(config.getGuiPermissionsItemLore().replace("{permission}", permission));
                gui.setItem(contentSlots.get(slotIndex++), createItem(Material.PAPER,
                        config.getGuiPermissionsItemName().replace("{permission}", permission),
                        lore,
                        "perm_entry"));
            }
        }

        gui.setItem(49, createItem(config.getNavBackMat(), config.getNavBackName(), List.of(config.getGuiNavLoreBack()),
                "back_info"));
        if (currentPage > 0) {
            gui.setItem(45, createItem(config.getNavPrevMat(), config.getNavPrevName(),
                    List.of(config.getGuiNavLorePage().replace("{page}", String.valueOf(currentPage))), "prev_page"));
        }
        if (currentPage + 1 < totalPages) {
            gui.setItem(53,
                    createItem(config.getNavNextMat(), config.getNavNextName(),
                            List.of(config.getGuiNavLorePage().replace("{page}", String.valueOf(currentPage + 2))),
                            "next_page"));
        }

        staff.openInventory(Objects.requireNonNull(gui));
    }

    private List<Integer> getBorderContentSlots(int size) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9;
            int col = slot % 9;
            boolean isBorder = row == 0 || row == (size / 9) - 1 || col == 0 || col == 8;
            if (!isBorder) {
                slots.add(slot);
            }
        }
        return slots;
    }

    public void openActivePunishmentsMenu(Player staff) {
        openActivePunishmentsMenu(staff, 0);
    }

    public void openActivePunishmentsMenu(Player staff, int page) {
        Runnable loadRecords = () -> {
            List<ActivePunishmentRecord> records = new ArrayList<>(plugin.getPunishmentManager().getActivePunishments());
            records.sort(java.util.Comparator
                    .comparing((ActivePunishmentRecord record) -> record.type().ordinal())
                    .thenComparing(record -> safeText(record.targetName()), String.CASE_INSENSITIVE_ORDER));

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (staff.isOnline()) {
                    openActivePunishmentsMenu(staff, records, page);
                }
            });
        };

        if (plugin.getMainConfigManager().isDbEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, loadRecords);
        } else {
            loadRecords.run();
        }
    }

    private void openActivePunishmentsMenu(Player staff, List<ActivePunishmentRecord> records, int page) {
        MainConfigManager config = plugin.getMainConfigManager();
        int menuSize = config.getGuiActiveMenuSize();
        List<Integer> contentSlots = getBorderContentSlots(menuSize);
        int pageSize = Math.max(1, contentSlots.size());
        int totalPages = Math.max(1, (int) Math.ceil((double) records.size() / pageSize));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));
        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, records.size());

        String title = replacePagePlaceholders(config.getGuiActiveTitle(), currentPage, totalPages, records.size());
        MaxStaffHolder holder = new MaxStaffHolder("ACTIVE_PUNISHMENTS", null);
        holder.setData("page", currentPage);
        holder.setData("activeRecords", List.copyOf(records));
        Inventory gui = Bukkit.createInventory(holder, menuSize, MessageUtils.legacyToComponentNoItalic(title));
        holder.setInventory(gui);

        setupBorder(gui);

        if (records.isEmpty()) {
            int emptySlot = contentSlots.isEmpty() ? menuSize / 2 : contentSlots.get(contentSlots.size() / 2);
            gui.setItem(emptySlot, createItem(config.getGuiActiveEmptyMat(), config.getGuiActiveEmptyName(),
                    config.getGuiActiveEmptyLore(), "active_empty"));
        } else {
            int slotIndex = 0;
            for (int i = start; i < end && slotIndex < contentSlots.size(); i++) {
                ActivePunishmentRecord record = records.get(i);
                gui.setItem(contentSlots.get(slotIndex++), createActivePunishmentItem(record));
            }
        }

        int lastRowStart = menuSize - 9;
        gui.setItem(lastRowStart + 4, createItem(config.getGuiActivePageInfoMat(),
                replacePagePlaceholders(config.getGuiActivePageInfoName(), currentPage, totalPages, records.size()),
                config.getGuiActivePageInfoLore().stream()
                        .map(line -> replacePagePlaceholders(line, currentPage, totalPages, records.size()))
                        .toList(),
                "active_page_info"));
        if (currentPage > 0) {
            gui.setItem(lastRowStart, createItem(config.getNavPrevMat(), config.getNavPrevName(),
                    List.of(config.getGuiNavLorePage().replace("{page}", String.valueOf(currentPage))),
                    "active_prev_page"));
        }
        if (currentPage + 1 < totalPages) {
            gui.setItem(lastRowStart + 8, createItem(config.getNavNextMat(), config.getNavNextName(),
                    List.of(config.getGuiNavLorePage().replace("{page}", String.valueOf(currentPage + 2))),
                    "active_next_page"));
        }

        staff.openInventory(Objects.requireNonNull(gui));
        startActivePunishmentsRefresh(staff);
    }

    private ItemStack createActivePunishmentItem(ActivePunishmentRecord record) {
        MainConfigManager config = plugin.getMainConfigManager();
        String type = formatActiveType(record.type());
        String remaining = formatRemainingDetailed(record);
        Material material = switch (record.type()) {
            case BAN -> config.getGuiActiveBanMat();
            case IP_BAN -> config.getGuiActiveIpBanMat();
            case MUTE -> config.getGuiActiveMuteMat();
        };

        String name = applyActivePlaceholders(config.getGuiActiveItemName(), record, type, remaining);
        List<String> lore = config.getGuiActiveItemLore().stream()
                .map(line -> applyActivePlaceholders(line, record, type, remaining))
                .collect(Collectors.toList());
        if (record.type() == ActivePunishmentRecord.Type.IP_BAN) {
            return createItem(material, name, lore, "active_entry");
        }

        return createActivePunishmentHead(record, name, lore, material);
    }

    private ItemStack createActivePunishmentHead(ActivePunishmentRecord record, String name, List<String> lore,
            Material fallbackMaterial) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) {
            return createItem(fallbackMaterial, name, lore, "active_entry");
        }

        String targetName = safeText(record.targetName());
        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        OfflinePlayer target = onlineTarget != null ? onlineTarget : Bukkit.getOfflinePlayer(targetName);
        meta.setOwningPlayer(target);
        meta.displayName(MessageUtils.legacyToComponentNoItalic(name));
        meta.lore(lore.stream().map(MessageUtils::getColoredMessage)
                .map(MessageUtils::legacyToComponentNoItalic).collect(Collectors.toList()));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "active_entry");
        item.setItemMeta(meta);
        return item;
    }

    private String applyActivePlaceholders(String text, ActivePunishmentRecord record, String type, String remaining) {
        if (text == null) {
            return "";
        }
        return text.replace("{type}", type)
                .replace("{target}", safeText(record.targetName()))
                .replace("{staff}", safeText(record.staff()))
                .replace("{reason}", safeText(record.reason()))
                .replace("{duration}", remaining)
                .replace("{remaining}", remaining);
    }

    private String replacePagePlaceholders(String text, int currentPage, int totalPages, int totalRecords) {
        if (text == null) {
            return "";
        }
        return text.replace("{page}", String.valueOf(currentPage + 1))
                .replace("{total}", String.valueOf(totalPages))
                .replace("{total-records}", String.valueOf(totalRecords));
    }

    private String formatActiveType(ActivePunishmentRecord.Type type) {
        return switch (type) {
            case BAN -> "BAN";
            case IP_BAN -> "IP-BAN";
            case MUTE -> "MUTE";
        };
    }

    private String formatRemainingDetailed(ActivePunishmentRecord record) {
        return TimeUtils.getDetailedDurationString(record.remainingMillis(System.currentTimeMillis()),
                plugin.getMainConfigManager());
    }

    private void startActivePunishmentsRefresh(Player staff) {
        cancelActivePunishmentsRefresh(staff.getUniqueId());

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!staff.isOnline() || !refreshOpenActivePunishmentsMenu(staff)) {
                cancelActivePunishmentsRefresh(staff.getUniqueId());
            }
        }, 20L, 20L);
        activePunishmentRefreshTasks.put(staff.getUniqueId(), task);
    }

    private void cancelActivePunishmentsRefresh(UUID staffId) {
        BukkitTask existing = activePunishmentRefreshTasks.remove(staffId);
        if (existing != null) {
            existing.cancel();
        }
    }

    private boolean refreshOpenActivePunishmentsMenu(Player staff) {
        Inventory topInventory = staff.getOpenInventory().getTopInventory();
        if (!(topInventory.getHolder() instanceof MaxStaffHolder holder)
                || !"ACTIVE_PUNISHMENTS".equals(holder.getMenuType())) {
            return false;
        }

        List<ActivePunishmentRecord> records = getActiveRecords(holder).stream()
                .filter(record -> !record.isExpired(System.currentTimeMillis()))
                .toList();
        holder.setData("activeRecords", records);

        MainConfigManager config = plugin.getMainConfigManager();
        int menuSize = topInventory.getSize();
        List<Integer> contentSlots = getBorderContentSlots(menuSize);
        int pageSize = Math.max(1, contentSlots.size());
        int totalPages = Math.max(1, (int) Math.ceil((double) records.size() / pageSize));
        int requestedPage = holder.getData("page") instanceof Integer currentPage ? currentPage : 0;
        int currentPage = Math.max(0, Math.min(requestedPage, totalPages - 1));
        holder.setData("page", currentPage);

        for (int contentSlot : contentSlots) {
            topInventory.setItem(contentSlot, null);
        }

        if (records.isEmpty()) {
            int emptySlot = contentSlots.isEmpty() ? menuSize / 2 : contentSlots.get(contentSlots.size() / 2);
            topInventory.setItem(emptySlot, createItem(config.getGuiActiveEmptyMat(), config.getGuiActiveEmptyName(),
                    config.getGuiActiveEmptyLore(), "active_empty"));
        } else {
            int start = currentPage * pageSize;
            int end = Math.min(start + pageSize, records.size());
            int slotIndex = 0;
            for (int i = start; i < end && slotIndex < contentSlots.size(); i++) {
                topInventory.setItem(contentSlots.get(slotIndex++), createActivePunishmentItem(records.get(i)));
            }
        }

        int lastRowStart = menuSize - 9;
        topInventory.setItem(lastRowStart, null);
        topInventory.setItem(lastRowStart + 8, null);
        topInventory.setItem(lastRowStart + 4, createItem(config.getGuiActivePageInfoMat(),
                replacePagePlaceholders(config.getGuiActivePageInfoName(), currentPage, totalPages, records.size()),
                config.getGuiActivePageInfoLore().stream()
                        .map(line -> replacePagePlaceholders(line, currentPage, totalPages, records.size()))
                        .toList(),
                "active_page_info"));
        if (currentPage > 0) {
            topInventory.setItem(lastRowStart, createItem(config.getNavPrevMat(), config.getNavPrevName(),
                    List.of(config.getGuiNavLorePage().replace("{page}", String.valueOf(currentPage))),
                    "active_prev_page"));
        }
        if (currentPage + 1 < totalPages) {
            topInventory.setItem(lastRowStart + 8, createItem(config.getNavNextMat(), config.getNavNextName(),
                    List.of(config.getGuiNavLorePage().replace("{page}", String.valueOf(currentPage + 2))),
                    "active_next_page"));
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private List<ActivePunishmentRecord> getActiveRecords(MaxStaffHolder holder) {
        Object records = holder.getData("activeRecords");
        if (records instanceof List<?>) {
            return (List<ActivePunishmentRecord>) records;
        }
        return Collections.emptyList();
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }

    public void openSanctionMenu(Player player, String targetName) {
        MainConfigManager config = plugin.getMainConfigManager();
        String titleTemplate = config.getGuiSanctionsTitle();
        String title = MessageUtils.getColoredMessage(titleTemplate.replace("{target}", targetName));

        MaxStaffHolder holder = new MaxStaffHolder("SANCTIONS", targetName);
        Inventory gui = Bukkit.createInventory(holder, 27, MessageUtils.legacyToComponentNoItalic(title));
        holder.setInventory(gui);

        setupBorder(gui);

        gui.setItem(11, createItem(config.getGuiSanctionBanMat(), config.getGuiItemBanName(),
                config.getGuiItemBanLore(), "punish_ban"));
        gui.setItem(13, createItem(config.getGuiSanctionMuteMat(), config.getGuiItemMuteName(),
                config.getGuiItemMuteLore(), "punish_mute"));
        gui.setItem(15, createItem(config.getGuiSanctionKickMat(), config.getGuiItemKickName(),
                config.getGuiItemKickLore(), "punish_kick"));

        gui.setItem(22, createItem(config.getNavBackMat(), config.getNavBackName(),
                Arrays.asList(config.getGuiNavLoreBack()), "back_info"));

        player.openInventory(Objects.requireNonNull(gui));
    }

    public void openReasonsMenu(Player player, String targetName, String type, int page) {
        ConfigurationSection section = plugin.getMainConfigManager().getReasons(type);
        if (section == null)
            return;

        List<String> keys = new ArrayList<>(section.getKeys(false));
        int totalPages = (int) Math.ceil(keys.size() / 4.0);
        MainConfigManager config = plugin.getMainConfigManager();

        String titleTemplate = config.getGuiReasonsTitle();
        String title = MessageUtils.getColoredMessage(titleTemplate
                .replace("{type}", type)
                .replace("{target}", targetName)
                .replace("{page}", String.valueOf(page + 1))
                .replace("{total}", String.valueOf(totalPages == 0 ? 1 : totalPages)));

        MaxStaffHolder holder = new MaxStaffHolder("REASONS", targetName);
        holder.setData("type", type);
        holder.setData("page", page);

        Inventory gui = Bukkit.createInventory(holder, 54, MessageUtils.legacyToComponentNoItalic(title));
        holder.setInventory(gui);

        setupBorder(gui);

        int start = page * 4;
        int end = Math.min(start + 4, keys.size());
        int[] rowStarts = { 10, 19, 28, 37 };

        for (int i = start; i < end; i++) {
            String key = keys.get(i);
            int baseSlot = rowStarts[i - start];

            Material mat = config.getReasonMaterial(type, key);
            if (mat == null || mat == Material.AIR) {
                plugin.getLogger().warning(
                        "¡The material for the sanction '" + key + "' in " + type + " It's invalid! Default paper.");
                mat = Material.PAPER;
            }
            ItemStack rItem = new ItemStack(mat);
            ItemMeta rMeta = rItem.getItemMeta();

            if (rMeta == null) {
                continue;
            }
            String reasonName = config.getReasonName(type, key);

            String rName = config.getGuiReasonsItemName()
                    .replace("{number}", String.valueOf(i + 1));
            rMeta.displayName(MessageUtils.legacyToComponentNoItalic(rName));

            List<String> rLore = new ArrayList<>();
            for (String line : config.getGuiReasonsItemLore()) {
                rLore.add(MessageUtils.getColoredMessage(line
                        .replace("{reason}", reasonName)
                        .replace("{id}", key)));
            }
            rMeta.lore(rLore.stream().map(MessageUtils::legacyToComponentNoItalic).collect(Collectors.toList()));
            rMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            rItem.setItemMeta(rMeta);
            gui.setItem(baseSlot, rItem);

            List<String> durations = config.getReasonDurations(type, key);
            int maxDurationsToShow = type.equals("KICK") ? 1 : 4;
            int durationSlotOffset = 0;

            for (int d = 0; d < maxDurationsToShow; d++) {
                String dur = (d < durations.size()) ? durations.get(d) : (type.equals("KICK") ? "Ahora" : "perm");
                if ((type.equals("BAN") || type.equals("MUTE"))
                        && !config.isPunishmentDurationAllowed(player, type.toLowerCase(), dur)) {
                    continue;
                }
                ItemStack dye = new ItemStack(config.getDurationDye(d));
                ItemMeta dMeta = dye.getItemMeta();
                if (dMeta == null) {
                    continue;
                }

                dMeta.getPersistentDataContainer().set(reasonKey, PersistentDataType.STRING, key);
                dMeta.getPersistentDataContainer().set(durationKey, PersistentDataType.STRING, dur);

                dMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "apply_punish");

                String dName = config.getGuiReasonsDyeName().replace("{duration}", dur);
                dMeta.displayName(MessageUtils.legacyToComponentNoItalic(dName));

                List<String> dLore = new ArrayList<>();
                for (String line : config.getGuiReasonsDyeLore()) {
                    dLore.add(MessageUtils.getColoredMessage(line
                            .replace("{type}", type)
                            .replace("{reason}", reasonName)
                            .replace("{id}", key)
                            .replace("{duration}", dur)));
                }
                dMeta.lore(dLore.stream().map(MessageUtils::legacyToComponentNoItalic).collect(Collectors.toList()));
                dye.setItemMeta(dMeta);
                gui.setItem(baseSlot + (durationSlotOffset + 2), dye);
                durationSlotOffset++;
            }
        }

        gui.setItem(49,
                createItem(config.getNavBackMat(),
                        config.getNavBackName(),
                        Arrays.asList(config.getGuiNavLoreBack()), "back_sanction"));
        if (page > 0)
            gui.setItem(45, createItem(config.getNavPrevMat(),
                    config.getNavPrevName(),
                    Arrays.asList(
                            config.getGuiNavLorePage().replace("{page}", String.valueOf(page))),
                    "prev_page"));
        if (end < keys.size())
            gui.setItem(53,
                    createItem(config.getNavNextMat(),
                            config.getNavNextName(), Arrays.asList(config
                                    .getGuiNavLorePage().replace("{page}", String.valueOf(page + 2))),
                            "next_page"));

        player.openInventory(Objects.requireNonNull(gui));
    }

    public void openConfirmMenu(Player player, String targetName, String type, String reasonId, String duration,
            int page) {
        MainConfigManager config = plugin.getMainConfigManager();
        String reasonName = config.getReasonName(type, reasonId);

        String title = MessageUtils.getColoredMessage(applyConfirmPlaceholders(
                config.getGuiConfirmTitle(), targetName, type, reasonName, duration));

        MaxStaffHolder holder = new MaxStaffHolder("CONFIRM", targetName);
        holder.setData("type", type);
        holder.setData("reasonId", reasonId);
        holder.setData("duration", duration);
        holder.setData("page", page);

        Inventory gui = Bukkit.createInventory(holder, 27, MessageUtils.legacyToComponentNoItalic(title));
        holder.setInventory(gui);

        setupBorder(gui);

        String yesName = applyConfirmPlaceholders(config.getGuiConfirmYesName(), targetName, type, reasonName,
                duration);
        List<String> yesLore = applyConfirmPlaceholders(config.getGuiConfirmYesLore(), targetName, type, reasonName,
                duration);

        String noName = applyConfirmPlaceholders(config.getGuiConfirmNoName(), targetName, type, reasonName, duration);
        List<String> noLore = applyConfirmPlaceholders(config.getGuiConfirmNoLore(), targetName, type, reasonName,
                duration);

        gui.setItem(11, createItem(config.getGuiConfirmYesMat(), yesName, yesLore, "confirm_yes"));
        gui.setItem(15, createItem(config.getGuiConfirmNoMat(), noName, noLore, "confirm_no"));

        player.openInventory(Objects.requireNonNull(gui));
    }

    public void openHistoryMenu(Player staff, String targetName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int bans = plugin.getPunishmentManager().getHistoryCount(targetName, "BAN");
            int mutes = plugin.getPunishmentManager().getHistoryCount(targetName, "MUTE");
            int kicks = plugin.getPunishmentManager().getHistoryCount(targetName, "KICK");
            int warns = plugin.getPunishmentManager().getHistoryCount(targetName, "WARN");

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!staff.isOnline())
                    return;

                MainConfigManager config = plugin.getMainConfigManager();
                String title = MessageUtils
                        .getColoredMessage(config.getGuiHistoryTitle().replace("{target}", targetName));

                MaxStaffHolder holder = new MaxStaffHolder("HISTORY", targetName);
                Inventory gui = Bukkit.createInventory(holder, 27, MessageUtils.legacyToComponentNoItalic(title));
                holder.setInventory(gui);

                setupBorder(gui);

                gui.setItem(10, createItem(config.getGuiHistoryBansMat(), config.getGuiHistoryBansName(),
                        config.getGuiHistoryBansLore().stream().map(s -> s.replace("{count}", String.valueOf(bans)))
                                .toList(),
                        "history_bans"));

                gui.setItem(12, createItem(config.getGuiHistoryMutesMat(), config.getGuiHistoryMutesName(),
                        config.getGuiHistoryMutesLore().stream().map(s -> s.replace("{count}", String.valueOf(mutes)))
                                .toList(),
                        "history_mutes"));

                gui.setItem(14, createItem(config.getGuiHistoryWarnsMat(), config.getGuiHistoryWarnsName(),
                        config.getGuiHistoryWarnsLore().stream().map(s -> s.replace("{count}", String.valueOf(warns)))
                                .toList(),
                        "history_warns"));

                gui.setItem(16, createItem(config.getGuiHistoryKicksMat(), config.getGuiHistoryKicksName(),
                        config.getGuiHistoryKicksLore().stream().map(s -> s.replace("{count}", String.valueOf(kicks)))
                                .toList(),
                        "history_kicks"));

                gui.setItem(config.getGuiHistoryBackSlot(),
                        createItem(config.getNavBackMat(), config.getNavBackName(), config.getGuiHistoryBackLore(),
                                "back_info"));

                staff.openInventory(Objects.requireNonNull(gui));
            });
        });
    }

    public void openDetailedHistoryMenu(Player staff, String targetName, String type) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> records = plugin.getPunishmentManager().getHistoryDetails(targetName, type);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!staff.isOnline())
                    return;

                MainConfigManager config = plugin.getMainConfigManager();
                String title = MessageUtils.getColoredMessage(config.getGuiDetailedTitle()
                        .replace("{type}", type).replace("{target}", targetName));

                MaxStaffHolder holder = new MaxStaffHolder("DETAILED_HISTORY", targetName);
                holder.setData("type", type);

                Inventory gui = Bukkit.createInventory(holder, 45, MessageUtils.legacyToComponentNoItalic(title));
                holder.setInventory(gui);

                setupBorder(gui);

                int slot = 10;
                for (int i = 0; i < records.size(); i++) {
                    if (slot > 34)
                        break;
                    if ((slot + 1) % 9 == 0)
                        slot += 2;

                    String record = records.get(i);
                    String[] parts = record.split("\\|");

                    List<String> lore = Arrays.asList(
                            config.getGuiDetailedDate().replace("{date}", parts[0]),
                            config.getGuiDetailedStaff().replace("{staff}", parts[1]),
                            config.getGuiDetailedReason().replace("{reason}", parts[2]),
                            config.getGuiDetailedDuration().replace("{duration}", parts[3]));

                    gui.setItem(slot,
                            createItem(config.getGuiDetailedRecordMat(),
                                    config.getGuiDetailedItemName().replace("{number}", String.valueOf(i + 1)), lore,
                                    "history_record"));
                    slot++;
                }

                gui.setItem(40, createItem(config.getNavBackMat(), config.getNavBackName(),
                        config.getGuiDetailedBackLore(), "back_history"));
                staff.openInventory(Objects.requireNonNull(gui));
            });
        });
    }

    public void openGameModeMenu(Player player) {
        MainConfigManager config = plugin.getMainConfigManager();

        MaxStaffHolder holder = new MaxStaffHolder("GAMEMODE", null);
        Inventory gui = Bukkit.createInventory(holder, 27,
                MessageUtils.legacyToComponentNoItalic(config.getGuiGmTitle()));
        holder.setInventory(gui);

        setupBorder(gui);

        gui.setItem(10, createItem(config.getGuiGmSurvivalMat(), config.getGuiGmSurvivalName(),
                config.getGuiGmSurvivalLore(), "gm_survival"));
        gui.setItem(12, createItem(config.getGuiGmCreativeMat(), config.getGuiGmCreativeName(),
                config.getGuiGmCreativeLore(), "gm_creative"));
        gui.setItem(14, createItem(config.getGuiGmAdventureMat(), config.getGuiGmAdventureName(),
                config.getGuiGmAdventureLore(), "gm_adventure"));
        gui.setItem(16, createItem(config.getGuiGmSpectatorMat(), config.getGuiGmSpectatorName(),
                config.getGuiGmSpectatorLore(), "gm_spectator"));

        player.openInventory(Objects.requireNonNull(gui));
    }

    public void openAltsMenu(Player staff, String targetName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String ip = plugin.getPunishmentManager().getPlayerIP(targetName);

            if (ip == null || ip.isEmpty()) {
                plugin.getLogger().warning("/alts could not find a cached IP for target " + targetName + ".");
                Bukkit.getScheduler().runTask(plugin,
                        () -> staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix()
                                + plugin.getMainConfigManager().getMsgNoIPFound() + " &7(Target: &f" + targetName
                                + "&7)")));
                return;
            }

            List<UUID> altsUUIDs = plugin.getPunishmentManager().getAllAccountsByIP(ip);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!staff.isOnline())
                    return;

                MainConfigManager config = plugin.getMainConfigManager();

                MaxStaffHolder holder = new MaxStaffHolder("ALTS", targetName);
                Inventory gui = Bukkit.createInventory(holder, 45, MessageUtils
                        .legacyToComponentNoItalic(config.getGuiAltsTitle().replace("{target}", targetName)));
                holder.setInventory(gui);

                setupBorder(gui);

                for (UUID uuid : altsUUIDs) {
                    OfflinePlayer altPlayer = Bukkit.getOfflinePlayer(uuid);
                    String name = altPlayer.getName();
                    if (name == null)
                        continue;

                    String status;
                    String color;

                    if (BanUtils.isPlayerNameBanned(name)) {
                        status = config.getGuiAltsStatusBanned();
                        color = "&4";
                    } else if (altPlayer.isOnline()) {
                        status = config.getGuiAltsStatusOnline();
                        color = "&a";
                    } else {
                        status = config.getGuiAltsStatusOffline();
                        color = "&c";
                    }

                    String finalStatus = status;
                    List<String> lore = config.getGuiAltsLore().stream()
                            .map(line -> line.replace("{dynamic}", config.getGuiAltsDynamic())
                                    .replace("{status}", finalStatus)
                                    .replace("{ip}", ip))
                            .collect(Collectors.toList());

                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    meta.setOwningPlayer(altPlayer);
                    meta.displayName(MessageUtils.legacyToComponentNoItalic(color + name));
                    meta.lore(lore.stream().map(MessageUtils::getColoredMessage)
                            .map(MessageUtils::legacyToComponentNoItalic).collect(Collectors.toList()));
                    meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "alt_head");
                    head.setItemMeta(meta);

                    gui.addItem(head);
                }
                if (gui != null) staff.openInventory(Objects.requireNonNull(gui));
            });
        });
    }

    public void openReviveMenu(Player staff) {
        openReviveMenu(staff, 0);
    }

    public void openReviveMenu(Player staff, int page) {
        MainConfigManager config = plugin.getMainConfigManager();
        List<InventorySnapshotManager.DeathSnapshot> snapshots = plugin.getInventorySnapshotManager()
                .getDeathSnapshots();

        if (snapshots.isEmpty()) {
            staff.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getReviveNoDeaths()));
            return;
        }

        int menuSize = config.getReviveMenuSize();
        int pageSize = Math.max(1, config.getRevivePageSize());
        int totalPages = Math.max(1, (int) Math.ceil((double) snapshots.size() / pageSize));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));
        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, snapshots.size());

        MaxStaffHolder holder = new MaxStaffHolder("REVIVE", staff.getName());
        holder.setData("page", currentPage);
        Inventory gui = Bukkit.createInventory(holder, menuSize,
                MessageUtils.legacyToComponentNoItalic(config.getGuiReviveTitle()
                        .replace("{page}", String.valueOf(currentPage + 1))
                        .replace("{total}", String.valueOf(totalPages))));
        holder.setInventory(gui);

        List<Integer> contentSlots = getReviveContentSlots(menuSize);
        int slotIndex = 0;
        for (int i = start; i < end && slotIndex < contentSlots.size(); i++) {
            InventorySnapshotManager.DeathSnapshot snapshot = snapshots.get(i);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta == null) {
                continue;
            }

            if (snapshot.uuid() != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(snapshot.uuid()));
            }

            String formattedDate = snapshot.updatedAt() <= 0
                    ? "unknown"
                    : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(snapshot.updatedAt()));

            meta.displayName(MessageUtils.legacyToComponentNoItalic(config.getGuiReviveItemName()
                    .replace("{player}", snapshot.playerName())));

            String itemAmount = String.valueOf(countSnapshotItems(snapshot));
            List<String> lore = config.getGuiReviveItemLore().stream()
                    .map(line -> line.replace("{player}", snapshot.playerName())
                            .replace("{death-cause}", snapshot.deathCause())
                            .replace("{date}", formattedDate)
                            .replace("{items-amount}", itemAmount)
                            .replace("{item-count}", itemAmount)
                            .replace("{page}", String.valueOf(currentPage + 1))
                            .replace("{total}", String.valueOf(totalPages)))
                    .map(MessageUtils::getColoredMessage)
                    .collect(Collectors.toList());
            meta.lore(lore.stream().map(MessageUtils::legacyToComponentNoItalic).collect(Collectors.toList()));
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "revive_apply");
            meta.getPersistentDataContainer().set(reviveTargetKey, PersistentDataType.STRING, snapshot.playerName());
            head.setItemMeta(meta);

            gui.setItem(contentSlots.get(slotIndex++), head);
        }

        int lastRowStart = menuSize - 9;
        String snapshotsOnPage = String.valueOf(end - start);
        String pageInfoName = config.getGuiRevivePageInfoName()
                .replace("{page}", String.valueOf(currentPage + 1))
                .replace("{total}", String.valueOf(totalPages))
                .replace("{snapshots-on-page}", snapshotsOnPage);
        List<String> pageInfoLore = config.getGuiRevivePageInfoLore().stream()
                .map(line -> line
                        .replace("{page}", String.valueOf(currentPage + 1))
                        .replace("{total}", String.valueOf(totalPages))
                        .replace("{snapshots-on-page}", snapshotsOnPage))
                .collect(Collectors.toList());

        gui.setItem(lastRowStart + 4, createItem(Material.PAPER,
                pageInfoName,
                pageInfoLore,
                "revive_page_info"));
        if (currentPage > 0) {
            gui.setItem(lastRowStart, createItem(config.getNavPrevMat(), config.getNavPrevName(),
                    List.of(config.getGuiNavLorePage().replace("{page}", String.valueOf(currentPage))),
                    "revive_prev_page"));
        }
        if (currentPage + 1 < totalPages) {
            gui.setItem(lastRowStart + 8, createItem(config.getNavNextMat(), config.getNavNextName(),
                    List.of(config.getGuiNavLorePage().replace("{page}", String.valueOf(currentPage + 2))),
                    "revive_next_page"));
        }

        staff.openInventory(Objects.requireNonNull(gui));
    }

    private List<Integer> getReviveContentSlots(int size) {
        List<Integer> slots = new ArrayList<>();
        int lastRowStart = size - 9;

        for (int slot = 0; slot < size; slot++) {
            if (slot == lastRowStart || slot == lastRowStart + 4 || slot == lastRowStart + 8) {
                continue;
            }
            slots.add(slot);
        }

        return slots;
    }

    public InventorySnapshotManager.DeathSnapshot restoreLatestDeathInventory(Player target) {
        InventorySnapshotManager.DeathSnapshot snapshot = plugin.getInventorySnapshotManager()
                .consumeDeathSnapshotByName(target.getName())
                .orElse(null);
        if (snapshot == null) {
            return null;
        }

        target.getInventory().setStorageContents(snapshot.storage().clone());
        target.getInventory().setArmorContents(snapshot.armor().clone());
        target.getInventory().setItemInOffHand(snapshot.offhand() == null ? null : snapshot.offhand().clone());
        target.setLevel(snapshot.xpLevel());
        target.setTotalExperience(snapshot.xpTotal());
        target.setExp(snapshot.xpProgress());
        target.updateInventory();
        return snapshot;
    }

    private int countSnapshotItems(InventorySnapshotManager.DeathSnapshot snapshot) {
        int count = 0;

        for (ItemStack item : snapshot.storage()) {
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                count += item.getAmount();
            }
        }

        for (ItemStack item : snapshot.armor()) {
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                count += item.getAmount();
            }
        }

        ItemStack offhand = snapshot.offhand();
        if (offhand != null && offhand.getType() != Material.AIR && offhand.getAmount() > 0) {
            count += offhand.getAmount();
        }

        return count;
    }

    public ItemStack createItem(Material material, String name, List<String> lore, String action) {
        if (material == null || material == Material.AIR) {
            material = Material.BARRIER;
        }
        if (name == null) {
            name = " ";
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtils.legacyToComponentNoItalic(name));
            if (lore != null) {
                meta.lore(lore.stream().map(MessageUtils::getColoredMessage)
                        .map(MessageUtils::legacyToComponentNoItalic).collect(Collectors.toList()));
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

            if (action != null) {
                NamespacedKey key = new NamespacedKey(plugin, "gui_action");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, action);
            }

            item.setItemMeta(meta);
        }
        return item;
    }


    private ItemStack createXraySuspectHead(XraySuspect suspect) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            OfflinePlayer offlinePlayer = suspect.getOfflinePlayer();
            meta.setOwningPlayer(offlinePlayer);
            meta.displayName(MessageUtils.legacyToComponentNoItalic(applyXrayPlaceholders(
                    plugin.getMainConfigManager().getGuiXrayHeadName(), suspect)));
            meta.lore(applyXrayPlaceholders(plugin.getMainConfigManager().getGuiXrayHeadLore(), suspect).stream()
                    .map(MessageUtils::getColoredMessage)
                    .map(MessageUtils::legacyToComponentNoItalic)
                    .collect(Collectors.toList()));
            meta.getPersistentDataContainer().set(targetPlayerKey, PersistentDataType.STRING,
                    suspect.getPlayerUuid().toString());
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "xray_tp_player");
            item.setItemMeta(meta);
        }
        return item;
    }

    private String applyXrayPlaceholders(String input, XraySuspect suspect) {
        if (input == null) {
            return "";
        }
        org.bukkit.Location location = suspect.getLastLocation();
        String world = location == null || location.getWorld() == null ? "Unknown" : location.getWorld().getName();
        String x = location == null ? "?" : String.valueOf(location.getBlockX());
        String y = location == null ? "?" : String.valueOf(location.getBlockY());
        String z = location == null ? "?" : String.valueOf(location.getBlockZ());
        return input
                .replace("{player}", suspect.getPlayerName())
                .replace("{mineral}", plugin.getMainConfigManager().getAntiXrayDisplayName(suspect.getLastMaterial()))
                .replace("{rate}", String.valueOf(suspect.getSessionRate()))
                .replace("{window_rate}", String.valueOf(suspect.getWindowRate()))
                .replace("{window_total}", String.valueOf(suspect.getWindowTotal()))
                .replace("{window_seconds}", String.valueOf(plugin.getMainConfigManager().getAntiXrayRateWindowSeconds()))
                .replace("{world}", world)
                .replace("{x}", x)
                .replace("{y}", y)
                .replace("{z}", z);
    }

    private List<String> applyXrayPlaceholders(List<String> lines, XraySuspect suspect) {
        if (lines == null) {
            return Collections.emptyList();
        }
        return lines.stream().map(line -> applyXrayPlaceholders(line, suspect)).collect(Collectors.toList());
    }

    private ItemStack createPlayerHead(Player p) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtils.legacyToComponentNoItalic("&a" + p.getName()));
            meta.setOwningPlayer(p);
            meta.lore(Collections.singletonList(
                    MessageUtils.legacyToComponentNoItalic(plugin.getMainConfigManager().getGuiHeadLore())));
            meta.getPersistentDataContainer().set(targetPlayerKey, PersistentDataType.STRING,
                    p.getUniqueId().toString());
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "tp_player");
            item.setItemMeta(meta);
        }
        return item;
    }

    private String applyConfirmPlaceholders(String text, String targetName, String type, String reason,
            String duration) {
        if (text == null) {
            return "";
        }

        String safeTarget = targetName == null ? "Unknown" : targetName;
        String safeType = type == null ? "Unknown" : type;
        String safeReason = reason == null ? "No reason" : reason;
        String safeDuration = duration == null ? "Permanent" : duration;

        return text.replace("{target}", safeTarget)
                .replace("{type}", safeType)
                .replace("{reason}", safeReason)
                .replace("{duration}", safeDuration);
    }

    private List<String> applyConfirmPlaceholders(List<String> lines, String targetName, String type, String reason,
            String duration) {
        if (lines == null || lines.isEmpty()) {
            return Collections.emptyList();
        }
        return lines.stream()
                .map(line -> applyConfirmPlaceholders(line, targetName, type, reason, duration))
                .collect(Collectors.toList());
    }

}
