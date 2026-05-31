package org.zkaleejoo.commands.registration;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.commands.DisabledModuleCommand;
import java.util.Objects;

public final class PluginCommandController {

    private final MaxStaff plugin;
    private final String fallbackPrefix;
    private final DisabledModuleCommand disabledCommand = new DisabledModuleCommand();
    private final Map<String, PluginCommand> declaredCommands = new LinkedHashMap<>();
    private final Set<String> missingDeclaredCommands = new LinkedHashSet<>();
    private final SimpleCommandMap commandMap;
    private final Map<String, Command> knownCommands;
    private boolean commandMapWarningLogged;
    private boolean knownCommandsMutationWarningLogged;

    public PluginCommandController(MaxStaff plugin) {
        this.plugin = plugin;
        this.fallbackPrefix = plugin.getName().toLowerCase(Locale.ROOT);
        this.commandMap = resolveCommandMap(plugin);
        this.knownCommands = resolveKnownCommands(commandMap);
        cacheDeclaredCommands();
    }

    public void enableCommand(String name, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand command = getDeclaredCommand(name);
        if (command == null) {
            warnMissingDeclaredCommand(name);
            return;
        }

        command.setExecutor(executor);
        command.setTabCompleter(tabCompleter);

        if (plugin.getMainConfigManager().isPluginCommandDisabled(command.getName())) {
            disableCommand(command);
            return;
        }

        if (!hasCommandMapAccess()) {
            warnCommandMapUnavailable();
            return;
        }

        if (requiresRegistrationRefresh(command)) {
            unregisterCommand(command);
            commandMap.register(fallbackPrefix, command);
        }
        removeDisabledAliases(command);
    }

    public void disableCommand(String name) {
        PluginCommand command = getDeclaredCommand(name);
        if (command == null) {
            warnMissingDeclaredCommand(name);
            return;
        }

        disableCommand(command);
    }

    public void warnUnknownDisabledCommands() {
        Set<String> disabledCommands = plugin.getMainConfigManager().getDisabledPluginCommands();
        if (disabledCommands.isEmpty()) {
            return;
        }

        Set<String> declaredLabels = new LinkedHashSet<>();
        for (PluginCommand command : declaredCommands.values()) {
            declaredLabels.add(normalizeLabel(command.getName()));
            for (String alias : command.getAliases()) {
                declaredLabels.add(normalizeLabel(alias));
            }
        }

        for (String disabledCommandLabel : disabledCommands) {
            if (!declaredLabels.contains(disabledCommandLabel)) {
                plugin.getLogger().warning("Disabled command label \"" + disabledCommandLabel
                        + "\" does not belong to MaxStaff and will be ignored.");
            }
        }
    }

    private void disableCommand(PluginCommand command) {
        if (!hasCommandMapAccess()) {
            command.setExecutor(disabledCommand);
            command.setTabCompleter(disabledCommand);
            warnCommandMapUnavailable();
            return;
        }

        Set<String> releasedLabels = getPlainLabels(command);
        unregisterCommand(command);
        for (String label : releasedLabels) {
            promoteReplacement(label, command);
        }
    }

    private void removeDisabledAliases(PluginCommand command) {
        for (String alias : command.getAliases()) {
            if (!plugin.getMainConfigManager().isPluginCommandDisabled(alias)) {
                continue;
            }

            if (removeCommandLabel(command, alias)) {
                promoteReplacement(alias, command);
            }
        }
    }

    private boolean removeCommandLabel(PluginCommand command, String label) {
        String normalizedLabel = normalizeLabel(label);
        return removeKnownCommands(command, normalizedLabel);
    }

    private void unregisterCommand(PluginCommand command) {
        command.unregister(commandMap);
        removeKnownCommands(command, null);
    }

    private void promoteReplacement(String label, PluginCommand removedCommand) {
        String normalizedLabel = normalizeLabel(label);
        if (normalizedLabel.isEmpty() || knownCommands.containsKey(normalizedLabel)) {
            return;
        }

        Command replacement = findReplacementCommand(normalizedLabel, removedCommand);
        if (replacement != null) {
            knownCommands.put(normalizedLabel, replacement);
        }
    }

