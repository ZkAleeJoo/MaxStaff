package org.zkaleejoo.utils;

public class TimeUtils {

    public static long parseDuration(String arg) {
        if (arg.equalsIgnoreCase("perm") || arg.equalsIgnoreCase("permanent")) {
            return -1; 
        }

        try {
            long value = Long.parseLong(arg.substring(0, arg.length() - 1));
            char unit = arg.toLowerCase().charAt(arg.length() - 1);

            switch (unit) {
                case 's': return value * 1000;
                case 'm': return value * 60 * 1000;
                case 'h': return value * 60 * 60 * 1000;
                case 'd': return value * 24 * 60 * 60 * 1000;
                case 'w': return value * 7 * 24 * 60 * 60 * 1000; 
                default: return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getDurationString(long duration) {
        if (duration == -1) return "Permanente";
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return days + " días";
        if (hours > 0) return hours + " horas";
        if (minutes > 0) return minutes + " minutos";
        return seconds + " segundos";
    }
}