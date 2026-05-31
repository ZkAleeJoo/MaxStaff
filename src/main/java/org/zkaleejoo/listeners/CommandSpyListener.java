package org.zkaleejoo.listeners;

import java.util.List;
import java.util.Locale;
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player sender = event.getPlayer();
        String rawCommand = event.getMessage();

        if (sender.hasPermission("maxstaff.admin")) return;

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff == null) continue;
            if (!plugin.getStaffManager().isSpying(staff) || staff.equals(sender)) {
                continue;
            }

            String commandForStaff = getCommandForStaff(staff, rawCommand);
            String format = plugin.getMainConfigManager().getMsgCmdSpyFormat()
                    .replace("{player}", sender.getName())
                    .replace("{command}", commandForStaff);

            staff.sendMessage(MessageUtils.getColoredMessage(format));
        }
    }

    private String getCommandForStaff(Player staff, String rawCommand) {
        if (rawCommand == null) {
            return "";
        }

        String bypassPermission = plugin.getMainConfigManager().getCmdSpySensitiveBypassPermission();
        if (bypassPermission != null && !bypassPermission.isBlank() && staff.hasPermission(bypassPermission)) {
            return rawCommand;
        }

        String trimmedCommand = rawCommand.trim();
        if (!trimmedCommand.startsWith("/")) {
            return rawCommand;
        }

        String commandBody = trimmedCommand.substring(1).trim();
        if (commandBody.isEmpty()) {
            return rawCommand;
        }

        String[] commandParts = commandBody.split("\\s+", 2);
        String commandLabel = commandParts[0].toLowerCase(Locale.ROOT);
        List<String> sensitiveCommands = plugin.getMainConfigManager().getCmdSpySensitiveCommands();
        if (sensitiveCommands == null || !sensitiveCommands.contains(commandLabel)) {
            return rawCommand;
        }

        String maskedArgument = plugin.getMainConfigManager().getCmdSpyMaskedArgument();
        if (maskedArgument == null || maskedArgument.isBlank()) {
            maskedArgument = "******";
        }

        if (commandParts.length == 1) {
            return "/" + commandParts[0];
        }

        return "/" + commandParts[0] + " " + maskedArgument;
    }
}