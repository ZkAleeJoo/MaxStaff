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
        Inventory gui = Bukkit.createInventory(null, 54, MessageUtils.getColoredMessage("&8Lista de Jugadores"));

        for (Player target : Bukkit.getOnlinePlayers()) {
            gui.addItem(createPlayerHead(target));
        }

        player.openInventory(gui);
    }

    // --- MENÚ 2: TIPOS DE SANCIÓN ---
    public void openSanctionMenu(Player player, String targetName) {
        Inventory gui = Bukkit.createInventory(null, 27, MessageUtils.getColoredMessage("&8Sancionar a: &0" + targetName));

        gui.setItem(11, createItem(Material.IRON_SWORD, "&c&lBANEAR", "&7Clic para elegir tiempo"));
        gui.setItem(13, createItem(Material.PAPER, "&e&lMUTEAR", "&7Clic para elegir tiempo"));
        gui.setItem(15, createItem(Material.FEATHER, "&b&lKICKEAR", "&7Clic para expulsar ahora"));

        player.openInventory(gui);
    }

    // --- MENÚ 3: DURACIÓN (TIEMPO) ---
    public void openTimeMenu(Player player, String targetName, String type) {
        Inventory gui = Bukkit.createInventory(null, 27, MessageUtils.getColoredMessage("&8Duración " + type + ": &0" + targetName));

        gui.setItem(10, createItem(Material.LIME_DYE, "&a1 Hora", "&7Duración: 1h", "time:1h"));
        gui.setItem(12, createItem(Material.YELLOW_DYE, "&e1 Día", "&7Duración: 1d", "time:1d"));
        gui.setItem(14, createItem(Material.ORANGE_DYE, "&67 Días", "&7Duración: 7d", "time:7d"));
        gui.setItem(16, createItem(Material.RED_DYE, "&4Permanente", "&7Duración: Perm", "time:perm"));

        gui.setItem(26, createItem(Material.ARROW, "&cVolver", "back"));

        player.openInventory(gui);
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.getColoredMessage(name));
        if (lore.length > 0) {
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
        meta.setLore(Arrays.asList(MessageUtils.getColoredMessage("&7Clic para teletransportarte")));
        item.setItemMeta(meta);
        return item;
    }
}