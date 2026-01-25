package org.zkaleejoo.utils;

import org.bukkit.Bukkit;
import org.zkaleejoo.MaxStaff;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {

    private final MaxStaff plugin;
    private final String slug = "maxstaff"; 

    public UpdateChecker(MaxStaff plugin) {
        this.plugin = plugin;
    }

    public void getVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                URL url = new URL("https://api.modrinth.com/v2/project/" + slug + "/version");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "MaxStaff/UpdateChecker/" + plugin.getDescription().getVersion());
                
                try (Scanner scanner = new Scanner(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    while (scanner.hasNextLine()) {
                        response.append(scanner.nextLine());
                    }
                    
                    String json = response.toString();
                    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"version_number\":\"([^\"]+)\"").matcher(json);
                    
                    if (matcher.find()) {
                        consumer.accept(matcher.group(1)); 
                    }
                }
            } catch (Exception exception) {
                plugin.getLogger().info("No new updates were found" + exception.getMessage());
            }
        });
    }
}