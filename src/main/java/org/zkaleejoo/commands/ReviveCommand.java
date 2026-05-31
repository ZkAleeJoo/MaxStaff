package org.zkaleejoo.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;

public class ReviveCommand implements CommandExecutor {

    private final MaxStaff plugin;

    public ReviveCommand(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MainConfigManager config = plugin.getMainConfigManager();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getMsgConsole()));
            return true;
        }

        if (!player.hasPermission("maxstaff.revive")) {
            player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getNoPermission()));
            return true;
        }

        if (args.length > 0) {
            player.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getReviveUse()));
            return true;
        }

        plugin.getGuiManager().openReviveMenu(player);
        return true;
    }
}
