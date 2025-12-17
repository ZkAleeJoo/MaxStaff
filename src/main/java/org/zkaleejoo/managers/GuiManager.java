package org.zkaleejoo.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

import java.util.Arrays;
import java.util.List;

public class GuiManager {

    private final MaxStaff plugin;

    public GuiManager(MaxStaff plugin) {
        this.plugin = plugin;
    }

    // --- MENÚ 1: JUGADORES ONLINE ---
    public void openPlayersMenu(Player player) {
        String title = plugin.getMainConfigManager().getGuiPlayersTitle();
        Inventory gui = Bukkit.createInventory(null, 54, MessageUtils.getColoredMessage(title));

        for (Player target : Bukkit.getOnlinePlayers()) {
            gui.addItem(createPlayerHead(target));
        }

        player.openInventory(gui);
    }

    // --- MENÚ 2: TIPOS DE SANCIÓN ---
    public void openSanctionMenu(Player player, String targetName) {
        String title = plugin.getMainConfigManager().getGuiSanctionsTitle().replace("{target}", targetName);
        Inventory gui = Bukkit.createInventory(null, 27, MessageUtils.getColoredMessage(title));

        // Usamos los items configurables
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

    // --- MENÚ 3: DURACIÓN (TIEMPO) ---
    public void openTimeMenu(Player player, String targetName, String type) {
        String title = plugin.getMainConfigManager().getGuiDurationTitle()
            .replace("{type}", type).replace("{target}", targetName);
        Inventory gui = Bukkit.createInventory(null, 27, MessageUtils.getColoredMessage(title));

        gui.setItem(10, createItem(Material.LIME_DYE, plugin.getMainConfigManager().getGuiTime1hName(), 
            java.util.Collections.singletonList(plugin.getMainConfigManager().getGuiTime1hLore())));
            
        gui.setItem(12, createItem(Material.YELLOW_DYE, plugin.getMainConfigManager().getGuiTime1dName(), 
            java.util.Collections.singletonList(plugin.getMainConfigManager().getGuiTime1dLore())));
            
        gui.setItem(14, createItem(Material.ORANGE_DYE, plugin.getMainConfigManager().getGuiTime7dName(), 
            java.util.Collections.singletonList(plugin.getMainConfigManager().getGuiTime7dLore())));
            
        gui.setItem(16, createItem(Material.RED_DYE, plugin.getMainConfigManager().getGuiTimePermName(), 
            java.util.Collections.singletonList(plugin.getMainConfigManager().getGuiTimePermLore())));

        gui.setItem(26, createItem(Material.ARROW, plugin.getMainConfigManager().getGuiBackName(), 
            new java.util.ArrayList<>())); // Lore vacío para el botón volver

        player.openInventory(gui);
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.getColoredMessage(name));
        if (lore != null && !lore.isEmpty()) {
            List<String> loreList = new java.util.ArrayList<>();
            for (String l : lore) loreList.add(MessageUtils.getColoredMessage(l));
            meta.setLore(loreList);
        }
        item.setItemMeta(meta);
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