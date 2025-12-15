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
}