package org.zkaleejoo;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.zkaleejoo.commands.AltsCommand;
import org.zkaleejoo.commands.ChatCommand;
import org.zkaleejoo.commands.CommandSpyCommand;
import org.zkaleejoo.commands.GameModeCommand;
import org.zkaleejoo.commands.MainCommand;
import org.zkaleejoo.config.MainConfigManager;

import org.zkaleejoo.managers.StaffManager;
import org.zkaleejoo.utils.UpdateChecker;
import org.zkaleejoo.listeners.PlayerJoinListener;
import org.zkaleejoo.listeners.StaffItemsListener;

import org.zkaleejoo.listeners.PlayerQuitListener;

import org.zkaleejoo.managers.GuiManager; 
import org.zkaleejoo.listeners.GuiListener;
import org.zkaleejoo.managers.ChatManager;
import org.zkaleejoo.managers.FreezeManager;
import org.zkaleejoo.listeners.FreezeListener;

import org.zkaleejoo.managers.PunishmentManager;
import org.zkaleejoo.commands.PunishmentCommand;
import org.zkaleejoo.commands.StaffChatCommand;
import org.zkaleejoo.commands.VanishCommand;
import org.zkaleejoo.listeners.ChatListener;
import org.zkaleejoo.listeners.CommandSpyListener;
import org.zkaleejoo.utils.MessageUtils;

public class MaxStaff extends JavaPlugin {

    private MainConfigManager mainConfigManager;
    private StaffManager staffManager;
    private GuiManager guiManager;
    private FreezeManager freezeManager;
    private PunishmentManager punishmentManager;
    private String latestVersion;
    private ChatManager chatManager;
    

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
        chatManager = new ChatManager(this);

        registerCommands();
        registerEvents();
        checkUpdates();

        String prefix = mainConfigManager.getPrefix();
        
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED+"   _____                   _________ __          _____  _____ ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED+"  /     \\ _____  ___  ___ /   _____//  |______ _/ ____\\/ ____\\");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED+" /  \\ /  \\\\__  \\ \\  \\/  / \\_____  \\\\   __\\__  \\\\   __\\\\   __\\ ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED+"/    Y    \\/ __ \\_>    <  /        \\|  |  / __ \\|  |   |  |   ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED+"\\____|__  (____  /__/\\_ \\/_______  /|__| (____  /__|   |__|   ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED+"        \\/     \\/      \\/        \\/           \\/              ");

        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&4It was activated correctly")); 

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new org.zkaleejoo.placeholders.MaxStaffExpansion(this).register();
            getLogger().info("PlaceholderAPI Hook successfully registered!");
        } else {
            getLogger().warning("PlaceholderAPI not found, placeholders will not work.");
        }
        
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
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&4It was successfully deactivated"));
    }

    public void registerCommands() {
        MainCommand mainCommand = new MainCommand(this);
        this.getCommand("maxstaff").setExecutor(mainCommand);
        this.getCommand("maxstaff").setTabCompleter(mainCommand);
        this.getCommand("vanish").setExecutor(new VanishCommand(this));
        this.getCommand("sc").setExecutor(new StaffChatCommand(this));
        this.getCommand("cmdspy").setExecutor(new CommandSpyCommand(this));
        this.getCommand("chat").setExecutor(new ChatCommand(this));
        this.getCommand("chat").setTabCompleter(new ChatCommand(this));
        GameModeCommand gmCmd = new GameModeCommand(this);
        this.getCommand("gamemode").setExecutor(gmCmd);
        this.getCommand("gamemode").setTabCompleter(gmCmd);
        AltsCommand altsCmd = new AltsCommand(this);
        getCommand("alts").setExecutor(altsCmd);
        

        PunishmentCommand punCmd = new PunishmentCommand(this);
        
        String[] punCommands = {
            "ban", "tempban", "mute", "tempmute", "kick", 
            "unban", "unmute", "warn", "history",
            "ban-ip", "tempban-ip", "unban-ip"
        };
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

    public ChatManager getChatManager() {
        return chatManager;
    }

    public PunishmentManager getPunishmentManager() { return punishmentManager; }

    public void registerEvents() {
        getServer().getPluginManager().registerEvents(new StaffItemsListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new FreezeListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandSpyListener(this), this);
    }
}