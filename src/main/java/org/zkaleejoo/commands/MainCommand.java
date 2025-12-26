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

public class MainCommand implements CommandExecutor, TabCompleter {

    private MaxStaff plugin;

    public MainCommand(MaxStaff plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (sender instanceof Player && !sender.hasPermission("maxstaff.admin")) {
                sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()));
                return true;
            }
            
            plugin.getMainConfigManager().reloadConfig();
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getPluginReload()));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgConsole()));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("maxstaff.admin")) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()));
            return true;
        }

        if(args.length >= 1){
             if(args[0].equalsIgnoreCase("help")){
                help(sender);

            } else if (args[0].equalsIgnoreCase("mode") || args[0].equalsIgnoreCase("staff")) {
                plugin.getStaffManager().toggleStaffMode(player);

            } else {
                 sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getSubcommandInvalid()));
            }
        } else {
            help(sender);
        }

        //NUEVA SECCIÓN
        if (args.length >= 1 && args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("maxstaff.admin")) {
                sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getUseStaffReset()));
                return true;
            }
            String target = args[1];
            String type = args[2].toUpperCase();
            
            String msg = plugin.getMainConfigManager().getMsgResetSuccess()
                        .replace("{type}", type)
                        .replace("{target}", target);
                        
                sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + msg));
                return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("take")) {
            if (!sender.hasPermission("maxstaff.admin")) {
                sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getUseStaffTake()));
                return true;
            }
            
            String target = args[1];
            String type = args[2].toUpperCase();
            int amount = 1;
            
            if (args.length >= 4) {
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + 
                        plugin.getMainConfigManager().getTakeNumberInvalid()));
                    return true;
                }
            }

            boolean success = plugin.getPunishmentManager().takeHistory(target, type, amount);
            if (success) {
                String msg = plugin.getMainConfigManager().getMsgTakeSuccess()
                .replace("{amount}", String.valueOf(amount))
                .replace("{type}", type)
                .replace("{target}", target);
                
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + msg));
            } else {
                sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + 
                    plugin.getMainConfigManager().getPlayerNoHistory()));
            }
            return true;
        }

        return true;
    }

    public void help(CommandSender sender){
        String titleTemplate = plugin.getConfig().getString("messages.command-help-title", "&fList of commands: &b{version}");
        sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + titleTemplate.replace("{version}", plugin.getDescription().getVersion())));
        
        List<String> helpLines = plugin.getConfig().getStringList("messages.command-help-list");
        if (helpLines == null || helpLines.isEmpty()) {
            sender.sendMessage(MessageUtils.getColoredMessage("&9> &a/maxstaff reload &7- Reload settings"));
            sender.sendMessage(MessageUtils.getColoredMessage("&9> &a/maxstaff mode &7- Toggle staff mode"));
            sender.sendMessage(MessageUtils.getColoredMessage("&9> &a/maxstaff help &7- View list of commands"));
            sender.sendMessage(MessageUtils.getColoredMessage("&9> &a/maxstaff reset"));
            sender.sendMessage(MessageUtils.getColoredMessage("&9> &a/maxstaff take"));
        } else {
            for (String line : helpLines) {
                sender.sendMessage(MessageUtils.getColoredMessage(line));
            }
        }
    }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            List<String> completions = new ArrayList<>();
            if (!sender.hasPermission("maxstaff.admin")) return completions;

            if (args.length == 1) {
                completions.addAll(Arrays.asList("reload", "help", "mode", "reset", "take"));
                return filterCompletions(completions, args[0]);
            }

            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("take")) {
                    return null; 
                }
            }

            if (args.length == 3) {
                if (args[0].equalsIgnoreCase("reset")) {
                    completions.addAll(Arrays.asList("BAN", "MUTE", "KICK", "WARN", "ALL"));
                } else if (args[0].equalsIgnoreCase("take")) {
                    completions.addAll(Arrays.asList("BAN", "MUTE", "KICK", "WARN"));
                }
                return filterCompletions(completions, args[2]);
            }

            return completions;
        }

    private List<String> filterCompletions(List<String> completions, String input) {
        List<String> filtered = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(input.toLowerCase())) {
                filtered.add(completion);
            }
        }
        return filtered;
    }
}