    private Command findReplacementCommand(String label, PluginCommand removedCommand) {
        for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
            Command candidate = entry.getValue();
            if (candidate == removedCommand || isOwnedByThisPlugin(candidate)) {
                continue;
            }

            if (extractBaseLabel(entry.getKey()).equals(label) || normalizeLabel(candidate.getName()).equals(label)) {
                return candidate;
            }

            for (String alias : candidate.getAliases()) {
                if (normalizeLabel(alias).equals(label)) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private boolean isOwnedByThisPlugin(Command command) {
        if (!(command instanceof PluginCommand pluginCommand)) {
            return false;
        }

        return pluginCommand.getPlugin().equals(plugin);
    }

    private PluginCommand getDeclaredCommand(String name) {
        return declaredCommands.get(normalizeLabel(name));
    }

    private void warnMissingDeclaredCommand(String name) {
        String normalized = normalizeLabel(name);
        if (normalized.isEmpty() || !missingDeclaredCommands.add(normalized)) {
            return;
        }

        plugin.getLogger().warning("Command \"" + normalized
                + "\" was requested but is not declared/registered from plugin.yml. The command will not work until it is declared under commands: in plugin.yml.");
    }

    private boolean requiresRegistrationRefresh(PluginCommand command) {
        if (!isRegisteredLabel(command, command.getName())) {
            return true;
        }

        for (String alias : command.getAliases()) {
            if (plugin.getMainConfigManager().isPluginCommandDisabled(alias)) {
                continue;
            }

            if (!isRegisteredLabel(command, alias)) {
                return true;
            }
        }

        return false;
    }

    private boolean isRegisteredLabel(PluginCommand command, String label) {
        String normalizedLabel = normalizeLabel(label);
        if (normalizedLabel.isEmpty()) {
            return false;
        }

        for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
            if (entry.getValue() == command && extractBaseLabel(entry.getKey()).equals(normalizedLabel)) {
                return true;
            }
        }

        return false;
    }

    private boolean removeKnownCommands(PluginCommand command, String labelFilter) {
        LinkedHashSet<String> labelsToRemove = new LinkedHashSet<>();
        for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
            if (entry.getValue() != command) {
                continue;
            }

            if (labelFilter != null && !extractBaseLabel(entry.getKey()).equals(labelFilter)) {
                continue;
            }

            labelsToRemove.add(entry.getKey());
        }

        if (labelsToRemove.isEmpty()) {
            return true;
        }

        try {
            for (String label : labelsToRemove) {
                knownCommands.remove(label, command);
            }
            return true;
        } catch (UnsupportedOperationException exception) {
            warnKnownCommandsMutationUnavailable(exception);
            return false;
        }
    }

    private Set<String> getPlainLabels(PluginCommand command) {
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(normalizeLabel(command.getName()));
        for (String alias : command.getAliases()) {
            labels.add(normalizeLabel(alias));
        }
        labels.remove("");
        return labels;
    }

    private boolean hasCommandMapAccess() {
        return commandMap != null && knownCommands != null;
    }

    private void cacheDeclaredCommands() {
        ConfigurationSection commandsSection = loadPluginCommandsSection();
        if (commandsSection == null) {
            plugin.getLogger().warning("Could not read commands from plugin.yml.");
            return;
        }

        for (String commandName : commandsSection.getKeys(false)) {
            PluginCommand command = plugin.getCommand(Objects.requireNonNull(commandName));
            if (command == null) {
                plugin.getLogger().warning("Command \"" + commandName + "\" is missing in plugin.yml.");
                continue;
            }

            declaredCommands.put(normalizeLabel(commandName), command);
        }

        if (declaredCommands.isEmpty()) {
            plugin.getLogger().warning(
                    "No commands were registered from plugin.yml. If commands do not work, ensure the server loaded the correct plugin jar and that plugin.yml is valid.");
        } else if (plugin.getMainConfigManager() != null && plugin.getMainConfigManager().isUpdateCheckEnabled()) {
            // Keep this log at FINE to avoid clutter unless the server enables fine logging.
            plugin.getLogger().fine("Loaded " + declaredCommands.size() + " command(s) from plugin.yml.");
        }
    }

    private ConfigurationSection loadPluginCommandsSection() {
        try (java.io.InputStream inputStream = plugin.getResource("plugin.yml")) {
            if (inputStream == null) {
                return null;
            }

            YamlConfiguration pluginYaml = YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8));
            return pluginYaml.getConfigurationSection("commands");
        } catch (java.io.IOException exception) {
            plugin.getLogger().warning("Could not load plugin.yml commands: " + exception.getMessage());
            return null;
        }
    }

    private void warnCommandMapUnavailable() {
        if (commandMapWarningLogged) {
            return;
        }

        commandMapWarningLogged = true;
        plugin.getLogger().warning(
                "Could not access the Bukkit command map. Disabled commands will be muted, but command ownership may remain.");
    }

    private void warnKnownCommandsMutationUnavailable(UnsupportedOperationException exception) {
        if (knownCommandsMutationWarningLogged) {
            return;
        }

        knownCommandsMutationWarningLogged = true;
        plugin.getLogger().warning(
                "Could not update Bukkit knownCommands for dynamic command toggling on this server implementation: "
                        + exception.getMessage());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> resolveKnownCommands(SimpleCommandMap simpleCommandMap) {
        if (simpleCommandMap == null) {
            return null;
        }

        try {
            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            return (Map<String, Command>) knownCommandsField.get(simpleCommandMap);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Could not access knownCommands from Bukkit command map: "
                    + exception.getMessage());
            return null;
        }
    }

    private SimpleCommandMap resolveCommandMap(MaxStaff plugin) {
        CommandMap publicCommandMap = Bukkit.getCommandMap();
        if (publicCommandMap instanceof SimpleCommandMap simpleCommandMap) {
            return simpleCommandMap;
        }

        try {
            Field commandMapField = findField(plugin.getServer().getClass(), "commandMap");
            if (commandMapField == null) {
                throw new NoSuchFieldException("commandMap");
            }
            commandMapField.setAccessible(true);
            return (SimpleCommandMap) commandMapField.get(plugin.getServer());
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger()
                    .warning("Could not access Bukkit command map for dynamic command toggling: "
                            + exception.getMessage());
            return null;
        }
    }

    private String normalizeLabel(String label) {
        if (label == null || label.isBlank()) {
            return "";
        }

        String normalized = label.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        return normalized;
    }

    private String extractBaseLabel(String label) {
        String normalized = normalizeLabel(label);
        int separatorIndex = normalized.indexOf(':');
        if (separatorIndex == -1) {
            return normalized;
        }

        return normalized.substring(separatorIndex + 1);
    }

    private Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }

        return null;
    }
}
