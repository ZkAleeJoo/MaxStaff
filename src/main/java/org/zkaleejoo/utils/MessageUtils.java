package org.zkaleejoo.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

public class MessageUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)&([0-9A-FK-OR])");
    private static final char COLOR_CHAR = '\u00A7';
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character(COLOR_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    public static String getColoredMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String color = matcher.group(1);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(toLegacyHexColor(color)));
        }
        message = matcher.appendTail(buffer).toString();

        return LEGACY_COLOR_PATTERN.matcher(message).replaceAll(COLOR_CHAR + "$1");
    }

    public static void broadcastToPlayersOnly(String message) {
        if (message == null || message.isEmpty())
            return;
        String coloredMessage = getColoredMessage(message);
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (player != null) {
                player.sendMessage(coloredMessage);
            }
        }
    }

    public static String stripColor(String message) {
        if (message == null)
            return null;
        return PlainTextComponentSerializer.plainText()
                .serialize(toComponent(message));
    }

    public static Component legacyToComponentNoItalic(String message) {
        return toComponent(message)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static Component toComponent(String message) {
        return LEGACY_SERIALIZER.deserialize(getColoredMessage(message));
    }

    private static String toLegacyHexColor(String color) {
        StringBuilder builder = new StringBuilder(14);
        builder.append(COLOR_CHAR).append('x');
        for (char character : color.toCharArray()) {
            builder.append(COLOR_CHAR).append(character);
        }
        return builder.toString();
    }

}
