package org.zkaleejoo.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SilentPunishmentCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUPPORTED_ACTIONS = Arrays.asList("ban", "tempban", "mute", "tempmute", "kick",
            "warn", "ban-ip", "tempban-ip");
    private final MaxStaff plugin;

    public SilentPunishmentCommand(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix()
                    + plugin.getMainConfigManager().getSilentPunishmentUse()));
            return true;
        }

        String action = args[0].toLowerCase();
        if (!SUPPORTED_ACTIONS.contains(action)) {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix()
                    + plugin.getMainConfigManager().getSilentInvalid()));
            return true;
        }

        String baseAction = normalizeAction(action);
        String permission = baseAction.equals("ban-ip") ? "maxstaff.punish.banip" : "maxstaff.punish." + baseAction;
        if (!CommandContextUtil.hasPermissionOrAdmin(sender, permission)) {
            sender.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()));
            return true;
        }

        String target = args[1];

        if (baseAction.equals("kick") || baseAction.equals("warn")) {
            String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                    : plugin.getMainConfigManager().getNoReason();
            if (baseAction.equals("kick")) {
                plugin.getPunishmentManager().kickPlayerSilent(sender, target, reason);
            } else {
                plugin.getPunishmentManager().warnPlayerSilent(sender, target, reason);
            }
            return true;
        }

        String time = "perm";
        int reasonStartIndex = 2;

        if (args.length > 2) {
            String token = args[2].toLowerCase();
            boolean looksLikeTime = TimeUtils.isValidDurationToken(token);
            boolean looksLikeDurationAttempt = !token.isEmpty()
                    && (Character.isDigit(token.charAt(0)) || token.startsWith("perm"));

            if (looksLikeDurationAttempt && !looksLikeTime) {
                sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix()
                        + plugin.getMainConfigManager().getSilentFormatInvalid()));
                return true;
            }

            if (looksLikeTime) {
                time = token;
                reasonStartIndex = 3;
            }
        }

        String reason = args.length > reasonStartIndex
                ? String.join(" ", Arrays.copyOfRange(args, reasonStartIndex, args.length))
                : plugin.getMainConfigManager().getNoReason();

        if (baseAction.equals("ban")) {
            if (!validateDurationLimit(sender, "ban", time)) {
                return true;
            }
            plugin.getPunishmentManager().banPlayerSilent(sender, target, reason, time);
        } else if (baseAction.equals("mute")) {
            if (!validateDurationLimit(sender, "mute", time)) {
                return true;
            }
            plugin.getPunishmentManager().mutePlayerSilent(sender, target, reason, time);
        } else if (baseAction.equals("ban-ip")) {
            if (!validateDurationLimit(sender, "ban-ip", time)) {
                return true;
            }
            plugin.getPunishmentManager().banIPPlayerSilent(sender, target, reason, time);
        }

        return true;
    }

    private String normalizeAction(String action) {
        if (action.equals("tempban")) {
            return "ban";
        }
        if (action.equals("tempmute")) {
            return "mute";
        }
        if (action.equals("tempban-ip")) {
            return "ban-ip";
        }
        return action;
    }

    private boolean validateDurationLimit(CommandSender sender, String punishmentType, String durationToken) {
        if (plugin.getMainConfigManager().isPunishmentDurationAllowed(sender, punishmentType, durationToken)) {
            return true;
        }

        long maxLimit = plugin.getMainConfigManager().getPunishmentDurationLimit(sender, punishmentType);
        String maxDisplay = maxLimit == Long.MIN_VALUE
                ? plugin.getMainConfigManager().getTimeUnitPermanent()
                : TimeUtils.getDurationString(maxLimit, plugin.getMainConfigManager());
        String durationDisplay = TimeUtils.getDurationString(TimeUtils.parseDuration(durationToken),
                plugin.getMainConfigManager());
        String message = plugin.getMainConfigManager().getMsgPunishmentLimitExceeded()
                .replace("{type}", punishmentType.toUpperCase())
                .replace("{duration}", durationDisplay)
                .replace("{max}", maxDisplay);
        sender.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + message));
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return SUPPORTED_ACTIONS.stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String action = args[0].toLowerCase();
            if (!SUPPORTED_ACTIONS.contains(action)) {
                return new ArrayList<>();
            }

            String baseAction = normalizeAction(action);
            String permission = baseAction.equals("ban-ip") ? "maxstaff.punish.banip" : "maxstaff.punish." + baseAction;
            if (!CommandContextUtil.hasPermissionOrAdmin(sender, permission)) {
                return new ArrayList<>();
            }

            return CommandContextUtil.filterOnlinePlayerNamesByPrefix(args[1]);
        }

        if (args.length == 3) {
            String base = normalizeAction(args[0].toLowerCase());
            if (base.equals("ban") || base.equals("mute") || base.equals("ban-ip")) {
                return Arrays.asList("1h", "1d", "7d", "30d", "perm").stream()
                        .filter(value -> value.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
