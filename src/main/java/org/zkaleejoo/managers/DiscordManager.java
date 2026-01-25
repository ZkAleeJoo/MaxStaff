package org.zkaleejoo.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.CustomConfig;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

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
        if (webhookUrl == null || webhookUrl.isEmpty() || !webhookUrl.contains("discord.com/api/webhooks")) {
            return;
        }
        if (webhookUrl.equals("https://discord.com/api/webhooks/ID/TOKEN")) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String path = "punishments." + type;
                String title = discordConfig.getConfig().getString(path + ".title", "Staff Action");
                String colorStr = discordConfig.getConfig().getString(path + ".color", "#ffffff").replace("#", "");
                int color = Integer.parseInt(colorStr, 16);
                
                String date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
                
                String format = discordConfig.getConfig().getString(path + ".format");
                if (format == null) return;

                String description = format
                        .replace("{target}", ChatColor.stripColor(target))
                        .replace("{staff}", ChatColor.stripColor(staff))
                        .replace("{reason}", ChatColor.stripColor(reason))
                        .replace("{duration}", duration != null ? duration : "N/A")
                        .replace("{count}", count != null ? count : "0")
                        .replace("{date}", date);

                description = description.replace("\"", "\\\"").replace("\n", "\\n");

                String jsonPayload = "{"
                        + "\"embeds\": [{"
                        + "\"title\": \"" + title + "\","
                        + "\"description\": \"" + description + "\","
                        + "\"color\": " + color + ","
                        + "\"footer\": {\"text\": \"MaxStaff Logger • " + date + "\"}"
                        + "}]"
                        + "}";

                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.addRequestProperty("Content-Type", "application/json");
                connection.addRequestProperty("User-Agent", "MaxStaff-Plugin");
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                }

                connection.getResponseCode(); 
                connection.disconnect();

            } catch (Exception e) {
                plugin.getLogger().warning("Error processing Webhook (Check your discord.yml): " + e.getMessage());
            }
        });
    }

    public void reload() {
        discordConfig.reloadConfig();
    }
}