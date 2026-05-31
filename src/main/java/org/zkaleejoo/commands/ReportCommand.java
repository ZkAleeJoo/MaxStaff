package org.zkaleejoo.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.zkaleejoo.managers.ReportManager;
import org.zkaleejoo.config.MainConfigManager.ClickActionType;

public class ReportCommand implements CommandExecutor, TabCompleter {

    private final MaxStaff plugin;

    public ReportCommand(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgConsole()));
            return true;
        }

        Player reporter = (Player) sender;

        if (!plugin.getMainConfigManager().isReportEnabled()) {
            reporter.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getReportDisabled()));
            return true;
        }

        if (!reporter.hasPermission("maxstaff.report")) {
            reporter.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getNoPermission()));
            return true;
        }

        if (args.length < 2) {
            reporter.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getReportUse()));
            return true;
        }

        String targetName = args[0];
        if (!plugin.getMainConfigManager().isReportSelfAllowed() && reporter.getName().equalsIgnoreCase(targetName)) {
            reporter.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getReportSelf()));
            return true;
        }

        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        if (plugin.getMainConfigManager().isReportRequireOnlineTarget() && onlineTarget == null) {
            reporter.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getReportOffline()));
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        int minLength = plugin.getMainConfigManager().getReportMinReasonLength();
        int maxLength = plugin.getMainConfigManager().getReportMaxReasonLength();

        if (reason.length() < minLength) {
            reporter.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getReportReasonShort()
                            .replace("{min}", String.valueOf(minLength))));
            return true;
        }

        if (maxLength > 0 && reason.length() > maxLength) {
            reporter.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getReportReasonLong()
                            .replace("{max}", String.valueOf(maxLength))));
            return true;
        }

        String bypassPermission = plugin.getMainConfigManager().getReportCooldownBypassPermission();
        if (bypassPermission == null || bypassPermission.isEmpty() || !reporter.hasPermission(bypassPermission)) {
            long remaining = plugin.getReportManager().getCooldownRemaining(reporter.getUniqueId());
            if (remaining > 0) {
                reporter.sendMessage(MessageUtils.getColoredMessage(
                        plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getReportCooldown()
                                .replace("{seconds}", String.valueOf(remaining))));
                return true;
            }
        }

        OfflinePlayer target = onlineTarget != null ? onlineTarget : Bukkit.getOfflinePlayer(targetName);
        String resolvedTargetName = target.getName() != null ? target.getName() : targetName;
        UUID targetUuid = target.getUniqueId();
        String worldName = reporter.getWorld().getName();
        org.bukkit.Location loc = reporter.getLocation();
        int x = loc != null ? loc.getBlockX() : 0;
        int y = loc != null ? loc.getBlockY() : 0;
        int z = loc != null ? loc.getBlockZ() : 0;

        ReportManager.ReportRecordMeta reportMeta = plugin.getReportManager().recordReport(reporter.getName(),
                reporter.getUniqueId(),
                resolvedTargetName, targetUuid, reason, worldName, x, y, z);
        plugin.getReportManager().markReported(reporter.getUniqueId());

        if (plugin.getDiscordManager() != null) {
            plugin.getDiscordManager().sendReportWebhook(reporter.getName(), resolvedTargetName, reason, worldName, x,
                    y, z);
        }

        if (plugin.getMainConfigManager().isReportNotifyEnabled()) {
            String notifyPermission = plugin.getMainConfigManager().getReportNotifyPermission();
            String notifyMessage = plugin.getMainConfigManager().getReportStaffNotify()
                    .replace("{reporter}", reporter.getName())
                    .replace("{target}", resolvedTargetName)
                    .replace("{reason}", reason)
                    .replace("{world}", worldName)
                    .replace("{x}", String.valueOf(x))
                    .replace("{y}", String.valueOf(y))
                    .replace("{z}", String.valueOf(z))
                    .replace("{id}", reportMeta.id())
                    .replace("{sequence}", String.valueOf(reportMeta.sequentialIndex()));

            String clickAction = plugin.getMainConfigManager().getReportStaffClickCommandTemplate()
                    .replace("{reporter}", reporter.getName())
                    .replace("{target}", resolvedTargetName)
                    .replace("{world}", worldName)
                    .replace("{x}", String.valueOf(x))
                    .replace("{y}", String.valueOf(y))
                    .replace("{z}", String.valueOf(z))
                    .replace("{reason}", reason)
                    .replace("{id}", reportMeta.id())
                    .replace("{sequence}", String.valueOf(reportMeta.sequentialIndex()));

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online == null) continue;
                if (notifyPermission != null && !notifyPermission.isEmpty()
                        && !online.hasPermission(notifyPermission)) {
                    continue;
                }

                String parsedAction = clickAction.replace("{staff}", online.getName());
                Component notification = MessageUtils.legacyToComponentNoItalic(notifyMessage);

                if (plugin.getMainConfigManager().isReportStaffClickEnabled() && parsedAction != null
                        && !parsedAction.isBlank()) {
                    Component hover = Component.empty();
                    boolean hasHoverLines = false;
                    for (String hoverLine : plugin.getMainConfigManager().getReportStaffNotifyHover()) {
                        String parsedHover = hoverLine
                                .replace("{staff}", online.getName())
                                .replace("{reporter}", reporter.getName())
                                .replace("{target}", resolvedTargetName)
                                .replace("{world}", worldName)
                                .replace("{x}", String.valueOf(x))
                                .replace("{y}", String.valueOf(y))
                                .replace("{z}", String.valueOf(z))
                                .replace("{reason}", reason)
                                .replace("{id}", reportMeta.id())
                                .replace("{sequence}", String.valueOf(reportMeta.sequentialIndex()));
                        if (hasHoverLines) {
                            hover = hover.append(Component.newline());
                        }
                        hover = hover.append(MessageUtils.legacyToComponentNoItalic(parsedHover));
                        hasHoverLines = true;
                    }

                    if (plugin.getMainConfigManager().getReportStaffClickActionType() == ClickActionType.RUN_COMMAND) {
                        notification = notification.clickEvent(ClickEvent.runCommand(parsedAction));
                    } else {
                        notification = notification.clickEvent(ClickEvent.suggestCommand(parsedAction));
                    }
                    if (hasHoverLines) {
                        notification = notification.hoverEvent(HoverEvent.showText(hover));
                    }
                }

                online.sendMessage(notification);

            }
        }

        reporter.sendMessage(MessageUtils.getColoredMessage(
                plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getReportSent()
                        .replace("{id}", reportMeta.id())
                        .replace("{sequence}", String.valueOf(reportMeta.sequentialIndex()))
                        .replace("{target}", resolvedTargetName)));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}