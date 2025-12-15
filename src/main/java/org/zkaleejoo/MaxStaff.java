package org.zkaleejoo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.zkaleejoo.commands.MainCommand;
import org.zkaleejoo.config.MainConfigManager;

public class MaxStaff extends JavaPlugin {

    private MainConfigManager mainConfigManager;

    //PLUGIN SE PRENDE
    @Override
    public void onEnable() {
        mainConfigManager = new MainConfigManager(this);

        registerCommands();

        Bukkit.getConsoleSender().sendMessage("   _____                   _________ __          _____  _____ \r\n" + 
                        "  /     \\ _____  ___  ___ /   _____//  |______ _/ ____\\/ ____\\\r\n" + 
                        " /  \\ /  \\\\__  \\ \\  \\/  / \\_____  \\\\   __\\__  \\\\   __\\\\   __\\ \r\n" + 
                        "/    Y    \\/ __ \\_>    <  /        \\|  |  / __ \\|  |   |  |   \r\n" + 
                        "\\____|__  (____  /__/\\_ \\/_______  /|__| (____  /__|   |__|   \r\n" + 
                        "        \\/     \\/      \\/        \\/           \\/              ");   

        String prefix = mainConfigManager.getPrefix();
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', prefix + "&fIt was activated correctly"));
    }

    // PLUGIN SE APAGA
    @Override
    public void onDisable() {
        String prefix = (mainConfigManager != null) ? mainConfigManager.getPrefix() : "&4MaxStaff ";
        Bukkit.getConsoleSender().sendMessage(
            ChatColor.translateAlternateColorCodes('&', prefix + "&fIt was successfully deactivated"));
    }

    public void registerCommands() {
        MainCommand mainCommand = new MainCommand(this);
        this.getCommand("maxstaff").setExecutor(mainCommand);
        this.getCommand("maxstaff").setTabCompleter(mainCommand);
    }

    public MainConfigManager getMainConfigManager() {
        return mainConfigManager;
    }
}