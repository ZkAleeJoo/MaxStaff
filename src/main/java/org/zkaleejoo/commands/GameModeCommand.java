package org.zkaleejoo.commands;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter; 
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GameModeCommand implements CommandExecutor, TabCompleter { 

    private final MaxStaff plugin;

    public GameModeCommand(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgConsole()));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("maxstaff.gamemode")) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()));
            return true;
        }

        //AGREGAR AL MAINCONFIG MANAGER
        if (args.length == 0) {
            if (plugin.getMainConfigManager().isGmMenuEnabled()) {
                plugin.getGuiManager().openGameModeMenu(player);
            } else {
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + "&cUsage: /gm <0|1|2|3>"));
            }
            return true;
        }

        GameMode mode = matchGameMode(args[0]);
        if (mode == null) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + "&cModo de juego inválido."));
            return true;
        }

        player.setGameMode(mode);
        
        if (plugin.getStaffManager().isInStaffMode(player)) {
            plugin.getStaffManager().updateSavedGameMode(player, mode);
        }

        player.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + 
            plugin.getMainConfigManager().getGuiGmFeedback().replace("{mode}", mode.name())));
        
        return true;
    }

    private GameMode matchGameMode(String arg) {
        switch (arg.toLowerCase()) {
            case "0": case "survival": case "s": return GameMode.SURVIVAL;
            case "1": case "creative": case "c": return GameMode.CREATIVE;
            case "2": case "adventure": case "a": return GameMode.ADVENTURE;
            case "3": case "spectator": case "sp": return GameMode.SPECTATOR;
            default: return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("0", "1", "2", "3", "survival", "creative", "adventure", "spectator")
                    .stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return null;
    }
}