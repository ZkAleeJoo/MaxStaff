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

    public GuiManager(MaxStaff plugin) { this.plugin = plugin; }

    private void setupBorder(Inventory inv) {
        ItemStack border = createItem(plugin.getMainConfigManager().getBorderMaterial(), " ", null);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(inv.getSize() - 9 + i, border);
        }
        for (int i = 0; i < inv.getSize(); i += 9) {
            inv.setItem(i, border);
            inv.setItem(i + 8, border);
        }
    }

    public void openPlayersMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiPlayersTitle()));
        setupBorder(gui);
        for (Player target : Bukkit.getOnlinePlayers()) gui.addItem(createPlayerHead(target));
        player.openInventory(gui);
    }

    public void openSanctionMenu(Player player, String targetName) {
        Inventory gui = Bukkit.createInventory(null, 36, MessageUtils.getColoredMessage(plugin.getMainConfigManager().getGuiSanctionsTitle().replace("{target}", targetName)));
        setupBorder(gui);
        gui.setItem(11, createItem(Material.IRON_SWORD, plugin.getMainConfigManager().getGuiItemBanName(), plugin.getMainConfigManager().getGuiItemBanLore()));
        gui.setItem(13, createItem(Material.PAPER, plugin.getMainConfigManager().getGuiItemMuteName(), plugin.getMainConfigManager().getGuiItemMuteLore()));
        gui.setItem(15, createItem(Material.FEATHER, plugin.getMainConfigManager().getGuiItemKickName(), plugin.getMainConfigManager().getGuiItemKickLore()));
        player.openInventory(gui);
    }

    public void openReasonsMenu(Player player, String targetName, String type, int page) {
        String titleTemplate = plugin.getConfig().getString("sanctions-menu", "&8Sancionar [{type}] - {target} ({page}/{total})");
        String title = MessageUtils.getColoredMessage(titleTemplate
                .replace("{type}", type)
                .replace("{target}", targetName)
                .replace("{page}", String.valueOf(page + 1))
                .replace("{total}", "3")); 
        
        Inventory gui = Bukkit.createInventory(null, 54, title);
        setupBorder(gui);
        
        ConfigurationSection section = plugin.getMainConfigManager().getReasons(type);
        if (section == null) return;

        List<String> keys = new ArrayList<>(section.getKeys(false));
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
            rItem.setItemMeta(rMeta);
            gui.setItem(baseSlot, rItem);

            List<String> durations = plugin.getMainConfigManager().getReasonDurations(type, key);
            for (int d = 0; d < 4; d++) {
                String dur = (d < durations.size()) ? durations.get(d) : "perm";
                ItemStack dye = new ItemStack(plugin.getMainConfigManager().getDurationDye(d));
                ItemMeta dMeta = dye.getItemMeta();
                
                String dName = plugin.getMainConfigManager().getGuiReasonsDyeName()
                        .replace("{duration}", dur);
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

        gui.setItem(49, createItem(plugin.getMainConfigManager().getNavBackMat(), plugin.getMainConfigManager().getNavBackName(), Arrays.asList(MessageUtils.getColoredMessage("&7Regresar"))));
        if (page > 0) gui.setItem(45, createItem(plugin.getMainConfigManager().getNavPrevMat(), plugin.getMainConfigManager().getNavPrevName(), Arrays.asList(MessageUtils.getColoredMessage("&7Página " + page))));
        if (end < keys.size()) gui.setItem(53, createItem(plugin.getMainConfigManager().getNavNextMat(), plugin.getMainConfigManager().getNavNextName(), Arrays.asList(MessageUtils.getColoredMessage("&7Página " + (page + 2)))));
        player.openInventory(gui);
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
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