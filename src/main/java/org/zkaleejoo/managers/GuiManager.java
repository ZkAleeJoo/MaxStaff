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
import org.bukkit.persistence.PersistentDataType;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.MaxStaffHolder; 

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GuiManager {

    private final MaxStaff plugin;
    private final NamespacedKey actionKey;

    public GuiManager(MaxStaff plugin) { 
        this.plugin = plugin; 
        this.actionKey = new NamespacedKey(plugin, "gui_action");
    }

    private void setupBorder(Inventory inv) {
        ItemStack border = createItem(plugin.getMainConfigManager().getBorderMaterial(), " ", null, "border");
        int size = inv.getSize();
        
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = size - 9; i < size; i++) inv.setItem(i, border);
        for (int i = 9; i < size - 9; i += 9) {
            inv.setItem(i, border);
            inv.setItem(i + 8, border);
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
                if (!staff.isOnline()) return;

                MainConfigManager config = plugin.getMainConfigManager();
                String title = MessageUtils.getColoredMessage(config.getGuiInfoTitle().replace("{target}", target.getName()));
                
                MaxStaffHolder holder = new MaxStaffHolder("INFO", target.getName());
                Inventory gui = Bukkit.createInventory(holder, 45, title);
                holder.setInventory(gui);
                
                setupBorder(gui);

                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta headMeta = (SkullMeta) head.getItemMeta();
                if (headMeta != null) {
                    headMeta.setOwningPlayer(target);
                    headMeta.setDisplayName(MessageUtils.getColoredMessage(config.getGuiInfoHeadName().replace("{target}", target.getName())));
                    
                    String statusText = target.isOnline() ? config.getStatusOnline() : config.getStatusOffline();
                    String ip = target.getAddress() != null ? target.getAddress().getAddress().getHostAddress() : "Offline";
                    
                    List<String> headLore = config.getGuiInfoHeadLore().stream()
                            .map(line -> line.replace("{status}", statusText)
                                            .replace("{health}", String.valueOf((int)target.getHealth()))
                                            .replace("{food}", String.valueOf(target.getFoodLevel()))
                                            .replace("{gm}", target.getGameMode().name())
                                            .replace("{ip}", ip))
                            .collect(Collectors.toList());
                    
                    headMeta.setLore(headLore.stream().map(MessageUtils::getColoredMessage).collect(Collectors.toList()));
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
                gui.setItem(20, createItem(config.getGuiInfoStatsMat(), config.getGuiInfoStatsName(), statsLore, "info_stats"));

                List<String> historyLore = config.getGuiInfoHistoryLore().stream()
                        .map(line -> line.replace("{bans}", String.valueOf(bans))
                                        .replace("{mutes}", String.valueOf(mutes))
                                        .replace("{kicks}", String.valueOf(kicks))
                                        .replace("{warns}", String.valueOf(warns)))
                        .collect(Collectors.toList());
                gui.setItem(21, createItem(config.getGuiInfoHistoryMat(), config.getGuiInfoHistoryName(), historyLore, "open_history"));

                gui.setItem(22, createItem(config.getGuiInfoActionMat(), config.getGuiInfoActionName(), config.getGuiInfoActionLore(), "open_sanction"));
                gui.setItem(23, createItem(config.getGuiInfoAltsMat(), config.getGuiInfoAltsName(), config.getGuiInfoAltsLore(), "open_alts"));
                gui.setItem(24, createItem(config.getGuiInfoInvMat(), config.getGuiInfoInvName(), config.getGuiInfoInvLore(), "open_inv"));

                staff.openInventory(gui);
            });
        });
    }

    public void openPlayersMenu(Player player) {
        MaxStaffHolder holder = new MaxStaffHolder("PLAYERS", null);
        
        Inventory gui = Bukkit.createInventory(holder, 54, MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiPlayersTitle()));
        holder.setInventory(gui);
        
        setupBorder(gui);
        for (Player target : Bukkit.getOnlinePlayers()) {
            gui.addItem(createPlayerHead(target));
        }
        player.openInventory(gui);
    }

    public void openSanctionMenu(Player player, String targetName) {
        MainConfigManager config = plugin.getMainConfigManager();
        String title = MessageUtils.getColoredMessage(config.getGuiSanctionsTitle().replace("{target}", targetName));
        
        MaxStaffHolder holder = new MaxStaffHolder("SANCTIONS", targetName);
        Inventory gui = Bukkit.createInventory(holder, 27, title);
        holder.setInventory(gui);
        
        setupBorder(gui);
        
        gui.setItem(11, createItem(Material.IRON_SWORD, config.getGuiItemBanName(), config.getGuiItemBanLore(), "punish_ban"));
        gui.setItem(13, createItem(Material.PAPER, config.getGuiItemMuteName(), config.getGuiItemMuteLore(), "punish_mute"));
        gui.setItem(15, createItem(Material.FEATHER, config.getGuiItemKickName(), config.getGuiItemKickLore(), "punish_kick"));
        
        gui.setItem(22, createItem(config.getNavBackMat(), config.getNavBackName(), Arrays.asList("&7Volver a Información"), "back_info"));
        
        player.openInventory(gui);
    }

    public void openReasonsMenu(Player player, String targetName, String type, int page) {
        ConfigurationSection section = plugin.getMainConfigManager().getReasons(type);
        if (section == null) return;

        List<String> keys = new ArrayList<>(section.getKeys(false));
        int totalPages = (int) Math.ceil(keys.size() / 4.0);
        
        String titleTemplate = plugin.getMainConfigManager().getGuiReasonsTitle();
        String title = MessageUtils.getColoredMessage(titleTemplate
            .replace("{type}", type)
            .replace("{target}", targetName)
            .replace("{page}", String.valueOf(page + 1))
            .replace("{total}", String.valueOf(totalPages == 0 ? 1 : totalPages)));
        
        MaxStaffHolder holder = new MaxStaffHolder("REASONS", targetName);
        holder.setData("type", type);
        holder.setData("page", page);

        Inventory gui = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(gui);
        
        setupBorder(gui);
        
        int start = page * 4;
        int end = Math.min(start + 4, keys.size());
        int[] rowStarts = {10, 19, 28, 37}; 

        for (int i = start; i < end; i++) {
            String key = keys.get(i);
            int baseSlot = rowStarts[i - start];
            
            ItemStack rItem = new ItemStack(plugin.getMainConfigManager().getReasonMaterial(type, key));
            ItemMeta rMeta = rItem.getItemMeta();
            
            String rName = plugin.getMainConfigManager().getGuiReasonsItemName()
                    .replace("{number}", String.valueOf(i + 1));
            rMeta.setDisplayName(MessageUtils.getColoredMessage(rName));
            
            List<String> rLore = new ArrayList<>();
            for (String line : plugin.getMainConfigManager().getGuiReasonsItemLore()) {
                rLore.add(MessageUtils.getColoredMessage(line
                        .replace("{reason}", plugin.getMainConfigManager().getReasonName(type, key))
                        .replace("{id}", key)));
            }
            rMeta.setLore(rLore);
            rMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            rMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "reason_display");
            rItem.setItemMeta(rMeta);
            gui.setItem(baseSlot, rItem);

            List<String> durations = plugin.getMainConfigManager().getReasonDurations(type, key);
            int maxDurationsToShow = type.equals("KICK") ? 1 : 4;

            for (int d = 0; d < maxDurationsToShow; d++) {
                String dur = (d < durations.size()) ? durations.get(d) : (type.equals("KICK") ? "Ahora" : "perm");
                ItemStack dye = new ItemStack(plugin.getMainConfigManager().getDurationDye(d));
                ItemMeta dMeta = dye.getItemMeta();

                NamespacedKey reasonKey = new NamespacedKey(plugin, "reason_id");
                NamespacedKey durationKey = new NamespacedKey(plugin, "duration");
                dMeta.getPersistentDataContainer().set(reasonKey, PersistentDataType.STRING, key);
                dMeta.getPersistentDataContainer().set(durationKey, PersistentDataType.STRING, dur);
                
                dMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "apply_punish");

                String dName = plugin.getMainConfigManager().getGuiReasonsDyeName().replace("{duration}", dur);
                dMeta.setDisplayName(MessageUtils.getColoredMessage(dName));
                
                List<String> dLore = new ArrayList<>();
                for (String line : plugin.getMainConfigManager().getGuiReasonsDyeLore()) {
                    dLore.add(MessageUtils.getColoredMessage(line
                            .replace("{type}", type)
                            .replace("{reason}", plugin.getMainConfigManager().getReasonName(type, key))
                            .replace("{id}", key)
                            .replace("{duration}", dur)));
                }
                dMeta.setLore(dLore);
                dye.setItemMeta(dMeta);
                gui.setItem(baseSlot + (d + 2), dye);
            }
        }

        gui.setItem(49, createItem(plugin.getMainConfigManager().getNavBackMat(), plugin.getMainConfigManager().getNavBackName(), Arrays.asList(MessageUtils.getColoredMessage("&7Regresar")), "back_sanction"));
        if (page > 0) gui.setItem(45, createItem(plugin.getMainConfigManager().getNavPrevMat(), plugin.getMainConfigManager().getNavPrevName(), Arrays.asList(MessageUtils.getColoredMessage("&7Ir a Página " + page)), "prev_page"));
        if (end < keys.size()) gui.setItem(53, createItem(plugin.getMainConfigManager().getNavNextMat(), plugin.getMainConfigManager().getNavNextName(), Arrays.asList(MessageUtils.getColoredMessage("&7Ir a Página " + (page + 2))), "next_page"));
        
        player.openInventory(gui);
    }

    public void openHistoryMenu(Player staff, String targetName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int bans = plugin.getPunishmentManager().getHistoryCount(targetName, "BAN");
            int mutes = plugin.getPunishmentManager().getHistoryCount(targetName, "MUTE");
            int kicks = plugin.getPunishmentManager().getHistoryCount(targetName, "KICK");
            int warns = plugin.getPunishmentManager().getHistoryCount(targetName, "WARN");

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!staff.isOnline()) return;

                MainConfigManager config = plugin.getMainConfigManager();
                String title = MessageUtils.getColoredMessage(config.getGuiHistoryTitle().replace("{target}", targetName));
                
                MaxStaffHolder holder = new MaxStaffHolder("HISTORY", targetName);
                Inventory gui = Bukkit.createInventory(holder, 27, title);
                holder.setInventory(gui);
                
                setupBorder(gui);

                gui.setItem(10, createItem(Material.RED_WOOL, config.getGuiHistoryBansName(), 
                    config.getGuiHistoryBansLore().stream().map(s -> s.replace("{count}", String.valueOf(bans))).toList(), "history_bans"));

                gui.setItem(12, createItem(Material.ORANGE_WOOL, config.getGuiHistoryMutesName(), 
                    config.getGuiHistoryMutesLore().stream().map(s -> s.replace("{count}", String.valueOf(mutes))).toList(), "history_mutes"));

                gui.setItem(14, createItem(Material.YELLOW_WOOL, config.getGuiHistoryWarnsName(), 
                    config.getGuiHistoryWarnsLore().stream().map(s -> s.replace("{count}", String.valueOf(warns))).toList(), "history_warns"));

                gui.setItem(16, createItem(Material.LIGHT_GRAY_WOOL, config.getGuiHistoryKicksName(), 
                    config.getGuiHistoryKicksLore().stream().map(s -> s.replace("{count}", String.valueOf(kicks))).toList(), "history_kicks"));

                staff.openInventory(gui);
            });
        });
    }

    public void openDetailedHistoryMenu(Player staff, String targetName, String type) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> records = plugin.getPunishmentManager().getHistoryDetails(targetName, type);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!staff.isOnline()) return;

                MainConfigManager config = plugin.getMainConfigManager();
                String title = MessageUtils.getColoredMessage(config.getGuiDetailedTitle()
                        .replace("{type}", type).replace("{target}", targetName));
                
                MaxStaffHolder holder = new MaxStaffHolder("DETAILED_HISTORY", targetName);
                holder.setData("type", type);

                Inventory gui = Bukkit.createInventory(holder, 45, title);
                holder.setInventory(gui);
                
                setupBorder(gui);
                
                int slot = 10;
                for (int i = 0; i < records.size(); i++) {
                    if (slot > 34) break; 
                    if ((slot + 1) % 9 == 0) slot += 2; 

                    String record = records.get(i);
                    String[] parts = record.split("\\|");
                    
                    List<String> lore = Arrays.asList(
                        config.getGuiDetailedDate().replace("{date}", parts[0]),
                        config.getGuiDetailedStaff().replace("{staff}", parts[1]),
                        config.getGuiDetailedReason().replace("{reason}", parts[2]),
                        config.getGuiDetailedDuration().replace("{duration}", parts[3])
                    );

                    gui.setItem(slot, createItem(Material.PAPER, config.getGuiDetailedItemName().replace("{number}", String.valueOf(i + 1)), lore, "history_record"));
                    slot++;
                }

                gui.setItem(40, createItem(config.getNavBackMat(), config.getNavBackName(), config.getGuiDetailedBackLore(), "back_history"));
                staff.openInventory(gui);
            });
        });
    }

    public void openGameModeMenu(Player player) {
        MainConfigManager config = plugin.getMainConfigManager();
        
        MaxStaffHolder holder = new MaxStaffHolder("GAMEMODE", null);
        Inventory gui = Bukkit.createInventory(holder, 27, MessageUtils.getColoredMessage(config.getGuiGmTitle()));
        holder.setInventory(gui);
        
        setupBorder(gui);

        gui.setItem(10, createItem(config.getGuiGmSurvivalMat(), config.getGuiGmSurvivalName(), config.getGuiGmSurvivalLore(), "gm_survival"));
        gui.setItem(12, createItem(config.getGuiGmCreativeMat(), config.getGuiGmCreativeName(), config.getGuiGmCreativeLore(), "gm_creative"));
        gui.setItem(14, createItem(config.getGuiGmAdventureMat(), config.getGuiGmAdventureName(), config.getGuiGmAdventureLore(), "gm_adventure"));
        gui.setItem(16, createItem(config.getGuiGmSpectatorMat(), config.getGuiGmSpectatorName(), config.getGuiGmSpectatorLore(), "gm_spectator"));

        player.openInventory(gui);
    }

    public void openAltsMenu(Player staff, String targetName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String ip = plugin.getPunishmentManager().getPlayerIP(targetName);
            
            if (ip == null) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgNoIPFound())));
                return;
            }

            List<UUID> altsUUIDs = plugin.getPunishmentManager().getAllAccountsByIP(ip);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!staff.isOnline()) return;

                MainConfigManager config = plugin.getMainConfigManager();
                
                MaxStaffHolder holder = new MaxStaffHolder("ALTS", targetName);
                Inventory gui = Bukkit.createInventory(holder, 45, MessageUtils.getColoredMessage(config.getGuiAltsTitle().replace("{target}", targetName)));
                holder.setInventory(gui);
                
                setupBorder(gui);

                for (UUID uuid : altsUUIDs) {
                    OfflinePlayer altPlayer = Bukkit.getOfflinePlayer(uuid);
                    String name = altPlayer.getName();
                    if (name == null) continue;

                    String status;
                    String color;

                    if (Bukkit.getBanList(org.bukkit.BanList.Type.NAME).isBanned(name)) {
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
                    meta.setDisplayName(MessageUtils.getColoredMessage(color + name));
                    meta.setLore(lore.stream().map(MessageUtils::getColoredMessage).collect(Collectors.toList()));
                    meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "alt_head");
                    head.setItemMeta(meta);

                    gui.addItem(head);
                }
                staff.openInventory(gui);
            });
        });
    }

    private ItemStack createItem(Material mat, String name, List<String> lore, String action) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtils.getColoredMessage(name));
            if (lore != null) {
                List<String> colored = new ArrayList<>();
                for (String l : lore) colored.add(MessageUtils.getColoredMessage(l));
                meta.setLore(colored);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            
            if (action != null && !action.isEmpty()) {
                meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPlayerHead(Player p) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtils.getColoredMessage("&a" + p.getName()));
            meta.setOwningPlayer(p);
            meta.setLore(Arrays.asList(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiHeadLore())));
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "tp_player");
            item.setItemMeta(meta);
        }
        return item;
    }
}