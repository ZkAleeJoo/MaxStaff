package org.zkaleejoo.managers;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey; 
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType; 
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StaffManager {

    private final MaxStaff plugin;
    private final Map<UUID, ItemStack[]> savedInventory = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private final Map<UUID, GameMode> savedGameMode = new HashMap<>();
    private final Map<UUID, Boolean> staffModePlayers = new HashMap<>();
    private final java.util.List<UUID> vanishedPlayers = new java.util.ArrayList<>();

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
        MainConfigManager config = plugin.getMainConfigManager();
        
        savedInventory.put(player.getUniqueId(), player.getInventory().getContents());
        savedArmor.put(player.getUniqueId(), player.getInventory().getArmorContents());
        savedGameMode.put(player.getUniqueId(), player.getGameMode());
        
        player.getInventory().clear();

        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);
        
        giveStaffItems(player);
        
        staffModePlayers.put(player.getUniqueId(), true);
        
        player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getStaffModeEnabled()));
        player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getInventorySaved()));
    }

    public void disableStaffMode(Player player) {
        MainConfigManager config = plugin.getMainConfigManager();
        player.getInventory().clear();
        
        if (savedInventory.containsKey(player.getUniqueId())) {
            player.getInventory().setContents(savedInventory.get(player.getUniqueId()));
            player.getInventory().setArmorContents(savedArmor.get(player.getUniqueId()));
            
            savedInventory.remove(player.getUniqueId());
            savedArmor.remove(player.getUniqueId());
        }
        
        GameMode originalMode = savedGameMode.getOrDefault(player.getUniqueId(), GameMode.SURVIVAL);
        player.setGameMode(originalMode);
        savedGameMode.remove(player.getUniqueId()); 
        
        player.setAllowFlight(originalMode == GameMode.CREATIVE || originalMode == GameMode.SPECTATOR);
        player.setFlying(false);
        player.setInvulnerable(false);
        
        staffModePlayers.remove(player.getUniqueId());
        
        player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getStaffModeDisabled()));
        player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getInventoryRestored()));
    }

    public boolean isInStaffMode(Player player) {
        return staffModePlayers.containsKey(player.getUniqueId());
    }

    private void giveStaffItems(Player player) {
        MainConfigManager config = plugin.getMainConfigManager();
        player.getInventory().setItem(0, createItem(config.getMatPunish(), config.getItemNamePunish(), "punish_tool"));
        player.getInventory().setItem(1, createItem(config.getMatFreeze(), config.getItemNameFreeze(), "freeze_tool"));
        player.getInventory().setItem(4, createItem(config.getMatPlayers(), config.getItemNamePlayers(), "players_tool"));
        player.getInventory().setItem(7, createItem(config.getMatInspect(), config.getItemNameInspect(), "inspect_tool"));
        player.getInventory().setItem(8, createItem(config.getMatVanish(), config.getItemNameVanish(), "vanish_tool"));
    }

    private ItemStack createItem(Material material, String name, String toolKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtils.getColoredMessage(name));
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

            NamespacedKey key = new NamespacedKey(plugin, "staff_tool");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, toolKey);

            item.setItemMeta(meta);
        }
        return item;
    }

    public void toggleVanish(Player player) {
        if (!staffModePlayers.containsKey(player.getUniqueId())) return;

        if (isVanished(player)) {
            setVanish(player, false);
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getMsgVanishOff()));
        } else {
            setVanish(player, true);
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getMsgVanishOn()));
        }
    }

    public void setVanish(Player player, boolean enable) {
        if (enable) {
            vanishedPlayers.add(player.getUniqueId());
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.hasPermission("maxstaff.see.vanish")) {
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

    public java.util.List<UUID> getVanishedPlayers() {
        return vanishedPlayers;
    }

    public void disableAllStaff() {
        for (UUID uuid : new java.util.ArrayList<>(staffModePlayers.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                disableStaffMode(player);
            }
        }
    }
}