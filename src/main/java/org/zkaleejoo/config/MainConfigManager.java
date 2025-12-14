package org.zkaleejoo.config;

import org.zkaleejoo.MaxStaff;


public class MainConfigManager {
    
    private CustomConfig configFile;

    public MainConfigManager(MaxStaff plugin){
        configFile = new CustomConfig("config.yml", null, plugin, false);
        configFile.registerConfig();
        loadConfig();
    }


    public void loadConfig(){
   
    }

    public void reloadConfig(){
        configFile.reloadConfig();
        loadConfig();
    }
    
}
