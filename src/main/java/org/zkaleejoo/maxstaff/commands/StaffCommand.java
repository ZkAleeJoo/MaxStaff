package org.zkaleejoo.maxstaff.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.maxstaff.managers.StaffModeManager;

public class StaffCommand implements CommandExecutor {

    private final StaffModeManager staffManager;

    public StaffCommand(StaffModeManager staffManager) {
        this.staffManager = staffManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cSolo jugadores pueden usar este comando.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("maxstaff.use")) {
            player.sendMessage("§cNo tienes permiso para usar esto.");
            return true;
        }

        staffManager.toggleStaffMode(player);
        return true;
    }
}