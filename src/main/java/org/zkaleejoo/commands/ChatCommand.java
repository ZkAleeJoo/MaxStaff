package org.zkaleejoo.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.managers.ChatManager; // <--- ESTA IMPORTACIÓN ES VITAL
import org.zkaleejoo.utils.MessageUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChatCommand implements CommandExecutor, TabCompleter {
    private final MaxStaff plugin;

    public ChatCommand(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("maxstaff.chat.admin")) {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgChatUsage()));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("clear")) {
            plugin.getChatManager().clearChat(sender);
        } 
        else if (sub.equals("mute")) {
            boolean currentStatus = plugin.getChatManager().isGlobalMute();
            plugin.getChatManager().setGlobalMute(!currentStatus);
            
            String msg = (!currentStatus ? plugin.getMainConfigManager().getMsgGlobalMuteEnabled() : plugin.getMainConfigManager().getMsgGlobalMuteDisabled())
                    .replace("{player}", sender.getName());
            
            Bukkit.broadcastMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + msg));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("mute", "clear").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}