package org.zkaleejoo.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;

public class CommandSpyCommand implements CommandExecutor {

    private final MaxStaff plugin;

    public CommandSpyCommand(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandContextUtil.requirePlayer(sender, plugin.getMainConfigManager());
        if (player == null) {
            return true;
        }

        if (!CommandContextUtil.requirePermission(player, "maxstaff.cmdspy", plugin.getMainConfigManager())) {
            return true;
        }

        plugin.getStaffManager().toggleCommandSpy(player);
        return true;
    }
}