package org.zkaleejoo.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

import java.util.Arrays;

public class PunishmentCommand implements CommandExecutor {

    private final MaxStaff plugin;

    public PunishmentCommand(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("maxstaff.punish")) {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()));
            return true;
        }

        if (args.length < 1) {
            String usage = plugin.getMainConfigManager().getMsgUsage()
                    .replace("{command}", label);
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + usage));
            return true;
        }

        String target = args[0];
        
        if (label.equalsIgnoreCase("kick")) {
            String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : plugin.getMainConfigManager().getNoReason();
            plugin.getPunishmentManager().kickPlayer(sender, target, reason);
            return true;
        }

        if (label.equalsIgnoreCase("unban")) {
            plugin.getPunishmentManager().unbanPlayer(sender, target);
            return true;
        }
        if (label.equalsIgnoreCase("unmute")) {
            plugin.getPunishmentManager().unmutePlayer(sender, target);
            return true;
        }

        if (label.equalsIgnoreCase("warn")) {
        if (args.length < 1) {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + " " + plugin.getMainConfigManager().getMsgUsage().replace("{command}", label)));
            return true;
        }

            String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Sin motivo";
            plugin.getPunishmentManager().warnPlayer(sender, target, reason);
            return true;
        }

        String time = "perm";
        String reason = plugin.getMainConfigManager().getNoReason();
        
        int reasonStartIndex = 1;
        if (args.length > 1) {
            String arg1 = args[1].toLowerCase();
            boolean looksLikeTime = (arg1.matches(".*[dhms]") && Character.isDigit(arg1.charAt(0))) 
                                    || arg1.equals("perm") || arg1.equals("permanent");

            if (looksLikeTime) {
                time = arg1;
                reasonStartIndex = 2;
            }
        }
        
        if (args.length > reasonStartIndex) {
            reason = String.join(" ", Arrays.copyOfRange(args, reasonStartIndex, args.length));
        }

        if (label.equalsIgnoreCase("ban") || label.equalsIgnoreCase("tempban")) {
            plugin.getPunishmentManager().banPlayer(sender, target, reason, time);
        } else if (label.equalsIgnoreCase("mute") || label.equalsIgnoreCase("tempmute")) {
            plugin.getPunishmentManager().mutePlayer(sender, target, reason, time);
        }

        return true;
    }
}