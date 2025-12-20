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

    // --- MENÚ 2: SELECCIÓN DE CATEGORÍA (BAN/MUTE/KICK) ---
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

    // --- MENÚ 3: MOTIVOS CON PAGINACIÓN ---
    public void openReasonsMenu(Player player, String targetName, String type, int page) {
        String title = MessageUtils.getColoredMessage("&8Sancionar - " + targetName + " (" + type + ")");
        Inventory gui = Bukkit.createInventory(null, 54, title);
        setupBorder(gui);
        
        ConfigurationSection section = plugin.getMainConfigManager().getReasons(type);
        if (section == null) return;

        List<String> keys = new ArrayList<>(section.getKeys(false));
        int itemsPerPage = 28;
        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, keys.size());

        int[] centralSlots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };

        for (int i = start; i < end; i++) {
            String key = keys.get(i);
            ItemStack item = new ItemStack(plugin.getMainConfigManager().getReasonMaterial(type, key));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getReasonName(type, key)));
            
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtils.getColoredMessage("&7ID: &f" + key));
            lore.add("");
            lore.add(MessageUtils.getColoredMessage("&eClick para ver duraciones"));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
            
            gui.setItem(centralSlots[i - start], item);
        }

        gui.setItem(45, createItem(Material.ARROW, plugin.getMainConfigManager().getNavBackName(), null));
        if (page > 0) gui.setItem(48, createItem(Material.PAPER, plugin.getMainConfigManager().getNavPrevName(), null));
        if (end < keys.size()) gui.setItem(50, createItem(Material.PAPER, plugin.getMainConfigManager().getNavNextName(), null));
        
        player.openInventory(gui);
    }

    // --- MENÚ 4: SELECCIÓN DE TIEMPO ESPECÍFICO ---
    public void openReasonDurationMenu(Player player, String targetName, String type, String reasonId) {
        String reasonName = plugin.getMainConfigManager().getReasonName(type, reasonId);
        String title = MessageUtils.getColoredMessage("&8Tiempo: " + type + " - " + reasonId + " : " + targetName);
        Inventory gui = Bukkit.createInventory(null, 27, title);
        setupBorder(gui);

        List<String> times = plugin.getMainConfigManager().getReasonDurations(type, reasonId);
        int[] slots = {10, 12, 14, 16}; 

        for (int i = 0; i < 4; i++) {
            String duration = (i < times.size()) ? times.get(i) : "perm";
            gui.setItem(slots[i], createItem(Material.CLOCK, "&a&lDuración: &f" + duration, 
                Arrays.asList("&7Sanción por: " + reasonName, "", "&eClick para aplicar sanción")));
        }
        
        gui.setItem(22, createItem(Material.ARROW, "&c« Volver a Motivos", null));

        player.openInventory(gui);
    }

    // --- UTILIDADES ---
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