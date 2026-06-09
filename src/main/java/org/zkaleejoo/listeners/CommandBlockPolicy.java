package org.zkaleejoo.listeners;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

final class CommandBlockPolicy {

    private CommandBlockPolicy() {
    }

    static Set<String> normalizeConfiguredCommands(Collection<String> commands) {
        if (commands == null || commands.isEmpty()) {
            return Set.of();
        }

        LinkedHashSet<String> normalizedCommands = new LinkedHashSet<>();
        for (String command : commands) {
            String normalized = normalizeCommandLabel(command);
            if (!normalized.isEmpty()) {
                normalizedCommands.add(normalized);
            }
        }

        return normalizedCommands.isEmpty() ? Set.of() : Set.copyOf(normalizedCommands);
    }

    static boolean isBlocked(String rawCommand, Set<String> blockedCommands) {
        if (blockedCommands == null || blockedCommands.isEmpty()) {
            return false;
        }

        Set<String> normalizedBlockedCommands = normalizeConfiguredCommands(blockedCommands);
        if (normalizedBlockedCommands.isEmpty()) {
            return false;
        }

        String commandLabel = normalizeCommandLabel(rawCommand);
        if (commandLabel.isEmpty()) {
            return false;
        }

        if (normalizedBlockedCommands.contains(commandLabel)) {
            return true;
        }

        int namespaceSeparator = commandLabel.indexOf(':');
        return namespaceSeparator >= 0
                && namespaceSeparator + 1 < commandLabel.length()
                && normalizedBlockedCommands.contains(commandLabel.substring(namespaceSeparator + 1));
    }

    private static String normalizeCommandLabel(String command) {
        if (command == null || command.isBlank()) {
            return "";
        }

        String normalized = command.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        int argumentStart = normalized.indexOf(' ');
        if (argumentStart >= 0) {
            normalized = normalized.substring(0, argumentStart);
        }

        return normalized.trim();
    }
}
