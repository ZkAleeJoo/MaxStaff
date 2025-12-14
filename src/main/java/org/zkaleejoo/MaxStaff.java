package org.zkaleejoo;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.zkaleejoo.config.MainConfigManager;
import org.bukkit.ChatColor;

public class MaxStaff extends JavaPlugin {

    public static String prefix = "&4&lMaxStaff &8» ";
    private MainConfigManager mainConfigManager;


    //PLUGIN INICIA
    @Override
    public void onEnable() {

        Bukkit.getConsoleSender().sendMessage("   _____                   _________ __          _____  _____ \r\n" + //
                        "  /     \\ _____  ___  ___ /   _____//  |______ _/ ____\\/ ____\\\r\n" + //
                        " /  \\ /  \\\\__  \\ \\  \\/  / \\_____  \\\\   __\\__  \\\\   __\\\\   __\\ \r\n" + //
                        "/    Y    \\/ __ \\_>    <  /        \\|  |  / __ \\|  |   |  |   \r\n" + //
                        "\\____|__  (____  /__/\\_ \\/_______  /|__| (____  /__|   |__|   \r\n" + //
                        "        \\/     \\/      \\/        \\/           \\/              ");   

        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', prefix + "&fIt was activated correctly"));
    }



    //PLUGIN SE APAGA
    @Override
    public void onDisable() {
            Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', prefix + "&fIt was successfully deactivated"));
    }

    public MainConfigManager getMainConfigManager() {
        return mainConfigManager;
    }


}