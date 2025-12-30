package org.zkaleejoo;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.zkaleejoo.commands.MainCommand;
import org.zkaleejoo.config.MainConfigManager;

import org.zkaleejoo.managers.StaffManager;
import org.zkaleejoo.utils.UpdateChecker;
import org.zkaleejoo.listeners.PlayerJoinListener;
import org.zkaleejoo.listeners.StaffItemsListener;

import org.zkaleejoo.listeners.PlayerQuitListener;

import org.zkaleejoo.managers.GuiManager; 
import org.zkaleejoo.listeners.GuiListener; 

import org.zkaleejoo.managers.FreezeManager;
import org.zkaleejoo.listeners.FreezeListener;

import org.zkaleejoo.managers.PunishmentManager;
import org.zkaleejoo.commands.PunishmentCommand;
import org.zkaleejoo.listeners.ChatListener;
import org.bukkit.plugin.java.JavaPlugin;

public class MaxStaff extends JavaPlugin {

    private MainConfigManager mainConfigManager;
    private StaffManager staffManager;
    private GuiManager guiManager;
    private FreezeManager freezeManager;
    private PunishmentManager punishmentManager;
    private String latestVersion;

    //PLUGIN SE PRENDE
    @Override
    public void onEnable() {
        int pluginId = 28604;
        Metrics metrics = new Metrics(this, pluginId);
        
        mainConfigManager = new MainConfigManager(this);
        freezeManager = new FreezeManager(this);
        staffManager = new StaffManager(this);
        guiManager = new GuiManager(this);
        punishmentManager = new PunishmentManager(this);

        registerCommands();
        registerEvents();
        checkUpdates();

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

    private void checkUpdates() {
    if (!getMainConfigManager().isUpdateCheckEnabled()) return;
    new UpdateChecker(this, 130851).getVersion(version -> {
        if (this.getDescription().getVersion().equalsIgnoreCase(version)) {
            getLogger().info("You are using the latest version!");
        } else {
            this.latestVersion = version;
            getLogger().warning("A new version is available: " + version);
            getLogger().warning("Download it at: https://www.spigotmc.org/resources/130851/");
        }
        });
    }

    public String getLatestVersion() {
    return latestVersion;
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

        PunishmentCommand punCmd = new PunishmentCommand(this);
        
        String[] punCommands = {"ban", "tempban", "mute", "tempmute", "kick", "unban", "unmute", "warn"};
        for (String cmd : punCommands) {
            if (getCommand(cmd) != null) {
                getCommand(cmd).setExecutor(punCmd);
                getCommand(cmd).setTabCompleter(punCmd);
            }
        }
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

    public PunishmentManager getPunishmentManager() { return punishmentManager; }

    public void registerEvents() {
        getServer().getPluginManager().registerEvents(new StaffItemsListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new FreezeListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }
}