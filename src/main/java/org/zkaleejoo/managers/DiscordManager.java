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
        if (!discordConfig.getConfig().getBoolean("enabled")) return;

        String webhookUrl = discordConfig.getConfig().getString("webhook-url");
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("URL")) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String title = discordConfig.getConfig().getString("punishments." + type + ".title");
                String colorStr = discordConfig.getConfig().getString("punishments." + type + ".color", "#ffffff").replace("#", "");
                int color = Integer.parseInt(colorStr, 16);
                
                String date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
                
                String cleanTarget = ChatColor.stripColor(target);
                String cleanStaff = ChatColor.stripColor(staff);
                String cleanReason = ChatColor.stripColor(reason);

                String description = discordConfig.getConfig().getString("punishments." + type + ".format")
                        .replace("{target}", cleanTarget)
                        .replace("{staff}", cleanStaff)
                        .replace("{reason}", cleanReason)
                        .replace("{duration}", duration != null ? duration : "N/A")
                        .replace("{count}", count != null ? count : "0")
                        .replace("{date}", date);

                description = description.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");

                String jsonPayload = "{"
                        + "\"embeds\": [{"
                        + "\"title\": \"" + title + "\","
                        + "\"description\": \"" + description + "\","
                        + "\"color\": " + color + ","
                        + "\"footer\": {\"text\": \"MaxStaff Logging System\"}"
                        + "}]"
                        + "}";

                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.addRequestProperty("Content-Type", "application/json");
                connection.addRequestProperty("User-Agent", "Java-MaxStaff");
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    os.flush();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode >= 400) {
                    plugin.getLogger().warning("Discord Webhook error! Code: " + responseCode);
                }

                connection.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Could not send Discord Webhook: " + e.getMessage());
            }
        });
    }

    public void reload() {
        discordConfig.reloadConfig();
    }
}