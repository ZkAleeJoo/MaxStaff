package org.zkaleejoo.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.managers.StaffManager;
import org.zkaleejoo.utils.MessageUtils;

public class StaffCommand implements CommandExecutor {

    private final MaxStaff plugin;

    public StaffCommand(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgConsole()));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("maxstaff.command.staff")) {
            player.sendMessage(MessageUtils.getColoredMessage("&cNo tienes permiso para usar este comando."));
            return true;
        }

        plugin.getStaffManager().toggleStaffMode(player); 
        
        //StaffManager.toggleStaffMode(player);
        
        return true;
    }
}