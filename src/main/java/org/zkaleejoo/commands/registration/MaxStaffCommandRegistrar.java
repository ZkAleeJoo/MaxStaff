package org.zkaleejoo.commands.registration;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.commands.AltsCommand;
import org.zkaleejoo.commands.ChatCommand;
import org.zkaleejoo.commands.CommandSpyCommand;
import org.zkaleejoo.commands.FlyCommand;
import org.zkaleejoo.commands.FreezeCommand;
import org.zkaleejoo.commands.GameModeCommand;
import org.zkaleejoo.commands.InvSeeCommand;
import org.zkaleejoo.commands.MainCommand;
import org.zkaleejoo.commands.PunishmentCommand;
import org.zkaleejoo.commands.ReportCommand;
import org.zkaleejoo.commands.ReviveCommand;
import org.zkaleejoo.commands.SanctionCommand;
import org.zkaleejoo.commands.SilentPunishmentCommand;
import org.zkaleejoo.commands.StaffChatCommand;
import org.zkaleejoo.commands.StaffCommand;
import org.zkaleejoo.commands.VanishCommand;
import org.zkaleejoo.commands.XrayCommand;

public final class MaxStaffCommandRegistrar {

    private static final String[] PUNISHMENT_COMMANDS = {
            "ban", "tempban", "mute", "tempmute", "kick",
            "unban", "unmute", "warn", "history",
            "ban-ip", "tempban-ip", "unban-ip"
    };

    private final MaxStaff plugin;
    private final PluginCommandController commandController;

    public MaxStaffCommandRegistrar(MaxStaff plugin) {
        this.plugin = plugin;
        this.commandController = new PluginCommandController(plugin);
    }

    public void registerCommands() {
        MainCommand mainCommand = new MainCommand(plugin);
        enableCommand("maxstaff", mainCommand, mainCommand);

        if (plugin.isModuleEnabled("staff-mode")) {
            enableCommand("staff", new StaffCommand(plugin), null);
            enableCommand("vanish", new VanishCommand(plugin), null);
        } else {
            disableCommands("staff", "vanish");
        }

        boolean staffChatEnabled = plugin.isModuleEnabled("chat")
                || plugin.isModuleEnabled("punishments")
                || plugin.isModuleEnabled("staff-mode");
        if (staffChatEnabled) {
            enableCommand("sc", new StaffChatCommand(plugin), null);
        } else {
            disableCommands("sc");
        }

        if (plugin.isModuleEnabled("command-spy")) {
            enableCommand("cmdspy", new CommandSpyCommand(plugin), null);
        } else {
            disableCommands("cmdspy");
        }

        if (plugin.isModuleEnabled("chat")) {
            ChatCommand chatCommand = new ChatCommand(plugin);
            enableCommand("chat", chatCommand, chatCommand);
        } else {
            disableCommands("chat");
        }

        if (plugin.isModuleEnabled("gamemode-gui")) {
            GameModeCommand gameModeCommand = new GameModeCommand(plugin);
            enableCommand("gamemode", gameModeCommand, gameModeCommand);
        } else {
            disableCommands("gamemode");
        }

        if (plugin.isModuleEnabled("alts")) {
            enableCommand("alts", new AltsCommand(plugin), null);
        } else {
            disableCommands("alts");
        }

        if (plugin.isModuleEnabled("sanctions-gui") && plugin.isModuleEnabled("punishments")) {
            SanctionCommand sanctionCommand = new SanctionCommand(plugin);
            enableCommand("sanction", sanctionCommand, sanctionCommand);
        } else {
            disableCommands("sanction");
        }

        if (plugin.isModuleEnabled("freeze")) {
            FreezeCommand freezeCommand = new FreezeCommand(plugin);
            enableCommand("freeze", freezeCommand, freezeCommand);
            enableCommand("unfreeze", freezeCommand, freezeCommand);
        } else {
            disableCommands("freeze", "unfreeze");
        }

        if (plugin.isModuleEnabled("anti-xray")) {
            enableCommand("xray", new XrayCommand(plugin), null);
        } else {
            disableCommands("xray");
        }

        if (plugin.isModuleEnabled("reports")) {
            ReportCommand reportCommand = new ReportCommand(plugin);
            enableCommand("report", reportCommand, reportCommand);
        } else {
            disableCommands("report");
        }

        if (plugin.isModuleEnabled("invsee")) {
            InvSeeCommand invSeeCommand = new InvSeeCommand(plugin);
            enableCommand("invsee", invSeeCommand, invSeeCommand);
        } else {
            disableCommands("invsee");
        }

        if (plugin.isModuleEnabled("revive")) {
            enableCommand("revive", new ReviveCommand(plugin), null);
        } else {
            disableCommands("revive");
        }

        FlyCommand flyCommand = new FlyCommand(plugin);
        enableCommand("fly", flyCommand, flyCommand);

        if (plugin.isModuleEnabled("punishments")) {
            PunishmentCommand punishmentCommand = new PunishmentCommand(plugin);
            SilentPunishmentCommand silentPunishmentCommand = new SilentPunishmentCommand(plugin);
            enableCommand("silent", silentPunishmentCommand, silentPunishmentCommand);
            for (String commandName : PUNISHMENT_COMMANDS) {
                enableCommand(commandName, punishmentCommand, punishmentCommand);
            }
        } else {
            disableCommands("silent");
            disableCommands(PUNISHMENT_COMMANDS);
        }

        commandController.warnUnknownDisabledCommands();
    }

    private void enableCommand(String name, CommandExecutor executor, TabCompleter tabCompleter) {
        commandController.enableCommand(name, executor, tabCompleter);
    }

    private void disableCommands(String... commandNames) {
        for (String commandName : commandNames) {
            commandController.disableCommand(commandName);
        }
    }
}
