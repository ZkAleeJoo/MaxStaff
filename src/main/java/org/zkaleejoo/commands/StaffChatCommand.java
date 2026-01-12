package org.zkaleejoo.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

public class StaffChatCommand implements CommandExecutor {

    private final MaxStaff plugin;

    public StaffChatCommand(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("maxstaff.staffchat")) {
            sender.sendMessage(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()
            ));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(MessageUtils.getColoredMessage("&c&l(!) &cEscribe un mensaje para el staff. Uso: /sc <mensaje>"));
            return true;
        }

        String message = String.join(" ", args);
        String senderName = sender.getName();
        
        String formattedMessage = plugin.getMainConfigManager().getStaffChatFormat()
                .replace("{player}", senderName)
                .replace("{message}", message);

        String coloredMessage = MessageUtils.getColoredMessage(formattedMessage);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("maxstaff.staffchat")) {
                online.sendMessage(coloredMessage);
            }
        }

        Bukkit.getConsoleSender().sendMessage(coloredMessage);

        return true;
    }
}