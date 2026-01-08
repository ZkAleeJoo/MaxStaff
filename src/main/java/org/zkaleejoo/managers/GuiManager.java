package org.zkaleejoo.managers;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GuiManager {

    private final MaxStaff plugin;

    public GuiManager(MaxStaff plugin) { 
        this.plugin = plugin; 
    }

    private Component t(String text) {
        return Component.text(MessageUtils.getColoredMessage(text));
    }

    public void openUserInfoMenu(Player staff, Player target) {
        MainConfigManager config = plugin.getMainConfigManager();
        
        Gui gui = Gui.gui()
                .title(t(config.getGuiInfoTitle().replace("{target}", target.getName())))
                .rows(3)
                .create();

        gui.setDefaultClickAction(event -> event.setCancelled(true));
        gui.getFiller().fillBorder(ItemBuilder.from(config.getBorderMaterial()).name(t(" ")).asGuiItem());

        long ticks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
        String playtime = (ticks / 72000) + "h " + ((ticks % 72000) / 1200) + "m";
        
        int bans = plugin.getPunishmentManager().getHistoryCount(target.getName(), "BAN");
        int mutes = plugin.getPunishmentManager().getHistoryCount(target.getName(), "MUTE");
        int kicks = plugin.getPunishmentManager().getHistoryCount(target.getName(), "KICK");
        int warns = plugin.getPunishmentManager().getHistoryCount(target.getName(), "WARN");

        List<Component> statsLore = config.getGuiInfoStatsLore().stream()
                .map(line -> t(line.replace("{target}", target.getName())
                        .replace("{uuid}", target.getUniqueId().toString())
                        .replace("{playtime}", playtime)
                        .replace("{total_punishments}", String.valueOf(bans + mutes + kicks))))
                .collect(Collectors.toList());

        gui.setItem(11, ItemBuilder.from(config.getGuiInfoStatsMat()).name(t(config.getGuiInfoStatsName())).lore(statsLore).asGuiItem());

        List<Component> historyLore = config.getGuiInfoHistoryLore().stream()
                .map(line -> t(line.replace("{bans}", String.valueOf(bans))
                        .replace("{mutes}", String.valueOf(mutes))
                        .replace("{kicks}", String.valueOf(kicks))
                        .replace("{warns}", String.valueOf(warns))))
                .collect(Collectors.toList());

        gui.setItem(13, ItemBuilder.from(config.getGuiInfoHistoryMat()).name(t(config.getGuiInfoHistoryName())).lore(historyLore).asGuiItem());

        gui.setItem(15, ItemBuilder.from(config.getGuiInfoActionMat())
                .name(t(config.getGuiInfoActionName()))
                .lore(config.getGuiInfoActionLore().stream().map(this::t).collect(Collectors.toList()))
                .asGuiItem(event -> openSanctionMenu(staff, target.getName())));

        gui.open(staff);
    }

    public void openPlayersMenu(Player player) {
        MainConfigManager config = plugin.getMainConfigManager();
        
        PaginatedGui gui = Gui.paginated()
                .title(t(config.getGuiPlayersTitle()))
                .rows(6)
                .pageSize(45)
                .create();

        gui.setDefaultClickAction(event -> event.setCancelled(true));
        gui.getFiller().fillBottom(ItemBuilder.from(config.getBorderMaterial()).name(t(" ")).asGuiItem());

        for (Player target : Bukkit.getOnlinePlayers()) {
            gui.addItem(ItemBuilder.from(createPlayerHead(target))
                    .asGuiItem(event -> {
                        player.teleport(target);
                        player.sendMessage(MessageUtils.getColoredMessage(config.getMsgTeleport().replace("{player}", target.getName())));
                    }));
        }

        gui.setItem(48, ItemBuilder.from(config.getNavPrevMat()).name(t(config.getNavPrevName())).asGuiItem(event -> gui.previous()));
        gui.setItem(50, ItemBuilder.from(config.getNavNextMat()).name(t(config.getNavNextName())).asGuiItem(event -> gui.next()));

        gui.open(player);
    }

    public void openSanctionMenu(Player player, String targetName) {
        MainConfigManager config = plugin.getMainConfigManager();
        
        Gui gui = Gui.gui()
                .title(t(config.getGuiSanctionsTitle().replace("{target}", targetName)))
                .rows(3)
                .create();

        gui.setDefaultClickAction(event -> event.setCancelled(true));
        gui.getFiller().fillBorder(ItemBuilder.from(config.getBorderMaterial()).name(t(" ")).asGuiItem());

        gui.setItem(11, ItemBuilder.from(Material.IRON_SWORD).name(t(config.getGuiItemBanName()))
                .lore(config.getGuiItemBanLore().stream().map(this::t).collect(Collectors.toList()))
                .asGuiItem(event -> openReasonsMenu(player, targetName, "BAN")));

        gui.setItem(13, ItemBuilder.from(Material.PAPER).name(t(config.getGuiItemMuteName()))
                .lore(config.getGuiItemMuteLore().stream().map(this::t).collect(Collectors.toList()))
                .asGuiItem(event -> openReasonsMenu(player, targetName, "MUTE")));

        gui.setItem(15, ItemBuilder.from(Material.FEATHER).name(t(config.getGuiItemKickName()))
                .lore(config.getGuiItemKickLore().stream().map(this::t).collect(Collectors.toList()))
                .asGuiItem(event -> openReasonsMenu(player, targetName, "KICK")));

        gui.open(player);
    }

    public void openReasonsMenu(Player player, String targetName, String type) {
        MainConfigManager config = plugin.getMainConfigManager();
        ConfigurationSection section = config.getReasons(type);
        if (section == null) return;

        PaginatedGui gui = Gui.paginated()
                .title(t(config.getGuiReasonsTitle().replace("{type}", type).replace("{target}", targetName)))
                .rows(6)
                .pageSize(36) 
                .create();

        gui.setDefaultClickAction(event -> event.setCancelled(true));
        gui.getFiller().fillBorder(ItemBuilder.from(config.getBorderMaterial()).name(t(" ")).asGuiItem());

        for (String key : section.getKeys(false)) {
            String reasonName = config.getReasonName(type, key);
            List<String> durations = config.getReasonDurations(type, key);

            GuiItem reasonIndicator = ItemBuilder.from(config.getReasonMaterial(type, key))
                .name(t(config.getGuiReasonsItemName().replace("{reason}", reasonName)))
                .asGuiItem();
            
            gui.addItem(reasonIndicator);

            for (int d = 0; d < Math.min(durations.size(), 4); d++) {
                String dur = durations.get(d);
                gui.addItem(ItemBuilder.from(config.getDurationDye(d))
                    .name(t(config.getGuiReasonsDyeName().replace("{duration}", dur)))
                    .asGuiItem(event -> {
                        executePunishment(player, targetName, type, reasonName, dur);
                        player.closeInventory();
                    }));
            }
        }

        gui.setItem(49, ItemBuilder.from(config.getNavBackMat()).name(t(config.getNavBackName())).asGuiItem(event -> openSanctionMenu(player, targetName)));
        gui.open(player);
    }

    private void executePunishment(Player staff, String target, String type, String reason, String duration) {
        switch (type) {
            case "BAN" -> plugin.getPunishmentManager().banPlayer(staff, target, reason, duration);
            case "MUTE" -> plugin.getPunishmentManager().mutePlayer(staff, target, reason, duration);
            case "KICK" -> plugin.getPunishmentManager().kickPlayer(staff, target, reason);
        }
    }

    private ItemStack createPlayerHead(Player p) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtils.getColoredMessage("&a" + p.getName()));
            meta.setOwningPlayer(p);
            meta.setLore(List.of(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiHeadLore())));
            item.setItemMeta(meta);
        }
        return item;
    }
}