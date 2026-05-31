package org.zkaleejoo.managers;

public record ActivePunishmentRecord(Type type, String targetName, String staff, String reason, long expiryMillis) {

    public enum Type {
        BAN,
        IP_BAN,
        MUTE
    }

    public boolean isPermanent() {
        return expiryMillis == -1L;
    }

    public boolean isExpired(long nowMillis) {
        return !isPermanent() && expiryMillis <= nowMillis;
    }

    public long remainingMillis(long nowMillis) {
        if (isPermanent()) {
            return -1L;
        }
        return Math.max(0L, expiryMillis - nowMillis);
    }
}
