package org.zkaleejoo.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FlyCommand implements CommandExecutor, TabCompleter {

    private static final String FLY_PERMISSION = "maxstaff.fly";
    private static final float MIN_FLY_SPEED = 0.1F;
    private static final float MAX_FLY_SPEED = 1.0F;

    private final MaxStaff plugin;

    public FlyCommand(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandContextUtil.requirePlayer(sender, plugin.getMainConfigManager());
        if (player == null) {
            return true;
        }

        if (!CommandContextUtil.requirePermission(player, FLY_PERMISSION, plugin.getMainConfigManager())) {
            return true;
        }

        if (args.length == 0) {
            toggleFly(player);
            return true;
        }

        Integer level = parseLevel(args[0]);
        if (level == null) {
            CommandContextUtil.sendPrefixed(player,
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getFlyUse());
            return true;
        }

        enableFlyWithLevel(player, level);
        return true;
    }

    private void toggleFly(Player player) {
        boolean enable = !player.getAllowFlight();
        player.setAllowFlight(enable);

        if (!enable) {
            player.setFlying(false);
            player.setFlySpeed(MIN_FLY_SPEED);
            CommandContextUtil.sendPrefixed(player,
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getFlyDisabled());
            return;
        }

        player.setFlying(true);
        player.setFlySpeed(plugin.getMainConfigManager().getFlySpeedLevel1());
        CommandContextUtil.sendPrefixed(player,
                plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getFlyEnabledDefault());
    }

    private void enableFlyWithLevel(Player player, int level) {
        float speed = switch (level) {
            case 1 -> plugin.getMainConfigManager().getFlySpeedLevel1();
            case 2 -> plugin.getMainConfigManager().getFlySpeedLevel2();
            case 3 -> plugin.getMainConfigManager().getFlySpeedLevel3();
            default -> MIN_FLY_SPEED;
        };

        speed = Math.max(MIN_FLY_SPEED, Math.min(MAX_FLY_SPEED, speed));

        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(speed);

        String message = plugin.getMainConfigManager().getFlyEnabledLevel()
                .replace("{level}", String.valueOf(level))
                .replace("{speed}", String.format(Locale.US, "%.1f", speed));
        CommandContextUtil.sendPrefixed(player, plugin.getMainConfigManager().getPrefix() + message);
    }

    private Integer parseLevel(String input) {
        try {
            int level = Integer.parseInt(input);
            return (level >= 1 && level <= 3) ? level : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("1", "2", "3").stream()
                    .filter(option -> option.startsWith(args[0]))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}