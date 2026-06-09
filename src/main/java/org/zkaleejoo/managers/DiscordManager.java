package org.zkaleejoo.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.CustomConfig;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DiscordManager {

    private final MaxStaff plugin;
    private CustomConfig discordConfig;

    public DiscordManager(MaxStaff plugin) {
        this.plugin = plugin;
        this.discordConfig = new CustomConfig("discord.yml", null, plugin, false);
        this.discordConfig.registerConfig();
    }

    public void sendWebhook(String type, String target, String staff, String reason, String duration, String count) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("target", target != null ? target : "N/A");
        placeholders.put("staff", staff != null ? staff : "Console");
        placeholders.put("reason", reason != null ? reason : "N/A");
        placeholders.put("duration", duration != null ? duration : "N/A");
        placeholders.put("count", count != null ? count : "0");
        sendWebhook(type, placeholders, target != null ? target : staff);
    }

    public void sendReportWebhook(String reporter, String target, String reason, String world, int x, int y, int z) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reporter", reporter != null ? reporter : "Unknown");
        placeholders.put("target", target != null ? target : "Unknown");
        placeholders.put("reason", reason != null ? reason : "No reason provided");
        placeholders.put("world", world != null ? world : "Unknown");
        placeholders.put("x", String.valueOf(x));
        placeholders.put("y", String.valueOf(y));
        placeholders.put("z", String.valueOf(z));
        placeholders.put("timestamp", new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));

        String type = "new";
        if (!discordConfig.getConfig().isConfigurationSection("reports." + type)) {
            type = discordConfig.getConfig().isConfigurationSection("reports.report") ? "report" : type;
        }

        sendWebhook(type, placeholders, reporter != null ? reporter : target);
    }

    public void sendXrayAlertWebhook(String player, String mineral, String world, int x, int y, int z, int rate,
            int windowRate, int windowTotal, int windowSeconds) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player != null ? player : "Unknown");
        placeholders.put("target", player != null ? player : "Unknown");
        placeholders.put("mineral", mineral != null ? mineral : "Unknown");
        placeholders.put("world", world != null ? world : "Unknown");
        placeholders.put("x", String.valueOf(x));
        placeholders.put("y", String.valueOf(y));
        placeholders.put("z", String.valueOf(z));
        placeholders.put("rate", String.valueOf(rate));
        placeholders.put("window_rate", String.valueOf(windowRate));
        placeholders.put("window_total", String.valueOf(windowTotal));
        placeholders.put("window_seconds", String.valueOf(windowSeconds));
        placeholders.put("timestamp", new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));

        sendWebhook("xray", placeholders, player);
    }

    private void sendWebhook(String type, Map<String, String> placeholders, String nameForImage) {
        if (!discordConfig.getConfig().getBoolean("enabled", false))
            return;
        if (!plugin.isEnabled())
            return;

        String webhookUrl = discordConfig.getConfig().getString("webhook-url");
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("URL_AQUI"))
            return;

        final String resolvedNameForImage = (nameForImage == null || nameForImage.isEmpty()) ? "MHF_Steve"
                : nameForImage;
        final Map<String, String> safePlaceholders = new HashMap<>(placeholders);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String path = "punishments." + type;
                if (!discordConfig.getConfig().isConfigurationSection(path)) {
                    path = "alerts." + type;
                }
                if (!discordConfig.getConfig().isConfigurationSection(path)) {
                    path = "reports." + type;
                }
                ConfigurationSection section = discordConfig.getConfig().getConfigurationSection(path);

                if (section == null || !section.getBoolean("enabled", true))
                    return;

                String serverName = discordConfig.getConfig().getString("server-name", "MaxStaff");
                safePlaceholders.putIfAbsent("server", serverName);
                safePlaceholders.put("timestamp", new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));

                String faceUrl = "https://mc-heads.net/avatar/" + resolvedNameForImage + "/100";
                safePlaceholders.putIfAbsent("face", faceUrl);

                String authorName = resolveText(section, "author", "MaxStaff Action", safePlaceholders, path + ".author");
                String authorIcon = resolveImage(section, "author-icon", faceUrl, safePlaceholders, faceUrl);
                String authorUrl = resolveText(section, "author-url", "", safePlaceholders, path + ".author-url");
                int color = parseHexColor(section.getString("color"), 0xFFFFFF, path + ".color");

                String defaultGlobalImage = sanitizeOrDefault(discordConfig.getConfig().getString("global-image"), "");
                String globalImage = resolveImage(section, "image", defaultGlobalImage, safePlaceholders, faceUrl);
                String thumbnail = resolveImage(section, "thumbnail", "", safePlaceholders, faceUrl);

                String content = resolveText(section, "content",
                        discordConfig.getConfig().getString("content", ""), safePlaceholders, path + ".content");
                String title = resolveText(section, "title", "", safePlaceholders, path + ".title");
                String description = resolveText(section, "description", "", safePlaceholders, path + ".description");
                String embedUrl = resolveText(section, "url", "", safePlaceholders, path + ".url");

                String defaultFooter = "{server} • {timestamp}";
                String footerText = resolveText(section, "footer",
                        discordConfig.getConfig().getString("footer", defaultFooter), safePlaceholders, path + ".footer");
                String footerIcon = resolveImage(section, "footer-icon",
                        discordConfig.getConfig().getString("footer-icon", ""), safePlaceholders, faceUrl);
                boolean timestampEnabled = section.getBoolean("timestamp",
                        discordConfig.getConfig().getBoolean("timestamp", false));

                String username = resolveText(section, "username",
                        discordConfig.getConfig().getString("username", ""), safePlaceholders, path + ".username");
                String avatarUrl = resolveImage(section, "avatar-url",
                        discordConfig.getConfig().getString("avatar-url", ""), safePlaceholders, faceUrl);

                StringBuilder json = new StringBuilder();
                json.append("{");
                List<String> rootProperties = new ArrayList<>();
                addStringProperty(rootProperties, "username", username);
                addStringProperty(rootProperties, "avatar_url", avatarUrl);
                addStringProperty(rootProperties, "content", content);

                StringBuilder embed = new StringBuilder();
                List<String> embedProperties = new ArrayList<>();

                List<String> authorProperties = new ArrayList<>();
                addStringProperty(authorProperties, "name", authorName);
                addStringProperty(authorProperties, "icon_url", authorIcon);
                addStringProperty(authorProperties, "url", authorUrl);
                addObjectProperty(embedProperties, "author", authorProperties);

                embedProperties.add("\"color\": " + color);
                addStringProperty(embedProperties, "title", title);
                addStringProperty(embedProperties, "description", description);
                addStringProperty(embedProperties, "url", embedUrl);
                addImageObjectProperty(embedProperties, "thumbnail", thumbnail);
                addImageObjectProperty(embedProperties, "image", globalImage);

                List<Map<?, ?>> fields = section.getMapList("fields");
                List<String> jsonFields = new ArrayList<>();
                if (!fields.isEmpty()) {
                    for (int i = 0; i < fields.size(); i++) {
                        Map<?, ?> field = fields.get(i);
                        String fieldPath = path + ".fields[" + i + "]";
                        try {
                            Object rawName = field.get("name");
                            Object rawValue = field.get("value");

                            if (!(rawName instanceof String) || ((String) rawName).trim().isEmpty()) {
                                plugin.getLogger()
                                        .warning("Field omitted due to invalid name in " + fieldPath + ".name");
                                continue;
                            }

                            if (!(rawValue instanceof String) || ((String) rawValue).trim().isEmpty()) {
                                plugin.getLogger()
                                        .warning("Field omitted due to invalid value in " + fieldPath + ".value");
                                continue;
                            }

                            String name = replacePlaceholders((String) rawName, safePlaceholders);
                            String value = replacePlaceholders((String) rawValue, safePlaceholders);

                            if (name.trim().isEmpty()) {
                                plugin.getLogger().warning(
                                        "Field omitted due to empty name after placeholders in " + fieldPath + ".name");
                                continue;
                            }

                            if (value.trim().isEmpty()) {
                                plugin.getLogger().warning("Field omitted due to empty value after placeholders in "
                                        + fieldPath + ".value");
                                continue;
                            }

                            boolean inline = Boolean.TRUE.equals(field.get("inline"));

                            List<String> fieldProperties = new ArrayList<>();
                            addStringProperty(fieldProperties, "name", name);
                            addStringProperty(fieldProperties, "value", value);
                            fieldProperties.add("\"inline\": " + inline);
                            jsonFields.add("{" + String.join(",", fieldProperties) + "}");
                        } catch (Exception fieldException) {
                            plugin.getLogger().warning(
                                    "Error procesando field en " + fieldPath + ": " + fieldException.getMessage());
                        }
                    }

                    if (!jsonFields.isEmpty()) {
                        embedProperties.add("\"fields\": [" + String.join(",", jsonFields) + "]");
                    }
                }

                List<String> footerProperties = new ArrayList<>();
                addStringProperty(footerProperties, "text", footerText);
                addStringProperty(footerProperties, "icon_url", footerIcon);
                addObjectProperty(embedProperties, "footer", footerProperties);

                if (timestampEnabled) {
                    embedProperties.add("\"timestamp\": \"" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date())
                            + "\"");
                }

                embed.append("{").append(String.join(",", embedProperties)).append("}");
                rootProperties.add("\"embeds\": [" + embed + "]");
                json.append(String.join(",", rootProperties));
                json.append("}");

                URL url = URI.create(webhookUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.addRequestProperty("Content-Type", "application/json");
                connection.addRequestProperty("User-Agent", "MaxStaff-Plugin");
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                connection.getResponseCode();
                connection.disconnect();

            } catch (Exception e) {
                plugin.getLogger().warning("Error sending Discord Webhook: " + e.getMessage());
            }
        });
    }

    private String resolveText(ConfigurationSection section, String key, String defaultValue, Map<String, String> placeholders,
            String configPath) {
        String rawValue = sanitizeOrDefault(section.getString(key), defaultValue);
        String resolvedValue = replacePlaceholders(rawValue, placeholders);
        if (resolvedValue.length() > 4096) {
            plugin.getLogger().warning("Discord text in " + configPath + " is too long. Truncating to 4096 characters.");
            return resolvedValue.substring(0, 4096);
        }
        return resolvedValue;
    }

    private String resolveImage(ConfigurationSection section, String key, String defaultValue, Map<String, String> placeholders,
            String faceUrl) {
        String rawValue = sanitizeOrDefault(section.getString(key), defaultValue);
        return replacePlaceholders(rawValue, placeholders).replace("{face}", faceUrl);
    }

    private void addStringProperty(List<String> properties, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        properties.add("\"" + key + "\": \"" + escape(value) + "\"");
    }

    private void addObjectProperty(List<String> properties, String key, List<String> objectProperties) {
        if (objectProperties == null || objectProperties.isEmpty()) {
            return;
        }
        properties.add("\"" + key + "\": {" + String.join(",", objectProperties) + "}");
    }

    private void addImageObjectProperty(List<String> properties, String key, String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        properties.add("\"" + key + "\": { \"url\": \"" + escape(url) + "\" }");
    }

    private String replacePlaceholders(String text, Map<String, String> placeholders) {
        if (text == null)
            return "";
        String replaced = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            replaced = replaced.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return replaced;
    }

    private String escape(String text) {
        if (text == null)
            return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private String sanitizeOrDefault(String text, String defaultValue) {
        if (text == null)
            return defaultValue;
        String sanitized = text.trim();
        return sanitized.isEmpty() ? defaultValue : sanitized;
    }

    private int parseHexColor(String colorValue, int fallbackColor, String configPath) {
        String sanitizedColor = sanitizeOrDefault(colorValue, "").replace("#", "").trim();
        if (sanitizedColor.isEmpty()) {
            return fallbackColor;
        }

        try {
            return Integer.parseInt(sanitizedColor, 16);
        } catch (NumberFormatException exception) {
            plugin.getLogger().warning("Invalid hexadecimal color in " + configPath + ": " + colorValue
                    + ". Wearing fallback " + String.format("0x%06X", fallbackColor));
            return fallbackColor;
        }
    }

    public void reload() {
        discordConfig.reloadConfig();
    }
}