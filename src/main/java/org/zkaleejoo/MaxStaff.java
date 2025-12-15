package org.zkaleejoo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.zkaleejoo.commands.MainCommand;
import org.zkaleejoo.config.MainConfigManager;

import org.zkaleejoo.managers.StaffManager;
import org.zkaleejoo.listeners.PlayerJoinListener;
import org.zkaleejoo.listeners.StaffItemsListener;

import org.zkaleejoo.listeners.PlayerQuitListener;

import org.zkaleejoo.managers.GuiManager; 
import org.zkaleejoo.listeners.GuiListener; 

import org.zkaleejoo.managers.FreezeManager;
import org.zkaleejoo.listeners.FreezeListener;

public class MaxStaff extends JavaPlugin {

    private MainConfigManager mainConfigManager;
    private StaffManager staffManager;
    private GuiManager guiManager;
    private FreezeManager freezeManager;

    //PLUGIN SE PRENDE
    @Override
    public void onEnable() {
        mainConfigManager = new MainConfigManager(this);
        freezeManager = new FreezeManager(this);
        staffManager = new StaffManager(this);
        guiManager = new GuiManager(this);

        registerCommands();
        registerEvents();

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

        if (staffManager != null) {
            staffManager.disableAllStaff();
        }

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

    public StaffManager getStaffManager() {
        return staffManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public FreezeManager getFreezeManager() {
        return freezeManager;
    }

    public void registerEvents() {
        getServer().getPluginManager().registerEvents(new StaffItemsListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new FreezeListener(this), this);
    }
}