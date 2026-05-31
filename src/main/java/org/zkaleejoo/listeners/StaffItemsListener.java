package org.zkaleejoo.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
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
import org.zkaleejoo.utils.InspectionInventoryBuilder;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.util.BlockIterator;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.PlayerInventory;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class StaffItemsListener implements Listener {

    private final MaxStaff plugin;
    private static final long WALL_COMPASS_COOLDOWN_MS = 350L;

    private final Map<UUID, Block> silentViewers = new HashMap<>();
    private final Map<Block, UUID> activeInspections = new HashMap<>();
    private final Map<UUID, Long> wallCompassCooldowns = new HashMap<>();
    private final Set<UUID> silentEnderChestViewers = new HashSet<>();

    public StaffItemsListener(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (plugin.getStaffManager().isInStaffMode(player)) {
            ItemStack item = event.getItemDrop().getItemStack();

            String toolType = getStaffToolType(item);

            if (toolType != null) {
                event.setCancelled(true);
                player.sendMessage(MessageUtils.getColoredMessage(
                        plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getCannotDrop()));
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (plugin.getStaffManager().isInStaffMode(player)) {
            ItemStack itemInHand = event.getItemInHand();

            String toolType = getStaffToolType(itemInHand);

            if (toolType != null) {
                event.setCancelled(true);

                player.sendMessage(MessageUtils.getColoredMessage(
                        plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getCannotPlace()));
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        Player player = event.getPlayer();
        if (!plugin.getStaffManager().isInStaffMode(player))
            return;

        ItemStack item = event.getItem();
        MainConfigManager config = plugin.getMainConfigManager();
        String toolType = getStaffToolType(item);

        if (toolType != null) {
            Action action = event.getAction();

            if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                if (toolType.equals("wall_compass_tool")) {
                    event.setCancelled(true);
                    if (!isWallCompassReady(player))
                        return;

                    int maxDistance = config.getWallCompassRange();
                    Location thruLoc = findThruLocation(player, maxDistance);

                    if (thruLoc != null) {
                        player.teleport(thruLoc);
                        Location _loc = player.getLocation(); if (_loc != null) player.playSound(_loc, Objects.requireNonNull(Sound.ENTITY_ENDERMAN_TELEPORT), 1.0f, 1.0f);
                    } else {
                        Location _loc2 = player.getLocation(); if (_loc2 != null) player.playSound(_loc2, Objects.requireNonNull(Sound.BLOCK_NOTE_BLOCK_BASS), 0.9f, 0.5f);
                    }
                }
                return;
            }

            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {

                if (toolType.equals("vanish_tool")) {
                    event.setCancelled(true);
                    plugin.getStaffManager().toggleVanish(player);
                } else if (toolType.equals("players_tool")) {
                    event.setCancelled(true);
                    plugin.getGuiManager().openPlayersMenu(player);
                    Location _loc3 = player.getLocation(); if (_loc3 != null) player.playSound(_loc3, Objects.requireNonNull(Sound.BLOCK_NOTE_BLOCK_CHIME), 1.0f, 1.0f);
                    player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getMsgPlayers()));
                } else if (toolType.equals("random_tp_tool")) {
                    event.setCancelled(true);

                    List<? extends Player> candidates = Bukkit.getOnlinePlayers().stream()
                            .filter(target -> target != null && !target.getUniqueId().equals(player.getUniqueId()))
                            .toList();

                    if (candidates.isEmpty()) {
                        Location _loc2 = player.getLocation(); if (_loc2 != null) player.playSound(_loc2, Objects.requireNonNull(Sound.BLOCK_NOTE_BLOCK_BASS), 0.9f, 0.5f);
                        player.sendMessage(
                                MessageUtils.getColoredMessage(config.getPrefix() + config.getMsgRandomTpNoTargets()));
                        return;
                    }

                    Player target = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
                    if (target == null) return;
                    player.teleport(target.getLocation());
                    Location _loc = player.getLocation(); if (_loc != null) player.playSound(_loc, Objects.requireNonNull(Sound.ENTITY_ENDERMAN_TELEPORT), 1.0f, 1.0f);
                    player.sendMessage(MessageUtils.getColoredMessage(
                            config.getPrefix() + config.getMsgRandomTp().replace("{player}", target.getName())));
                } else if (toolType.equals("wall_compass_tool")) {
                    event.setCancelled(true);
                    if (!isWallCompassReady(player))
                        return;

                    int maxDistance = config.getWallCompassRange();
                    Block targetBlock = player.getTargetBlockExact(maxDistance);

                    if (targetBlock == null) {
                        Location _loc2 = player.getLocation(); if (_loc2 != null) player.playSound(_loc2, Objects.requireNonNull(Sound.BLOCK_NOTE_BLOCK_BASS), 0.9f, 0.5f);
                        return;
                    }

                    Location destination = findSafeTopLocation(targetBlock, player.getLocation());
                    if (destination == null) {
                        Location _loc6 = player.getLocation(); if (_loc6 != null) player.playSound(_loc6, Objects.requireNonNull(Sound.BLOCK_NOTE_BLOCK_BASS), 0.9f, 0.5f);
                        return;
                    }

                    player.teleport(destination);
                    Location _loc = player.getLocation(); if (_loc != null) player.playSound(_loc, Objects.requireNonNull(Sound.ENTITY_ENDERMAN_TELEPORT), 1.0f, 1.0f);
                } else if (toolType.equals("punish_tool")) {
                    event.setCancelled(true);
                    org.bukkit.util.RayTraceResult ray = player.getWorld().rayTraceEntities(
                            player.getEyeLocation(),
                            player.getEyeLocation().getDirection(),
                            5,
                            entity -> entity instanceof org.bukkit.entity.Player && !entity.equals(player));

                    if (ray == null || ray.getHitEntity() == null) {
                        player.sendMessage(
                                MessageUtils.getColoredMessage(config.getPrefix() + config.getPlayerClickPls()));
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
                        silentEnderChestViewers.add(player.getUniqueId());
                        player.openInventory(player.getEnderChest());
                    } else {
                        if (activeInspections.containsKey(block)) {
                            UUID inspectorUUID = activeInspections.get(block);
                            Player inspector = Bukkit.getPlayer(inspectorUUID);
                            String name = (inspector != null) ? inspector.getName() : "otro Staff";

                            player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() +
                                    "&cError: El contenedor ya está siendo inspeccionado por &e" + name));
                            Location _loc5 = player.getLocation(); if (_loc5 != null) player.playSound(_loc5, Objects.requireNonNull(Sound.ENTITY_VILLAGER_NO), 1.0f, 1.0f);
                            return;
                        }

                        Container container = (Container) block.getState();
                        Inventory realInv = container.getInventory();

                        Component title = container.customName() != null ? container.customName()
                                : Component.text(container.getType().name());

                        Inventory silentInv = createMirrorInventory(realInv, title);
                        silentInv.setContents(realInv.getContents());

                        silentViewers.put(player.getUniqueId(), block);
                        activeInspections.put(block, player.getUniqueId());

                        player.openInventory(silentInv);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return;
        Player player = event.getPlayer();
        if (!plugin.getStaffManager().isInStaffMode(player))
            return;
        if (!(event.getRightClicked() instanceof Player))
            return;

        Player target = (Player) event.getRightClicked();
        ItemStack item = player.getInventory().getItemInMainHand();
        MainConfigManager config = plugin.getMainConfigManager();

        String toolType = getStaffToolType(item);
        if (toolType == null)
            return;

        if (toolType.equals("inspect_tool")) {
            event.setCancelled(true);
            String msg = config.getMsgInspect().replace("{player}", target.getName());
            player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + msg));
            player.openInventory(Objects.requireNonNull(InspectionInventoryBuilder.createOnlineInspection(
                    "INSPECT_ONLINE",
                    target,
                    config.getInvseeInspectionOnlineTitle().replace("{player}", target.getName()),
                    config,
                    player.hasPermission("maxstaff.revive"))));
        } else if (toolType.equals("punish_tool")) {
            event.setCancelled(true);
            plugin.getGuiManager().openUserInfoMenu(player, target);
        } else if (toolType.equals("freeze_tool")) {

            if (target.hasPermission("maxstaff.admin") || target.hasPermission("maxstaff.freeze")) {
                player.sendMessage(
                        MessageUtils.getColoredMessage(config.getPrefix() + "&cNo puedes congelar a este usuario."));
                return;
            }

            event.setCancelled(true);
            plugin.getFreezeManager().toggleFreeze(player, target);
        }
    }

    private boolean isWallCompassReady(Player player) {
        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();
        Long lastUse = wallCompassCooldowns.get(uuid);

        if (lastUse != null && now - lastUse < WALL_COMPASS_COOLDOWN_MS) {
            return false;
        }

        wallCompassCooldowns.put(uuid, now);
        return true;
    }

    private boolean isSafeTeleportLocation(Location location) {
        Block feet = location.getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();
        Block ground = location.clone().add(0, -1, 0).getBlock();

        return feet.isPassable() && head.isPassable() && !ground.isPassable();
    }

    private Location findSafeTopLocation(Block clickedBlock, Location playerView) {
        Location destination = clickedBlock.getLocation().add(0.5, 0.0, 0.5);
        destination.setYaw(playerView.getYaw());
        destination.setPitch(playerView.getPitch());

        int minY = destination.getWorld().getMinHeight();
        int maxY = destination.getWorld().getMaxHeight();

        while (destination.getY() > minY && destination.getBlock().isPassable()) {
            destination.subtract(0, 1, 0);
        }
        destination.add(0, 1, 0);

        int upwardChecks = 0;
        int maxUpwardChecks = 6;
        while (!isSafeTeleportLocation(destination) && destination.getY() < maxY && upwardChecks < maxUpwardChecks) {
            destination.add(0, 1, 0);
            upwardChecks++;
        }

        if (!isSafeTeleportLocation(destination)) {
            return null;
        }

        return destination;
    }

    private String getStaffToolType(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta())
            return null;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, "staff_tool");
        if (!meta.getPersistentDataContainer().has(key, Objects.requireNonNull(PersistentDataType.STRING)))
            return null;
        return meta.getPersistentDataContainer().get(key, Objects.requireNonNull(PersistentDataType.STRING));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (!plugin.getStaffManager().isInStaffMode(player))
            return;

        if (isViewingSilentContainer(player)
                && shouldCancelSilentContainerMove(plugin.getMainConfigManager().isStaffModeAllowContainerItemMove())) {
            event.setCancelled(true);
            return;
        }

        if (event.getClick() == ClickType.NUMBER_KEY) {
            int hotbarButton = event.getHotbarButton();
            if (hotbarButton >= 0 && hotbarButton <= 8 && isProtectedStaffSlot(hotbarButton)) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory() == null)
            return;

        int clickedSlot = event.getSlot();
        if (event.getClickedInventory() instanceof PlayerInventory && isProtectedStaffSlot(clickedSlot)) {
            event.setCancelled(true);
            return;
        }

        InventoryAction action = event.getAction();
        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        int hotbarButton = event.getHotbarButton();

        if (isStaffToolItem(currentItem)
                || isStaffToolItem(cursorItem)
                || (hotbarButton >= 0 && isProtectedStaffSlot(hotbarButton))) {
            event.setCancelled(true);
            return;
        }

        if ((action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || action == InventoryAction.HOTBAR_SWAP)
                && isStaffToolItem(currentItem)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (!plugin.getStaffManager().isInStaffMode(player))
            return;

        if (isViewingSilentContainer(player)
                && shouldCancelSilentContainerMove(plugin.getMainConfigManager().isStaffModeAllowContainerItemMove())) {
            event.setCancelled(true);
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= topSize) {
                int convertedSlot = event.getView().convertSlot(rawSlot);
                if (isProtectedStaffSlot(convertedSlot)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        ItemStack oldCursor = event.getOldCursor();
        if (isStaffToolItem(oldCursor)) {
            event.setCancelled(true);
            return;
        }

        for (ItemStack newItem : event.getNewItems().values()) {
            if (isStaffToolItem(newItem)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (silentEnderChestViewers.remove(uuid)) {
            return;
        }

        if (silentViewers.containsKey(uuid)) {
            Block block = silentViewers.remove(uuid);
            activeInspections.remove(block);

            if (!shouldSaveSilentContainerChanges(plugin.getMainConfigManager().isStaffModeAllowContainerItemMove())) {
                return;
            }

            if (block.getState() instanceof Container) {
                Container container = (Container) block.getState();
                container.getInventory().setContents(event.getInventory().getContents());
            }
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        Player player = (Player) event.getEntity();
        if (plugin.getStaffManager().isInStaffMode(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        wallCompassCooldowns.remove(uuid);
        silentEnderChestViewers.remove(uuid);

        if (silentViewers.containsKey(uuid)) {
            Block block = silentViewers.remove(uuid);
            activeInspections.remove(block);

            if (!shouldSaveSilentContainerChanges(plugin.getMainConfigManager().isStaffModeAllowContainerItemMove())) {
                return;
            }

            if (block.getState() instanceof Container container) {
                Inventory topInventory = player.getOpenInventory().getTopInventory();
                if (topInventory != null && topInventory.getHolder() == null) {
                    container.getInventory().setContents(topInventory.getContents());
                }
            }
        }
    }

    private Location findThruLocation(Player player, int maxDistance) {
        try {
            BlockIterator iterator = new BlockIterator(player, maxDistance);
            boolean hitWall = false;

            while (iterator.hasNext()) {
                Block block = iterator.next();

                if (!block.isPassable()) {
                    hitWall = true;
                } else if (hitWall && block.isPassable()) {
                    Location dest = block.getLocation().add(0.5, 0, 0.5);
                    Location playerLoc = player.getLocation();
                    if (playerLoc != null) {
                        dest.setYaw(playerLoc.getYaw());
                        dest.setPitch(playerLoc.getPitch());
                    }

                    while (dest.getBlock().isPassable() && dest.getY() > dest.getWorld().getMinHeight()) {
                        dest.subtract(0, 1, 0);
                    }
                    dest.add(0, 1, 0);

                    if (dest.getBlock().isPassable() && dest.clone().add(0, 1, 0).getBlock().isPassable()) {
                        return dest;
                    }
                }
            }
        } catch (IllegalStateException e) {
            plugin.getLogger().fine("[StaffMode] findThruLocation failed for " + player.getName()
                    + " (maxDistance=" + maxDistance + "): " + e.getMessage());
        }
        return null;
    }

    private Inventory createMirrorInventory(Inventory realInv, Component title) {
        int size = realInv.getSize();
        if (size >= 9 && size <= 54 && size % 9 == 0) {
            return Bukkit.createInventory(null, size, title);
        }

        InventoryType type = realInv.getType();
        if (type.isCreatable()) {
            return Bukkit.createInventory(null, type, title);
        }

        int fallbackSize = Math.min(54, ((Math.max(size, 1) + 8) / 9) * 9);
        return Bukkit.createInventory(null, fallbackSize, title);
    }

    private boolean isStaffToolItem(ItemStack item) {
        return getStaffToolType(item) != null;
    }

    private boolean isViewingSilentContainer(Player player) {
        UUID uuid = player.getUniqueId();
        return silentViewers.containsKey(uuid) || silentEnderChestViewers.contains(uuid);
    }

    static boolean shouldCancelSilentContainerMove(boolean allowItemMove) {
        return !allowItemMove;
    }

    static boolean shouldSaveSilentContainerChanges(boolean allowItemMove) {
        return allowItemMove;
    }

    private boolean isProtectedStaffSlot(int slot) {
        if (slot < 0 || slot > 8) {
            return false;
        }

        MainConfigManager config = plugin.getMainConfigManager();
        if (slot == config.getStaffPlayersSlot()
                || slot == config.getStaffRandomTpSlot()
                || slot == config.getStaffWallCompassSlot()
                || slot == config.getStaffInspectSlot()
                || slot == config.getStaffVanishSlot()) {
            return true;
        }

        if (plugin.isModuleEnabled("freeze") && slot == config.getStaffFreezeSlot()) {
            return true;
        }

        return plugin.isModuleEnabled("sanctions-gui") && slot == config.getStaffPunishSlot();
    }

}
