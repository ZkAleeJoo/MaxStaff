package org.zkaleejoo.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey; 
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType; 
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.Sound;

public class StaffItemsListener implements Listener {

    private final MaxStaff plugin;
    private final Map<UUID, Block> silentViewers = new HashMap<>();

    public StaffItemsListener(MaxStaff plugin) {
        this.plugin = plugin;
        
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.getStaffManager().isInStaffMode(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getCannotDrop()));
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (plugin.getStaffManager().isInStaffMode(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getCannotPlace()));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Player player = event.getPlayer();
        if (!plugin.getStaffManager().isInStaffMode(player)) return;

        ItemStack item = event.getItem();
        MainConfigManager config = plugin.getMainConfigManager();
        String toolType = getStaffToolType(item);

        if (toolType != null) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                
                if (toolType.equals("vanish_tool")) {
                    event.setCancelled(true); 
                    plugin.getStaffManager().toggleVanish(player);
                }
                else if (toolType.equals("players_tool")) {
                    event.setCancelled(true);
                    plugin.getGuiManager().openPlayersMenu(player);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
                    player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getMsgPlayers()));
                }
                else if (toolType.equals("punish_tool")) {
                    event.setCancelled(true);
                    org.bukkit.util.RayTraceResult ray = player.getWorld().rayTraceEntities(
                        player.getEyeLocation(), 
                        player.getEyeLocation().getDirection(), 
                        5, 
                        entity -> entity instanceof org.bukkit.entity.Player && !entity.equals(player)
                    );
                    
                    if (ray == null || ray.getHitEntity() == null) {
                        player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getPlayerClickPls()));
                    }
                }
                return; 
            }
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null) {
                if (block.getState() instanceof Container || block.getType() == Material.ENDER_CHEST) {
                    event.setCancelled(true);

                    if (block.getType() == Material.ENDER_CHEST) {
                        player.openInventory(player.getEnderChest());
                    } else {
                        Container container = (Container) block.getState();
                        Inventory realInv = container.getInventory();
                        
                        String title = container.getCustomName() != null ? container.getCustomName() : container.getType().name();
                        
                        Inventory silentInv = Bukkit.createInventory(null, realInv.getSize(), title);
                        silentInv.setContents(realInv.getContents());
                        
                        silentViewers.put(player.getUniqueId(), block);
                        player.openInventory(silentInv);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!plugin.getStaffManager().isInStaffMode(player)) return;
        if (!(event.getRightClicked() instanceof Player)) return;

        Player target = (Player) event.getRightClicked();
        ItemStack item = player.getInventory().getItemInMainHand();
        MainConfigManager config = plugin.getMainConfigManager();

        String toolType = getStaffToolType(item);
        if (toolType == null) return;

        if (toolType.equals("inspect_tool")) {
            event.setCancelled(true);
            String msg = config.getMsgInspect().replace("{player}", target.getName());
            player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + msg));
            player.openInventory(target.getInventory());
        }
        else if (toolType.equals("punish_tool")) {
            event.setCancelled(true);
            plugin.getGuiManager().openUserInfoMenu(player, target);
        }
        else if (toolType.equals("freeze_tool")) {
            event.setCancelled(true);
            plugin.getFreezeManager().toggleFreeze(player, target);
        }   
    }


    private String getStaffToolType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, "staff_tool");
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (silentViewers.containsKey(uuid)) {
            Block block = silentViewers.remove(uuid); 
            if (block.getState() instanceof Container) {
                Container container = (Container) block.getState();
                container.getInventory().setContents(event.getInventory().getContents());
            }
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (plugin.getStaffManager().isInStaffMode(player)) {
            event.setCancelled(true); 
        }
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        silentViewers.remove(event.getPlayer().getUniqueId());
    }

}