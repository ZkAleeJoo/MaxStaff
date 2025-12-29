package org.zkaleejoo;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.zkaleejoo.commands.MainCommand;
import org.zkaleejoo.commands.PunishmentCommand;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.listeners.*;
import org.zkaleejoo.managers.*;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.ServerVersion;
import org.zkaleejoo.utils.UpdateChecker;

public class MaxStaff extends JavaPlugin {

    public static String prefix = "&8[&9MaxStaff&8] ";
    private String version = getDescription().getVersion();
    private String latestVersion;
    private boolean isPaper;

    private MainConfigManager mainConfigManager;
    private StaffManager staffManager;
    private PunishmentManager punishmentManager;
    private FreezeManager freezeManager;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        ServerVersion.getVersion(); 
        checkServerType();

        this.mainConfigManager = new MainConfigManager(this);
        this.staffManager = new StaffManager(this);
        this.punishmentManager = new PunishmentManager(this);
        this.freezeManager = new FreezeManager(this);
        this.guiManager = new GuiManager(this);

        registerCommands();
        registerEvents();

        checkUpdates();

        sendConsoleHeader();
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&cPlugin successfully deactivated."));
    }

    private void checkServerType() {
        try {
            Class.forName("io.papermc.paper.event.packet.PlayerChunkLoadEvent");
            this.isPaper = true;
        } catch (ClassNotFoundException e) {
            this.isPaper = false;
        }
    }

    public void registerCommands() {
        MainCommand mainCommand = new MainCommand(this);
        getCommand("maxstaff").setExecutor(mainCommand);
        getCommand("maxstaff").setTabCompleter(mainCommand);

        PunishmentCommand punishmentCommand = new PunishmentCommand(this);
        getCommand("punish").setExecutor(punishmentCommand);
    }

    public void registerEvents() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new FreezeListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new StaffItemsListener(this), this);
    }

    private void sendConsoleHeader() {
        String serverType = isPaper ? "&bPaper" : "&eSpigot/Bukkit";
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("   _____                   _________ __          _____  _____ \r\n" + //
                        "  /     \\ _____  ___  ___ /   _____//  |______ _/ ____\\/ ____\\\r\n" + //
                        " /  \\ /  \\\\__  \\ \\  \\/  / \\_____  \\\\   __\\__  \\\\   __\\\\   __\\ \r\n" + //
                        "/    Y    \\/ __ \\_>    <  /        \\|  |  / __ \\|  |   |  |   \r\n" + //
                        "\\____|__  (____  /__/\\_ \\/_______  /|__| (____  /__|   |__|   \r\n" + //
                        "        \\/     \\/      \\/        \\/           \\/              "));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&7Version: &f" + version));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&7Server: " + serverType));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&7Detected: &a" + ServerVersion.getVersion().name()));
    }

    public MainConfigManager getMainConfigManager() { return mainConfigManager; }
    public StaffManager getStaffManager() { return staffManager; }
    public PunishmentManager getPunishmentManager() { return punishmentManager; }
    public FreezeManager getFreezeManager() { return freezeManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public boolean isPaper() { return isPaper; }
    public String getLatestVersion() { return latestVersion; }

    private void checkUpdates() {
        if (!mainConfigManager.isUpdateCheckEnabled()) return;
        new UpdateChecker(this, 122239).getVersion(version -> {
            if (!this.getDescription().getVersion().equalsIgnoreCase(version)) {
                this.latestVersion = version;
                getLogger().warning("New version available: " + version);
            }
        });
    }
}