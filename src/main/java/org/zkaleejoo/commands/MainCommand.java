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
import org.bukkit.Bukkit;
import org.bukkit.permissions.PermissionAttachmentInfo;
import java.util.Collections;
import java.util.Locale;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final MaxStaff plugin;

    public MainCommand(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("maxstaff.admin")) {
                sendNoPermission(sender);
                return true;
            }
            plugin.reloadPluginState();
            plugin.getGuiManager().updateCachedItems();
            plugin.getStaffManager().cacheStaffItems();
            sender.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getPluginReload()));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("cleanupbans")) {
            if (!sender.hasPermission("maxstaff.admin")) {
                sendNoPermission(sender);
                return true;
            }

            if (plugin.getPunishmentManager() instanceof org.zkaleejoo.managers.PunishmentManagerMysql mysqlManager) {
                mysqlManager.cleanupExpiredPunishments(sender);
            } else {
                sender.sendMessage(MessageUtils.getColoredMessage(
                        plugin.getMainConfigManager().getPrefix() + "&cCleanup is only available while using MySQL storage."));
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgConsole()));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            if (!player.hasPermission("maxstaff.admin")) {
                sendNoPermission(player);
                return true;
            }
            help(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reset")) {
            if (!player.hasPermission("maxstaff.admin")) {
                sendNoPermission(player);
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(MessageUtils.getColoredMessage(
                        plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getUseStaffReset()));
                return true;
            }
            String target = args[1];
            String type = args[2].toUpperCase();

            plugin.getPunishmentManager().resetHistory(target, type);

            String msg = plugin.getMainConfigManager().getMsgResetSuccess()
                    .replace("{type}", type)
                    .replace("{target}", target);
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + msg));
            return true;
        }

        else if (sub.equals("take")) {
            if (!player.hasPermission("maxstaff.admin")) {
                sendNoPermission(player);
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(MessageUtils.getColoredMessage(
                        plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getUseStaffTake()));
                return true;
            }

            String target = args[1];
            String type = args[2].toUpperCase();
            int amount = 1;

            if (args.length >= 4) {
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix()
                            + plugin.getMainConfigManager().getTakeNumberInvalid()));
                    return true;
                }
            }

            if (plugin.getPunishmentManager().takeHistory(target, type, amount)) {
                String msg = plugin.getMainConfigManager().getMsgTakeSuccess()
                        .replace("{amount}", String.valueOf(amount))
                        .replace("{type}", type)
                        .replace("{target}", target);
                sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + msg));
            } else {
                sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix()
                        + plugin.getMainConfigManager().getPlayerNoHistory()));
            }
            return true;
        }

        else if (sub.equals("debugperm")) {
            if (!player.hasPermission("maxstaff.admin")) {
                sendNoPermission(player);
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(MessageUtils.getColoredMessage(
                        plugin.getMainConfigManager().getPrefix() + "&cUso: /maxstaff debugperm <jugador> <tipo>"));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(MessageUtils.getColoredMessage(
                        plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgOffline()));
                return true;
            }

            String sanctionType = args[2].toLowerCase(Locale.ROOT);
            String specificNode = switch (sanctionType) {
                case "mute", "tempmute" -> "maxstaff.punish.mute";
                case "ban", "tempban" -> "maxstaff.punish.ban";
                case "kick" -> "maxstaff.punish.kick";
                case "warn" -> "maxstaff.punish.warn";
                default -> null;
            };

            if (specificNode == null) {
                sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix()
                        + "&cTipo no soportado. Usa: mute, ban, kick o warn."));
                return true;
            }

            sender.sendMessage(MessageUtils.getColoredMessage("&8&m---------------- &6DebugPerm &8&m----------------"));
            sender.sendMessage(MessageUtils.getColoredMessage("&7Player: &e" + target.getName()));
            sender.sendMessage(MessageUtils.getColoredMessage("&7Type: &e" + sanctionType));
            sender.sendMessage(MessageUtils
                    .getColoredMessage("&7maxstaff.admin: " + boolColor(target.hasPermission("maxstaff.admin"))));
            sender.sendMessage(MessageUtils
                    .getColoredMessage("&7maxstaff.punish: " + boolColor(target.hasPermission("maxstaff.punish"))));
            sender.sendMessage(MessageUtils
                    .getColoredMessage("&7" + specificNode + ": " + boolColor(target.hasPermission(specificNode))));

            List<String> matched = new ArrayList<>();
            for (PermissionAttachmentInfo info : target.getEffectivePermissions()) {
                if (!info.getValue()) {
                    continue;
                }
                String perm = info.getPermission();
                if (perm.equals("maxstaff.admin") || perm.equals("maxstaff.punish")
                        || perm.startsWith("maxstaff.punish.")) {
                    matched.add(perm);
                }
            }

            Collections.sort(matched);
            sender.sendMessage(MessageUtils.getColoredMessage("&7Detected nodes (&f" + matched.size() + "&7):"));
            if (matched.isEmpty()) {
                sender.sendMessage(
                        MessageUtils.getColoredMessage("&8 - &cNo relevant maxstaff nodes in effective permissions."));
            } else {
                for (String node : matched) {
                    sender.sendMessage(MessageUtils.getColoredMessage("&8 - &a" + node));
                }
            }
            sender.sendMessage(MessageUtils.getColoredMessage("&8&m---------------------------------------------"));
            return true;
        }

        sender.sendMessage(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getSubcommandInvalid()));
        return true;
    }

    private void sendNoPermission(CommandSender sender) {
        sender.sendMessage(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()));
    }

    public void help(CommandSender sender) {
        String titleTemplate = plugin.getMainConfigManager().getHelpTitle();
        sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix()
                + titleTemplate.replace("{version}", plugin.getPluginMeta().getVersion())));

        List<String> helpLines = plugin.getMainConfigManager().getHelpLines();

        if (helpLines == null || helpLines.isEmpty()) {
            if (sender.hasPermission("maxstaff.admin")) {
                sender.sendMessage(MessageUtils.getColoredMessage("&9> &a/maxstaff reload &7- Reload settings"));
                sender.sendMessage(MessageUtils.getColoredMessage("&9> &a/maxstaff help &7- View list of commands"));
                sender.sendMessage(MessageUtils.getColoredMessage("&9> &a/maxstaff reset"));
                sender.sendMessage(MessageUtils.getColoredMessage("&9> &a/maxstaff take"));
                sender.sendMessage(MessageUtils.getColoredMessage("&9> &a/maxstaff cleanupbans &7- Remove expired bans/mutes from MySQL"));
                sender.sendMessage(MessageUtils.getColoredMessage(
                        "&9> &a/maxstaff debugperm <player> <mute|ban|kick|warn> &7- Diagnose permissions"));
            }
        } else {
            for (String line : helpLines) {
                sender.sendMessage(MessageUtils.getColoredMessage(line));
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("maxstaff.admin")) {
                completions.addAll(Arrays.asList("reload", "help", "reset", "take", "cleanupbans", "debugperm"));
            }
            return filterCompletions(completions, args[0]);
        }

        if (args.length == 2) {
            if (sender.hasPermission("maxstaff.admin") && (args[0].equalsIgnoreCase("reset")
                    || args[0].equalsIgnoreCase("take") || args[0].equalsIgnoreCase("debugperm"))) {
                return null;
            }
        }

        if (args.length == 3 && sender.hasPermission("maxstaff.admin")) {
            if (args[0].equalsIgnoreCase("reset")) {
                completions.addAll(Arrays.asList("BAN", "MUTE", "KICK", "WARN", "ALL"));
            } else if (args[0].equalsIgnoreCase("take")) {
                completions.addAll(Arrays.asList("BAN", "MUTE", "KICK", "WARN"));
            } else if (args[0].equalsIgnoreCase("debugperm")) {
                completions.addAll(Arrays.asList("mute", "ban", "kick", "warn"));
            }
            return filterCompletions(completions, args[2]);
        }

        return completions;
    }

    private String boolColor(boolean value) {
        return value ? "&aYES" : "&cNO";
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
