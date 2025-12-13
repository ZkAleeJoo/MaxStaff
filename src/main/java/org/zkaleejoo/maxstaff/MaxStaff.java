package org.zkaleejoo.maxstaff;

import org.bukkit.plugin.java.JavaPlugin;

public class MaxStaff extends JavaPlugin {

    private static MaxStaff instance;

    @Override
    public void onEnable() {
        instance = this;
        
        // Mensaje en consola con colores para confirmar que cargó
        getLogger().info("--------------------------------");
        getLogger().info(" MaxStaff se ha habilitado correctamente.");
        getLogger().info(" Version: " + getDescription().getVersion());
        getLogger().info("--------------------------------");
        
        // Aquí registraremos comandos y eventos más tarde
    }

    @Override
    public void onDisable() {
        getLogger().info("MaxStaff se ha deshabilitado.");
    }

    public static MaxStaff getInstance() {
        return instance;
    }
}