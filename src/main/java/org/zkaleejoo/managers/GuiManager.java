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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;

import java.util.List;
import java.util.stream.Collectors;

public class GuiManager {

    private final MaxStaff plugin;

    public GuiManager(MaxStaff plugin) { 
        this.plugin = plugin; 
    }

    /**
     * MÉTODO DE COMPATIBILIDAD (1.19 - 1.21):
     * Crea un GuiItem usando el sistema nativo de Bukkit para evitar errores de versión.
     */
    private GuiItem createSafeGuiItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtils.getColoredMessage(name));
            if (lore != null) {
                meta.setLore(lore.stream()
                        .map(MessageUtils::getColoredMessage)
                        .collect(Collectors.toList()));
            }
            item.setItemMeta(meta);
        }
        return ItemBuilder.from(item).asGuiItem();
    }

    // Títulos de menú compatibles con Triumph-GUI (Adventure Component)
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
        
        // Borde compatible
        gui.getFiller().fillBorder(createSafeGuiItem(config.getBorderMaterial(), " ", null));

        long ticks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
        String playtime = (ticks / 72000) + "h " + ((ticks % 72000) / 1200) + "m";
        
        int bans = plugin.getPunishmentManager().getHistoryCount(target.getName(), "BAN");
        int mutes = plugin.getPunishmentManager().getHistoryCount(target.getName(), "MUTE");
        int kicks = plugin.getPunishmentManager().getHistoryCount(target.getName(), "KICK");
        int warns = plugin.getPunishmentManager().getHistoryCount(target.getName(), "WARN");

        // Estadísticas e Historial
        gui.setItem(11, createSafeGuiItem(config.getGuiInfoStatsMat(), config.getGuiInfoStatsName(), 
            config.getGuiInfoStatsLore().stream().map(l -> l.replace("{target}", target.getName()).replace("{playtime}", playtime)).collect(Collectors.toList())));

        gui.setItem(13, createSafeGuiItem(config.getGuiInfoHistoryMat(), config.getGuiInfoHistoryName(), 
            config.getGuiInfoHistoryLore().stream().map(l -> l.replace("{bans}", String.valueOf(bans)).replace("{mutes}", String.valueOf(mutes))).collect(Collectors.toList())));

        // Botón de Sancionar
        gui.setItem(15, ItemBuilder.from(createSafeGuiItem(config.getGuiInfoActionMat(), config.getGuiInfoActionName(), config.getGuiInfoActionLore()).getItemStack())
                .asGuiItem(event -> openSanctionMenu(staff, target.getName())));

        gui.open(staff);
    }

    public void openPlayersMenu(Player player) {
        MainConfigManager config = plugin.getMainConfigManager();
        PaginatedGui gui = Gui.paginated().title(t(config.getGuiPlayersTitle())).rows(6).pageSize(45).create();

        gui.setDefaultClickAction(event -> event.setCancelled(true));
        gui.getFiller().fillBottom(createSafeGuiItem(config.getBorderMaterial(), " ", null));

        for (Player target : Bukkit.getOnlinePlayers()) {
            gui.addItem(ItemBuilder.from(createPlayerHead(target)).asGuiItem(event -> {
                player.teleport(target);
                player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getMsgTeleport().replace("{player}", target.getName())));
            }));
        }

        gui.setItem(48, ItemBuilder.from(createSafeGuiItem(config.getNavPrevMat(), config.getNavPrevName(), null).getItemStack()).asGuiItem(event -> gui.previous()));
        gui.setItem(50, ItemBuilder.from(createSafeGuiItem(config.getNavNextMat(), config.getNavNextName(), null).getItemStack()).asGuiItem(event -> gui.next()));

        gui.open(player);
    }

    public void openSanctionMenu(Player player, String targetName) {
        MainConfigManager config = plugin.getMainConfigManager();
        Gui gui = Gui.gui().title(t(config.getGuiSanctionsTitle().replace("{target}", targetName))).rows(3).create();

        gui.setDefaultClickAction(event -> event.setCancelled(true));
        gui.getFiller().fillBorder(createSafeGuiItem(config.getBorderMaterial(), " ", null));

        gui.setItem(11, ItemBuilder.from(createSafeGuiItem(Material.IRON_SWORD, config.getGuiItemBanName(), config.getGuiItemBanLore()).getItemStack())
                .asGuiItem(event -> openReasonsMenu(player, targetName, "BAN")));

        gui.setItem(13, ItemBuilder.from(createSafeGuiItem(Material.PAPER, config.getGuiItemMuteName(), config.getGuiItemMuteLore()).getItemStack())
                .asGuiItem(event -> openReasonsMenu(player, targetName, "MUTE")));

        gui.setItem(15, ItemBuilder.from(createSafeGuiItem(Material.FEATHER, config.getGuiItemKickName(), config.getGuiItemKickLore()).getItemStack())
                .asGuiItem(event -> openReasonsMenu(player, targetName, "KICK")));

        gui.open(player);
    }

    public void openReasonsMenu(Player player, String targetName, String type) {
        MainConfigManager config = plugin.getMainConfigManager();
        ConfigurationSection section = config.getReasons(type);
        if (section == null) return;

        PaginatedGui gui = Gui.paginated().title(t(config.getGuiReasonsTitle().replace("{type}", type).replace("{target}", targetName))).rows(6).pageSize(36).create();

        gui.setDefaultClickAction(event -> event.setCancelled(true));
        gui.getFiller().fillBorder(createSafeGuiItem(config.getBorderMaterial(), " ", null));

        for (String key : section.getKeys(false)) {
            String reasonName = config.getReasonName(type, key);
            List<String> durations = config.getReasonDurations(type, key);

            gui.addItem(createSafeGuiItem(config.getReasonMaterial(type, key), config.getGuiReasonsItemName().replace("{reason}", reasonName), null));

            for (int d = 0; d < Math.min(durations.size(), 4); d++) {
                String dur = durations.get(d);
                gui.addItem(ItemBuilder.from(createSafeGuiItem(config.getDurationDye(d), config.getGuiReasonsDyeName().replace("{duration}", dur), null).getItemStack())
                    .asGuiItem(event -> {
                        executePunishment(player, targetName, type, reasonName, dur);
                        player.closeInventory();
                    }));
            }
        }
        gui.setItem(49, ItemBuilder.from(createSafeGuiItem(config.getNavBackMat(), config.getNavBackName(), null).getItemStack()).asGuiItem(event -> openSanctionMenu(player, targetName)));
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