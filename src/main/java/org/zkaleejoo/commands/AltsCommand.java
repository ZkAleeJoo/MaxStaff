package org.zkaleejoo.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

public class AltsCommand implements CommandExecutor {
    private final MaxStaff plugin;

    public AltsCommand(MaxStaff plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgConsole());
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("maxstaff.alts")) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + "&cUso: /alts <jugador>"));
            return true;
        }

        plugin.getGuiManager().openAltsMenu(player, args[0]);
        return true;
    }
}