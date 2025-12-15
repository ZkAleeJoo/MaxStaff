package org.zkaleejoo.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

public class MainCommand implements CommandExecutor, TabCompleter {

    private MaxStaff plugin;

    public MainCommand(MaxStaff plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {

        // COMANDOS DE CONSOLA O JUGADOR
        
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (sender instanceof Player && !sender.hasPermission("maxstaff.admin")) {
                    sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()));
                    return true;
                }

                plugin.getMainConfigManager().reloadConfig();
                sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getPluginReload()));
                return true;
            }
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission("maxstaff.admin")) {
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()));
                return true;
            }
        }

        if(args.length >= 1){
             if(args[0].equalsIgnoreCase("help")){
                help(sender);
            } else {
                 sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getSubcommandInvalid()));
            }
        } else {
            help(sender);
        }

        return true;
    }

    public void help(CommandSender sender){
        sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + "&fLista de comandos: " + plugin.getDescription().getVersion()));
        sender.sendMessage(MessageUtils.getColoredMessage("&9> &a/maxstaff reload"));
        sender.sendMessage(MessageUtils.getColoredMessage("&9> &a/maxstaff help"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("maxstaff.admin")) {
            return completions;
        }

        if (args.length == 1) {
            completions.add("reload");
            completions.add("help");
            return filterCompletions(completions, args[0]);
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