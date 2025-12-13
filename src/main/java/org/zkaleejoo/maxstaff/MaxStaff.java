package org.zkaleejoo.maxstaff;

import org.bukkit.plugin.java.JavaPlugin;
import org.zkaleejoo.maxstaff.commands.StaffCommand;
import org.zkaleejoo.maxstaff.managers.StaffModeManager;

public class MaxStaff extends JavaPlugin {

    private static MaxStaff instance;
    private StaffModeManager staffModeManager;

    @Override
    public void onEnable() {
        instance = this;
        
        this.staffModeManager = new StaffModeManager(this);


        getCommand("staff").setExecutor(new StaffCommand(staffModeManager));

        getLogger().info("MaxStaff habilitado correctamente (Modo Profesional).");
    }

    @Override
    public void onDisable() {
        getLogger().info("MaxStaff deshabilitado.");
    }

    public static MaxStaff getInstance() {
        return instance;
    }
    
    public StaffModeManager getStaffModeManager() {
        return staffModeManager;
    }
}