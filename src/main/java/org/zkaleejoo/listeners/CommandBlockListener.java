package org.zkaleejoo.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

public class CommandBlockListener implements Listener {

    private final MaxStaff plugin;

    public CommandBlockListener(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (shouldBlockFrozenCommand(player)) {
            event.setCancelled(true);
            sendBlockedMessage(player, plugin.getMainConfigManager().getFreezeCommandBlocked());
            return;
        }

        if (shouldBlockStaffModeCommand(player, event.getMessage())) {
            event.setCancelled(true);
            sendBlockedMessage(player, plugin.getMainConfigManager().getStaffModeCommandBlocked());
        }
    }

    private boolean shouldBlockFrozenCommand(Player player) {
        return plugin.isModuleEnabled("freeze")
                && plugin.getMainConfigManager().isFreezeBlockAllCommands()
                && plugin.getFreezeManager() != null
                && plugin.getFreezeManager().isFrozen(player);
    }

    private boolean shouldBlockStaffModeCommand(Player player, String command) {
        return plugin.isModuleEnabled("staff-mode")
                && plugin.getStaffManager() != null
                && plugin.getStaffManager().isInStaffMode(player)
                && CommandBlockPolicy.isBlocked(command, plugin.getMainConfigManager().getStaffModeBlacklistedCommands());
    }

    private void sendBlockedMessage(Player player, String message) {
        player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + message));
    }
}
