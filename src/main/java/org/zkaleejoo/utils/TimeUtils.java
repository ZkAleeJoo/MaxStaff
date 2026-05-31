package org.zkaleejoo.utils;

import org.zkaleejoo.config.MainConfigManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtils {

    private static final Pattern DURATION_TOKEN_PATTERN = Pattern.compile("^(\\d+)([smhdw])$|^(perm|permanent|permanentemente)$");
    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d+)([smhdw])$");

    public static boolean isValidDurationToken(String arg) {
        if (arg == null || arg.isEmpty()) return false;
        return DURATION_TOKEN_PATTERN.matcher(arg.toLowerCase().trim()).matches();
    }

    public static long parseDuration(String arg) {
        if (!isValidDurationToken(arg)) {
            return 0;
        }
        arg = arg.toLowerCase().trim();

        if (arg.equals("perm") || arg.equals("permanent") || arg.equals("permanentemente")) return -1;

        Matcher matcher = TIME_PATTERN.matcher(arg);
        if (!matcher.matches()) return 0;

        try {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "s": return value * 1000L;
                case "m": return value * 60L * 1000L;
                case "h": return value * 60L * 60L * 1000L;
                case "d": return value * 24L * 60L * 60L * 1000L;
                case "w": return value * 7L * 24L * 60L * 60L * 1000L;
                default: return 0;
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String getDurationString(long duration, MainConfigManager config) {
        if (duration == -1) return config.getTimeUnitPermanent();
        if (duration <= 0) return "0 " + config.getTimeUnitSeconds(); 
        
        long seconds = duration / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        long days = hours / 24L;
        
        if (days > 0) return days + " " + config.getTimeUnitDays();
        if (hours > 0) return hours + " " + config.getTimeUnitHours();
        if (minutes > 0) return minutes + " " + config.getTimeUnitMinutes();
        return seconds + " " + config.getTimeUnitSeconds();
    }

    public static String getDetailedDurationString(long duration, MainConfigManager config) {
        return getDetailedDurationString(
                duration,
                config.getTimeUnitPermanent(),
                config.getTimeUnitDays(),
                config.getTimeUnitHours(),
                config.getTimeUnitMinutes(),
                config.getTimeUnitSeconds());
    }

    public static String getDetailedDurationString(long duration, String permanent, String daysUnit,
            String hoursUnit, String minutesUnit, String secondsUnit) {
        if (duration == -1L) {
            return permanent;
        }
        if (duration <= 0L) {
            return "0 " + secondsUnit;
        }

        long totalSeconds = duration / 1000L;
        long days = totalSeconds / 86_400L;
        long hours = (totalSeconds % 86_400L) / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;

        StringBuilder result = new StringBuilder();
        appendDurationPart(result, days, daysUnit);
        appendDurationPart(result, hours, hoursUnit);
        appendDurationPart(result, minutes, minutesUnit);
        appendDurationPart(result, seconds, secondsUnit);

        return result.isEmpty() ? "0 " + secondsUnit : result.toString();
    }

    private static void appendDurationPart(StringBuilder result, long value, String unit) {
        if (value <= 0L) {
            return;
        }
        if (!result.isEmpty()) {
            result.append(' ');
        }
        result.append(value).append(' ').append(unit);
    }
}
