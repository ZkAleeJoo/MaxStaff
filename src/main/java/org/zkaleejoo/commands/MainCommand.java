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
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + "&cEste comando solo puede ser ejecutado por jugadores."));
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

        return true;
    }

    public void help(CommandSender sender){
        sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + "&fLista de comandos: &b" + plugin.getDescription().getVersion()));
        sender.sendMessage(MessageUtils.getColoredMessage("&9> &a/maxstaff reload &7- Recargar configuración"));
        sender.sendMessage(MessageUtils.getColoredMessage("&9> &a/maxstaff mode &7- Activar/Desactivar modo staff"));
        sender.sendMessage(MessageUtils.getColoredMessage("&9> &a/maxstaff help &7- Ver lista de comandos"));
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
            completions.add("mode"); 
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