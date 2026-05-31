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

public class FreezeCommand implements CommandExecutor, TabCompleter {

    private final MaxStaff plugin;

    public FreezeCommand(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player staff = CommandContextUtil.requirePlayer(sender, plugin.getMainConfigManager());
        if (staff == null) {
            return true;
        }
        String action = label.toLowerCase();

        if (!CommandContextUtil.requirePermission(staff, "maxstaff.freeze", plugin.getMainConfigManager())) {
            return true;
        }

        if (args.length < 1) {
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getFreezeUse()));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgOffline()));
            return true;
        }

        if (target.hasPermission("maxstaff.admin") || target.hasPermission("maxstaff.freeze")) {
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getFreezeStaff()));
            return true;
        }

        boolean isFrozen = plugin.getFreezeManager().isFrozen(target);

        if (action.equals("freeze") || action.equals("ss")) {
            if (isFrozen) {
                plugin.getFreezeManager().setFrozen(target, false, staff.getName());
                staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgUnfreezeStaff().replace("{player}", target.getName())));
            } else {
                plugin.getFreezeManager().setFrozen(target, true, staff.getName());
                staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgFreezeStaff().replace("{player}", target.getName())));
            }
        } else if (action.equals("unfreeze") || action.equals("uss")) {
            if (!isFrozen) {
                staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getUnfreezeAlready()));
            } else {
                plugin.getFreezeManager().setFrozen(target, false, staff.getName());
                staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgUnfreezeStaff().replace("{player}", target.getName())));
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandContextUtil.filterOnlinePlayerNamesByPrefix(args[0]);
        }
        return new ArrayList<>();
    }
}