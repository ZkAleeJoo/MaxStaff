package org.zkaleejoo.utils;

import org.zkaleejoo.config.MainConfigManager;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BanScreen {
    private static final String DURATION = "{duration}";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(?:staff|reason|duration)\\}");

    private BanScreen() {
    }

    public static String render(MainConfigManager config, String staff, String reason, long remainingMillis) {
        String duration = TimeUtils.getCompactDurationString(remainingMillis, config.getTimeUnitPermanent());
        String screen = PLACEHOLDER.matcher(config.getScreenBan()).replaceAll(match -> Matcher.quoteReplacement(
                switch (match.group()) {
                    case "{staff}" -> Objects.toString(staff, "CONSOLE");
                    case "{reason}" -> Objects.toString(reason, "N/A");
                    default -> duration;
                }));
        return MessageUtils.getColoredMessage(screen);
    }

    public static String refreshStored(MainConfigManager config, String screen, long remainingMillis) {
        String template = config.getScreenBan();
        if (screen == null || remainingMillis == -1L || template == null || !template.contains(DURATION)) {
            return screen;
        }
        String legacyUnit = String.join("|",
                Pattern.quote(MessageUtils.getColoredMessage(config.getTimeUnitDays())),
                Pattern.quote(MessageUtils.getColoredMessage(config.getTimeUnitHours())),
                Pattern.quote(MessageUtils.getColoredMessage(config.getTimeUnitMinutes())),
                Pattern.quote(MessageUtils.getColoredMessage(config.getTimeUnitSeconds())));
        String durationPattern = "(\\d+[dhms](?: \\d+[dhms])*|\\d+ (?:" + legacyUnit + "))";

        String coloredTemplate = MessageUtils.getColoredMessage(template);
        Matcher placeholders = PLACEHOLDER.matcher(coloredTemplate);
        StringBuilder pattern = new StringBuilder();
        int literalStart = 0;
        while (placeholders.find()) {
            pattern.append(Pattern.quote(coloredTemplate.substring(literalStart, placeholders.start())));
            pattern.append(DURATION.equals(placeholders.group()) ? durationPattern : ".*?");
            literalStart = placeholders.end();
        }
        pattern.append(Pattern.quote(coloredTemplate.substring(literalStart)));

        Matcher stored = Pattern.compile(pattern.toString(), Pattern.DOTALL).matcher(screen);
        if (!stored.matches()) {
            return screen;
        }

        String duration = TimeUtils.getCompactDurationString(remainingMillis, config.getTimeUnitPermanent());
        StringBuilder updated = new StringBuilder(screen);
        for (int group = stored.groupCount(); group >= 1; group--) {
            updated.replace(stored.start(group), stored.end(group), duration);
        }
        return updated.toString();
    }
}
