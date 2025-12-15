package org.zkaleejoo.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.zkaleejoo.MaxStaff;

public class MainConfigManager {
    
    private CustomConfig configFile;
    private MaxStaff plugin;

    private String prefix;
    private String noPermission;
    private String pluginReload;
    private String subcommandInvalid;
    private String subcommandSpecified;

    private String staffModeEnabled;
    private String staffModeDisabled;
    private String inventorySaved;
    private String inventoryRestored;
    private String cannotDrop;  
    private String cannotPlace; 
    
    private String msgInspect;
    private String msgVanishOn;
    private String msgVanishOff;
    private String msgPunish;
    private String msgPlayers;

    public MainConfigManager(MaxStaff plugin){
        this.plugin = plugin;
        configFile = new CustomConfig("config.yml", null, plugin, false);
        configFile.registerConfig();
        loadConfig();
    }

    public void loadConfig(){
        FileConfiguration config = configFile.getConfig();
        
        prefix = config.getString("general.prefix");
        noPermission = config.getString("messages.no-permission");
        pluginReload = config.getString("messages.plugin-reload");
        subcommandInvalid = config.getString("messages.subcommand-invalid");
        subcommandSpecified = config.getString("messages.subcommand-specified");

        staffModeEnabled = config.getString("staff-mode.enabled");
        staffModeDisabled = config.getString("staff-mode.disabled");
        inventorySaved = config.getString("staff-mode.inventory-saved");
        inventoryRestored = config.getString("staff-mode.inventory-restored");
        cannotDrop = config.getString("staff-mode.cannot-drop");
        cannotPlace = config.getString("staff-mode.cannot-place");

        msgInspect = config.getString("staff-mode.items.inspect.message");
        msgVanishOn = config.getString("staff-mode.items.vanish.message-on");
        msgVanishOff = config.getString("staff-mode.items.vanish.message-off");
        msgPunish = config.getString("staff-mode.items.punish.message");
        msgPlayers = config.getString("staff-mode.items.players.message");
    }

    public void reloadConfig(){
        configFile.reloadConfig();
        loadConfig();
    }
    
    public String getPrefix() { return prefix; }
    public String getNoPermission() { return noPermission; }
    public String getPluginReload() { return pluginReload; }
    public String getSubcommandInvalid() { return subcommandInvalid; }
    public String getSubcommandSpecified() { return subcommandSpecified; }

    public String getStaffModeEnabled() { return staffModeEnabled; }
    public String getStaffModeDisabled() { return staffModeDisabled; }
    public String getInventorySaved() { return inventorySaved; }
    public String getInventoryRestored() { return inventoryRestored; }
    public String getCannotDrop() { return cannotDrop; }
    public String getCannotPlace() { return cannotPlace; }
    
    public String getMsgInspect() { return msgInspect; }
    public String getMsgVanishOn() { return msgVanishOn; }
    public String getMsgVanishOff() { return msgVanishOff; }
    public String getMsgPunish() { return msgPunish; }
    public String getMsgPlayers() { return msgPlayers; }
}