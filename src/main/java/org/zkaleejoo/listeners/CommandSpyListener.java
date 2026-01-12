package org.zkaleejoo.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

public class CommandSpyListener implements Listener {

    private final MaxStaff plugin;

    public CommandSpyListener(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player sender = event.getPlayer();
        String command = event.getMessage();

        if (sender.hasPermission("maxstaff.admin")) return;

        String format = plugin.getMainConfigManager().getMsgCmdSpyFormat()
                .replace("{player}", sender.getName())
                .replace("{command}", command);

        String coloredMsg = MessageUtils.getColoredMessage(format);

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (plugin.getStaffManager().isSpying(staff)) {
                if (staff.equals(sender)) continue;
                
                staff.sendMessage(coloredMsg);
            }
        }
    }
}