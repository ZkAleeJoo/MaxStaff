package org.zkaleejoo.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

public class StaffChatCommand implements CommandExecutor {

    private static final String STAFF_CHAT_PERMISSION = "maxstaff.staffchat";

    private final MaxStaff plugin;

    public StaffChatCommand(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(STAFF_CHAT_PERMISSION)) {
            sender.sendMessage(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()
            ));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getChatStaffUse()
                ));
                return true;
            }

            boolean enabled = plugin.toggleStaffChat(player.getUniqueId());
            String feedback = enabled
                ? plugin.getMainConfigManager().getStaffChatToggleOn()
                : plugin.getMainConfigManager().getStaffChatToggleOff();

            player.sendMessage(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getPrefix() + feedback
            ));
            return true;
        }

        String message = String.join(" ", args);
        sendStaffMessage(sender, message);

        return true;
    }

    public void sendStaffMessage(CommandSender sender, String message) {
        String formattedMessage = plugin.getMainConfigManager().getStaffChatFormat()
            .replace("{player}", sender.getName())
            .replace("{message}", message);

        String coloredMessage = MessageUtils.getColoredMessage(formattedMessage);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null) continue;
            if (online.hasPermission(STAFF_CHAT_PERMISSION)) {
                online.sendMessage(coloredMessage);
            }
        }

        Bukkit.getConsoleSender().sendMessage(coloredMessage);
    }
}