package org.zkaleejoo.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.CustomConfig;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
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
        if (!discordConfig.getConfig().getBoolean("enabled", false)) return;

        String webhookUrl = discordConfig.getConfig().getString("webhook-url");
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("URL_AQUI")) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String path = "punishments." + type;
                if (!discordConfig.getConfig().isConfigurationSection(path)) {
                    path = "alerts." + type;
                }
                ConfigurationSection section = discordConfig.getConfig().getConfigurationSection(path);
                
                if (section == null) return;

                String authorName = section.getString("author", "MaxStaff Action");
                String colorStr = section.getString("color", "#ffffff").replace("#", "");
                int color = Integer.parseInt(colorStr, 16);
                String globalImage = discordConfig.getConfig().getString("global-image", "");
                String thumbnail = section.getString("thumbnail", "");
                
                String finalTarget = (target != null) ? target : "N/A";
                String faceUrl = "https://minotar.net/avatar/" + finalTarget + "/100.png";
                
                if (thumbnail.contains("{face}")) thumbnail = faceUrl;

                StringBuilder json = new StringBuilder();
                json.append("{");
                json.append("\"embeds\": [{");
                
                json.append("\"author\": {");
                json.append("\"name\": \"").append(escape(authorName)).append("\",");
                json.append("\"icon_url\": \"").append(faceUrl).append("\"");
                json.append("},");

                json.append("\"color\": ").append(color).append(",");

                if (!thumbnail.isEmpty()) {
                    json.append("\"thumbnail\": { \"url\": \"").append(thumbnail).append("\" },");
                }

                if (!globalImage.isEmpty()) {
                    json.append("\"image\": { \"url\": \"").append(globalImage).append("\" },");
                }

                List<Map<?, ?>> fields = section.getMapList("fields");
                if (!fields.isEmpty()) {
                    json.append("\"fields\": [");
                    for (int i = 0; i < fields.size(); i++) {
                        Map<?, ?> field = fields.get(i);
                        String name = escape(replacePlaceholders(field.get("name").toString(), target, staff, reason, duration, count));
                        String value = escape(replacePlaceholders(field.get("value").toString(), target, staff, reason, duration, count));
                        boolean inline = (boolean) field.get("inline");

                        json.append("{");
                        json.append("\"name\": \"").append(name).append("\",");
                        json.append("\"value\": \"").append(value).append("\",");
                        json.append("\"inline\": ").append(inline);
                        json.append("}");

                        if (i < fields.size() - 1) json.append(",");
                    }
                    json.append("],");
                }

                String date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
                String serverName = discordConfig.getConfig().getString("server-name", "MaxStaff");
                
                json.append("\"footer\": { \"text\": \"").append(serverName).append(" • ").append(date).append("\" }");
                
                json.append("}]"); 
                json.append("}");  

                URL url = new URL(webhookUrl);
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
                plugin.getLogger().warning("Error enviando Webhook de Discord: " + e.getMessage());
            }
        });
    }

    private String replacePlaceholders(String text, String target, String staff, String reason, String duration, String count) {
        if (text == null) return "";
        return text
                .replace("{target}", target != null ? target : "N/A")
                .replace("{staff}", staff != null ? staff : "Console")
                .replace("{reason}", reason != null ? reason : "N/A")
                .replace("{duration}", duration != null ? duration : "N/A")
                .replace("{count}", count != null ? count : "0");
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "");
    }

    public void reload() {
        discordConfig.reloadConfig();
    }
}