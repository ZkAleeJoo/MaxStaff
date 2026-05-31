package org.zkaleejoo.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

public class SanctionCommand implements CommandExecutor, TabCompleter {

    private final MaxStaff plugin;

    public SanctionCommand(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player staff = CommandContextUtil.requirePlayer(sender, plugin.getMainConfigManager());
        if (staff == null) {
            return true;
        }

        if (args.length < 1) {
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getCommandSanctionUse()));
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            if (!CommandContextUtil.requirePermission(staff, "maxstaff.sanctions.list", plugin.getMainConfigManager())) {
                return true;
            }
            plugin.getGuiManager().openActivePunishmentsMenu(staff);
            return true;
        }

        if (!CommandContextUtil.requirePermission(staff, "maxstaff.punish", plugin.getMainConfigManager())) {
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgOffline()));
            return true;
        }

        if (target.equals(staff)) {
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getCommandSanctionSelf()));
            return true;
        }

        plugin.getGuiManager().openUserInfoMenu(staff, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (CommandContextUtil.hasPermissionOrAdmin(sender, "maxstaff.sanctions.list")) {
                completions.add("list");
            }
            if (CommandContextUtil.hasPermissionOrAdmin(sender, "maxstaff.punish")) {
                completions.addAll(CommandContextUtil.filterOnlinePlayerNamesByPrefix(args[0]));
            }
            String prefix = args[0].toLowerCase();
            return completions.stream()
                    .filter(value -> value.toLowerCase().startsWith(prefix))
                    .toList();
        }
        return new ArrayList<>();
    }
}
