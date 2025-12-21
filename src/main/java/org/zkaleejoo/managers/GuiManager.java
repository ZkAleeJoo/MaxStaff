package org.zkaleejoo.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiManager {

    private final MaxStaff plugin;

    public GuiManager(MaxStaff plugin) {
        this.plugin = plugin;
    }

    // --- DISEÑO: BORDES PROFESIONALES ---
    private void setupBorder(Inventory inv) {
        ItemStack border = createItem(plugin.getMainConfigManager().getBorderMaterial(), " ", null);
        int size = inv.getSize();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(size - 9 + i, border);
        }
        for (int i = 0; i < size; i += 9) {
            inv.setItem(i, border);
            inv.setItem(i + 8, border);
        }
    }

    // --- MENÚ 1: JUGADORES ONLINE ---
    public void openPlayersMenu(Player player) {
        String title = MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiPlayersTitle());
        Inventory gui = Bukkit.createInventory(null, 54, title);
        setupBorder(gui);

        for (Player target : Bukkit.getOnlinePlayers()) {
            gui.addItem(createPlayerHead(target));
        }

        player.openInventory(gui);
    }

    // --- MENÚ 2: SELECCIÓN DE CATEGORÍA ---
    public void openSanctionMenu(Player player, String targetName) {
        String title = MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiSanctionsTitle().replace("{target}", targetName));
        Inventory gui = Bukkit.createInventory(null, 36, title);
        setupBorder(gui);

        gui.setItem(11, createItem(Material.IRON_SWORD, 
            plugin.getMainConfigManager().getGuiItemBanName(), 
            plugin.getMainConfigManager().getGuiItemBanLore()));
            
        gui.setItem(13, createItem(Material.PAPER, 
            plugin.getMainConfigManager().getGuiItemMuteName(), 
            plugin.getMainConfigManager().getGuiItemMuteLore()));
            
        gui.setItem(15, createItem(Material.FEATHER, 
            plugin.getMainConfigManager().getGuiItemKickName(), 
            plugin.getMainConfigManager().getGuiItemKickLore()));

        player.openInventory(gui);
    }

    // --- MENÚ 3: MOTIVOS CON TIEMPOS ---
    public void openReasonsMenu(Player player, String targetName, String type, int page) {
        String title = MessageUtils.getColoredMessage("&8Sancionar - " + targetName + " (" + (page + 1) + "/3)");
        Inventory gui = Bukkit.createInventory(null, 54, title);
        setupBorder(gui);
        
        ConfigurationSection section = plugin.getMainConfigManager().getReasons(type);
        if (section == null) return;

        List<String> keys = new ArrayList<>(section.getKeys(false));
        int itemsPerPage = 4; 
        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, keys.size());

        int[] rowStarts = {10, 19, 28, 37};

        for (int i = start; i < end; i++) {
            String key = keys.get(i);
            int rowIdx = i - start;
            int baseSlot = rowStarts[rowIdx];
            
            ItemStack reasonItem = new ItemStack(plugin.getMainConfigManager().getReasonMaterial(type, key));
            ItemMeta rMeta = reasonItem.getItemMeta();
            rMeta.setDisplayName(MessageUtils.getColoredMessage("&c&lSanción #" + (i + 1)));
            rMeta.setLore(Arrays.asList(
                MessageUtils.getColoredMessage("&7Motivo: &f" + plugin.getMainConfigManager().getReasonName(type, key)),
                MessageUtils.getColoredMessage("&8ID: " + key)
            ));
            rMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            reasonItem.setItemMeta(rMeta);
            gui.setItem(baseSlot, reasonItem);

            List<String> durations = plugin.getMainConfigManager().getReasonDurations(type, key);
            int[] dyeOffsets = {2, 3, 4, 5}; 

            for (int d = 0; d < 4; d++) {
                String duration = (d < durations.size()) ? durations.get(d) : "perm";
                
                Material dyeMat = getDyeColor(duration);
                
                ItemStack dye = new ItemStack(dyeMat);
                ItemMeta dMeta = dye.getItemMeta();
                dMeta.setDisplayName(MessageUtils.getColoredMessage("&eTiempo: &f" + duration));
                dMeta.setLore(Arrays.asList(
                    MessageUtils.getColoredMessage("&7Tipo: &6" + type),
                    MessageUtils.getColoredMessage("&7Motivo: &f" + plugin.getMainConfigManager().getReasonName(type, key)),
                    MessageUtils.getColoredMessage("&8ID: " + key),
                    MessageUtils.getColoredMessage("&8TimeValue: " + duration),
                    "",
                    MessageUtils.getColoredMessage("&eClick para aplicar sanción")
                ));
                dye.setItemMeta(dMeta);
                gui.setItem(baseSlot + dyeOffsets[d], dye);
            }
        }

        gui.setItem(49, createItem(Material.BOOK, "&c&l« Volver", Arrays.asList("&7Regresar a categorías")));

        if (page > 0) {
            gui.setItem(45, createItem(Material.ARROW, "&e« Anterior", Arrays.asList("&7Página " + page)));
        }
        if (end < keys.size()) {
            gui.setItem(53, createItem(Material.ARROW, "&aSiguiente »", Arrays.asList("&7Página " + (page + 2))));
        }
        
        player.openInventory(gui);
    }

    private Material getDyeColor(String duration) {
        String d = duration.toLowerCase();
        if (d.contains("m") || d.contains("h")) return Material.LIME_DYE;      
        if (d.equals("1d") || d.equals("2d")) return Material.YELLOW_DYE;    
        if (d.contains("d") || d.contains("w")) return Material.ORANGE_DYE; 
        if (d.contains("perm") || d.contains("permanent")) return Material.RED_DYE; 
        return Material.GRAY_DYE;
    }

    public void openReasonDurationMenu(Player player, String targetName, String type, String reasonId) {
        openReasonsMenu(player, targetName, type, 0);
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtils.getColoredMessage(name));
            if (lore != null) {
                List<String> coloredLore = new ArrayList<>();
                for (String l : lore) coloredLore.add(MessageUtils.getColoredMessage(l));
                meta.setLore(coloredLore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPlayerHead(Player p) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setDisplayName(MessageUtils.getColoredMessage("&a" + p.getName()));
        meta.setOwningPlayer(p);
        meta.setLore(Arrays.asList(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiHeadLore())));
        item.setItemMeta(meta);
        return item;
    }
}