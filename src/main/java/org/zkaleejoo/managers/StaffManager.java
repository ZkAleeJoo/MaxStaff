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
import org.zkaleejoo.config.CustomConfig;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StaffManager {

    private final MaxStaff plugin;
    private final CustomConfig staffData;
    private final Map<UUID, ItemStack[]> savedInventory = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private final Map<UUID, GameMode> savedGameMode = new HashMap<>();
    private final Map<UUID, Boolean> staffModePlayers = new HashMap<>();
    private final java.util.List<UUID> vanishedPlayers = new java.util.ArrayList<>();
    private final Set<UUID> commandSpyPlayers = new HashSet<>();    

    public StaffManager(MaxStaff plugin) {
        this.plugin = plugin;
        this.staffData = new CustomConfig("staff_data.yml", null, plugin, true);
        this.staffData.registerConfig();
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
        UUID uuid = player.getUniqueId();
        
        ItemStack[] contents = player.getInventory().getContents();
        String gm = player.getGameMode().name();
        int foodLevel = player.getFoodLevel();
        
        savedInventory.put(uuid, contents);
        
        staffData.getConfig().set("data." + uuid + ".inventory", contents);
        staffData.getConfig().set("data." + uuid + ".gamemode", gm);
        staffData.getConfig().set("data." + uuid + ".food", foodLevel);
        staffData.saveConfig(); 

        player.getInventory().clear();
        player.setFoodLevel(20);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);
        
        giveStaffItems(player);
        staffModePlayers.put(uuid, true);
        
        setVanish(player, true); 
        player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getMsgVanishOn()));
        player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getStaffModeEnabled()));
        player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getInventorySaved()));

        new org.bukkit.scheduler.BukkitRunnable() {
        @Override
        public void run() {
            if (!isInStaffMode(player) || !player.isOnline()) {
                this.cancel();
                return;
            }

            MainConfigManager config = plugin.getMainConfigManager();
            
            String statusText = isVanished(player) ? config.getStatusEnabled() : config.getStatusDisabled();
            
            String message = MessageUtils.getColoredMessage(
                config.getMsgActionBar().replace("{status}", statusText)
            );
            
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                new net.md_5.bungee.api.chat.TextComponent(message));
        }
    }.runTaskTimer(plugin, 0L, 20L); 
    }

        public void disableStaffMode(Player player) {
        MainConfigManager config = plugin.getMainConfigManager();
        UUID uuid = player.getUniqueId();
        
        player.getInventory().clear();
        
        ItemStack[] toRestore = null;

        if (savedInventory.containsKey(uuid)) {
            toRestore = savedInventory.remove(uuid);
        } 
        else if (staffData.getConfig().contains("data." + uuid)) {
            List<?> list = staffData.getConfig().getList("data." + uuid + ".inventory");
            if (list != null) {
                toRestore = list.toArray(new ItemStack[0]);
            }
        }

        if (toRestore != null) {
            player.getInventory().setContents(toRestore);
            
            String gmName = staffData.getConfig().getString("data." + uuid + ".gamemode", "SURVIVAL");
            try {
                GameMode originalMode = GameMode.valueOf(gmName);
                player.setGameMode(originalMode);
                player.setAllowFlight(originalMode == GameMode.CREATIVE || originalMode == GameMode.SPECTATOR);
            } catch (IllegalArgumentException e) {
                player.setGameMode(GameMode.SURVIVAL);
            }

            int originalFood = staffData.getConfig().getInt("data." + uuid + ".food", 20);
            player.setFoodLevel(originalFood);
        }

        staffData.getConfig().set("data." + uuid, null);
        staffData.saveConfig();
        
        player.setFlying(false);
        player.setInvulnerable(false);
        staffModePlayers.remove(uuid);
        
        if (isVanished(player)) {
            setVanish(player, false);
        }

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

        if (isVanished(player)) {
            setVanish(player, false);
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgVanishOff()));
        } else {
            setVanish(player, true);
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgVanishOn()));
        }
    }

    public void setVanish(Player player, boolean enable) {
        if (enable) {
            vanishedPlayers.add(player.getUniqueId());
            
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
            
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.hasPermission("maxstaff.see.vanish")) {
                    target.hidePlayer(plugin, player);
                }
            }
        } else {
            vanishedPlayers.remove(player.getUniqueId());
            
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
            
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

    public void toggleCommandSpy(Player player) {
        if (commandSpyPlayers.contains(player.getUniqueId())) {
            commandSpyPlayers.remove(player.getUniqueId());
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgCmdSpyDisabled()));
        } else {
            commandSpyPlayers.add(player.getUniqueId());
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgCmdSpyEnabled()));
        }
    }

    public boolean isSpying(Player player) {
        return commandSpyPlayers.contains(player.getUniqueId());
    }

    public void updateSavedGameMode(Player player, org.bukkit.GameMode newMode) {
        if (savedGameMode.containsKey(player.getUniqueId())) {
            savedGameMode.put(player.getUniqueId(), newMode);
        }
    }

    public boolean hasPersistentStaffData(UUID uuid) {
        return staffData.getConfig().contains("data." + uuid);
    }
}