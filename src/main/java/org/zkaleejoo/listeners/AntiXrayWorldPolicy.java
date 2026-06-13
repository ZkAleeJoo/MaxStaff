package org.zkaleejoo.listeners;

import java.util.Locale;
import java.util.Set;

final class AntiXrayWorldPolicy {

    private AntiXrayWorldPolicy() {
    }

    static boolean isMonitoringEnabled(Set<String> blacklistedWorlds, String worldName) {
        if (blacklistedWorlds == null || blacklistedWorlds.isEmpty() || worldName == null || worldName.isBlank()) {
            return true;
        }

        return !blacklistedWorlds.contains(worldName.toLowerCase(Locale.ROOT));
    }
}
