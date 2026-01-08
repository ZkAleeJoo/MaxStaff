package org.zkaleejoo.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PunishmentCommand implements CommandExecutor, TabCompleter { 

    private final MaxStaff plugin;

    public PunishmentCommand(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String baseLabel = label.toLowerCase();
        
        if (baseLabel.equals("tempban")) baseLabel = "ban";
        if (baseLabel.equals("tempmute")) baseLabel = "mute";
        
        String permission = "maxstaff.punish." + baseLabel;

        if (!sender.hasPermission(permission)) {
            sender.sendMessage(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()
            ));
            return true;
        }

        if (args.length < 1) {
            String usage = plugin.getMainConfigManager().getMsgUsage().replace("{command}", label);
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
            String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : plugin.getMainConfigManager().getNoReason();
            plugin.getPunishmentManager().warnPlayer(sender, target, reason);
            return true;
        }
        // Dentro de PunishmentCommand.java -> onCommand
        if (label.equalsIgnoreCase("history")) {
            if (args.length < 1) {
                sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + "&cUsage: /history <player>"));
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgConsole()));
                return true;
            }
            plugin.getGuiManager().openHistoryMenu((Player) sender, args[0]);
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        String baseLabel = label.toLowerCase();
        if (baseLabel.equals("tempban")) baseLabel = "ban";
        if (baseLabel.equals("tempmute")) baseLabel = "mute";
        
        if (!sender.hasPermission("maxstaff.punish." + baseLabel)) return completions;

        if (args.length == 1) {
            if (label.equalsIgnoreCase("unban")) {
                completions.addAll(plugin.getPunishmentManager().getBannedPlayerNames());
            } 
            else if (label.equalsIgnoreCase("unmute")) {
                completions.addAll(plugin.getPunishmentManager().getMutedPlayerNames());
            } 
            else {
                return null; 
            }
            
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (label.equalsIgnoreCase("ban") || label.equalsIgnoreCase("tempban") || 
                label.equalsIgnoreCase("mute") || label.equalsIgnoreCase("tempmute")) {
                completions.addAll(Arrays.asList("1h", "1d", "7d", "30d", "perm"));
                return completions.stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }
}