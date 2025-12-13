package org.zkaleejoo.maxstaff.managers;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.zkaleejoo.maxstaff.MaxStaff;
import org.zkaleejoo.maxstaff.utils.ItemBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StaffModeManager {

    private final MaxStaff plugin;
    private final Map<UUID, ItemStack[]> savedInventory = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();

    public StaffModeManager(MaxStaff plugin) {
        this.plugin = plugin;
    }

    public void toggleStaffMode(Player player) {
        if (isInStaffMode(player)) {
            disableStaffMode(player);
        } else {
            enableStaffMode(player);
        }
    }

    public boolean isInStaffMode(Player player) {
        return savedInventory.containsKey(player.getUniqueId());
    }

    private void enableStaffMode(Player player) {
        savedInventory.put(player.getUniqueId(), player.getInventory().getContents());
        savedArmor.put(player.getUniqueId(), player.getInventory().getArmorContents());

        player.getInventory().clear();
        player.setGameMode(GameMode.CREATIVE);
        player.sendMessage("§a§lSTAFF » §fModo Staff §aACTIVADO§f.");
        
        for (Player target : Bukkit.getOnlinePlayers()) {
            target.hidePlayer(plugin, player); 
        }

        giveStaffItems(player);
    }

    private void disableStaffMode(Player player) {
        player.getInventory().clear();

        if (savedInventory.containsKey(player.getUniqueId())) {
            player.getInventory().setContents(savedInventory.get(player.getUniqueId()));
            player.getInventory().setArmorContents(savedArmor.get(player.getUniqueId()));
            
            savedInventory.remove(player.getUniqueId());
            savedArmor.remove(player.getUniqueId());
        }

        for (Player target : Bukkit.getOnlinePlayers()) {
            target.showPlayer(plugin, player);
        }

        player.setGameMode(GameMode.SURVIVAL); 
        player.sendMessage("§c§lSTAFF » §fModo Staff §cDESACTIVADO§f.");
    }

    private void giveStaffItems(Player player) {
        ItemStack freezeTool = new ItemBuilder(Material.PACKED_ICE)
                .name("&b&lCongelador &7(Click Derecho)")
                .lore("&7Usa esto para congelar", "&7a jugadores sospechosos.")
                .build();

        ItemStack inspectTool = new ItemBuilder(Material.BOOK)
                .name("&e&lInspeccionar Inventario")
                .lore("&7Click derecho a un jugador", "&7para ver sus items.")
                .build();

        ItemStack vanishTool = new ItemBuilder(Material.CLOCK)
                .name("&a&lTeletransporte Aleatorio")
                .lore("&7Click para ir a un jugador random.")
                .build();

        player.getInventory().setItem(0, inspectTool); 
        player.getInventory().setItem(1, freezeTool);  
        player.getInventory().setItem(8, vanishTool); 
    }
}