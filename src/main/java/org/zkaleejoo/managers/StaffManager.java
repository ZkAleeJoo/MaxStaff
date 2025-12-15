package org.zkaleejoo.managers;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StaffManager {

    private final MaxStaff plugin;
    private final Map<UUID, ItemStack[]> savedInventory = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    
    private final Map<UUID, Boolean> staffModePlayers = new HashMap<>();

    public StaffManager(MaxStaff plugin) {
        this.plugin = plugin;
    }

    public void toggleStaffMode(Player player) {
        if (isInStaffMode(player)) {
            disableStaffMode(player);
        } else {
            enableStaffMode(player);
        }
    }

    public void enableStaffMode(Player player) {
        savedInventory.put(player.getUniqueId(), player.getInventory().getContents());
        savedArmor.put(player.getUniqueId(), player.getInventory().getArmorContents());
        
        player.getInventory().clear();
        player.setGameMode(GameMode.CREATIVE); 
        
        giveStaffItems(player);
        
        staffModePlayers.put(player.getUniqueId(), true);
        player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + "&aStaff Mode Activated. Inventory saved."));
        
    }

    public void disableStaffMode(Player player) {
        player.getInventory().clear();
        
        if (savedInventory.containsKey(player.getUniqueId())) {
            player.getInventory().setContents(savedInventory.get(player.getUniqueId()));
            player.getInventory().setArmorContents(savedArmor.get(player.getUniqueId()));
            
            savedInventory.remove(player.getUniqueId());
            savedArmor.remove(player.getUniqueId());
        }
        
        player.setGameMode(GameMode.SURVIVAL);
        
        staffModePlayers.remove(player.getUniqueId());
        player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + "&cStaff Mode Disabled. Inventory Restored."));
    }

    public boolean isInStaffMode(Player player) {
        return staffModePlayers.containsKey(player.getUniqueId());
    }

    private void giveStaffItems(Player player) {
        player.getInventory().setItem(0, createItem(Material.BOOK, "&c&lSanctions"));
        
        player.getInventory().setItem(4, createItem(Material.CLOCK, "&b&lOnline Players"));
        
        player.getInventory().setItem(7, createItem(Material.CHEST, "&6&lInspect Inventory"));
        
        player.getInventory().setItem(8, createItem(Material.NETHER_STAR, "&a&lVanish (Alternate)"));
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtils.getColoredMessage(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    //LOGICA DEL VANISH
    public void toggleVanish(Player player) {
        if (!staffModePlayers.containsKey(player.getUniqueId())) return;


        if (isVanished(player)) {
            setVanish(player, false);
            player.sendMessage(MessageUtils.getColoredMessage("&7Vanish: &cDISABLED"));
        } else {
            setVanish(player, true);
            player.sendMessage(MessageUtils.getColoredMessage("&aVanish: &ACTIVATED"));
        }
    }

    private final java.util.List<UUID> vanishedPlayers = new java.util.ArrayList<>();

    public void setVanish(Player player, boolean enable) {
        if (enable) {
            vanishedPlayers.add(player.getUniqueId());
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.hasPermission("maxstaff.see.vanish")) {  //PERMISO
                    target.hidePlayer(plugin, player);
                }
            }

        } else {
            vanishedPlayers.remove(player.getUniqueId());
            for (Player target : Bukkit.getOnlinePlayers()) {
                target.showPlayer(plugin, player);
            }
        }
    }

    public boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }

}