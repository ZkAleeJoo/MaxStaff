package org.zkaleejoo;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bstats.bukkit.Metrics;
import org.zkaleejoo.commands.registration.MaxStaffCommandRegistrar;
import org.zkaleejoo.config.MainConfigManager;

import org.zkaleejoo.managers.StaffManager;
import org.zkaleejoo.utils.UpdateChecker;
import org.zkaleejoo.listeners.PlayerJoinListener;
import org.zkaleejoo.listeners.StaffItemsListener;
import org.zkaleejoo.listeners.StaffModeListener;
import org.zkaleejoo.listeners.PlayerQuitListener;

import org.zkaleejoo.managers.GuiManager;
import org.zkaleejoo.managers.IPunishmentManager;
import org.zkaleejoo.listeners.GuiListener;
import org.zkaleejoo.managers.ChatManager;
import org.zkaleejoo.managers.DiscordManager;
import org.zkaleejoo.managers.FreezeManager;
import org.zkaleejoo.listeners.FreezeListener;
import org.zkaleejoo.listeners.GlobalPunishmentListener;

import org.zkaleejoo.managers.PunishmentManager;
import org.zkaleejoo.managers.PunishmentManagerMysql;
import org.zkaleejoo.listeners.ChatListener;
import org.zkaleejoo.listeners.CommandSpyListener;
import org.zkaleejoo.listeners.CommandBlockListener;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.managers.ReportManager;
import java.util.List;
import org.zkaleejoo.managers.InventorySnapshotManager;
import org.zkaleejoo.listeners.PlayerDeathListener;
import org.zkaleejoo.managers.ClientTrackerManager;
import org.bukkit.event.HandlerList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.zkaleejoo.config.CustomConfig;
import org.zkaleejoo.listeners.AntiXrayListener;
import org.zkaleejoo.listeners.VanishProtectionListener;

public class MaxStaff extends JavaPlugin {

    private static final int BSTATS_PLUGIN_ID = 31109;
    private static final long UPDATE_CHECK_INTERVAL_TICKS = 20L * 60L * 60L * 5L;

    private MainConfigManager mainConfigManager;
    private MaxStaffCommandRegistrar commandRegistrar;
    private StaffManager staffManager;
    private GuiManager guiManager;
    private FreezeManager freezeManager;
    private IPunishmentManager punishmentManager;
    private String latestVersion;
    private ChatManager chatManager;
    private DiscordManager discordManager;
    private ReportManager reportManager;
    private InventorySnapshotManager inventorySnapshotManager;
    private ClientTrackerManager clientTrackerManager;
    private AntiXrayListener antiXrayListener;
    private Metrics metrics;
    private BukkitTask updateCheckTask;
    private final Set<UUID> staffChatToggledPlayers = ConcurrentHashMap.newKeySet();

    // PLUGIN ENCIENDE
    @Override
    public void onEnable() {
        CustomConfig initialConfig = new CustomConfig("config.yml", null, this, false);
        initialConfig.registerConfig();

        mainConfigManager = new MainConfigManager(this);
        syncMetricsState();
        commandRegistrar = new MaxStaffCommandRegistrar(this);
        freezeManager = new FreezeManager(this);
        freezeManager.cleanupOrphanDisplays();
        staffManager = new StaffManager(this);
        guiManager = new GuiManager(this);

        chatManager = new ChatManager(this);
        this.discordManager = new DiscordManager(this);
        reportManager = new ReportManager(this);
        inventorySnapshotManager = new InventorySnapshotManager(this);
        clientTrackerManager = new ClientTrackerManager(this);
        if (isModuleEnabled("client-tracker")) {
            clientTrackerManager.registerChannels();
        }
        int startupRemoved = inventorySnapshotManager.cleanupExpiredDeathSnapshots();
        if (startupRemoved > 0) {
            getLogger().info("Cleaned " + startupRemoved + " expired death snapshots on startup.");
        }

        long cleanupIntervalTicks = Math.max(1L, mainConfigManager.getInventorySnapshotCleanupIntervalMinutes()) * 20L
                * 60L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            int removed = inventorySnapshotManager.cleanupExpiredDeathSnapshots();
            if (removed > 0) {
                getLogger().fine("Cleaned " + removed + " expired death snapshots.");
            }
        }, cleanupIntervalTicks, cleanupIntervalTicks);

        if (mainConfigManager.isDbEnabled()) {
            try {
                punishmentManager = new PunishmentManagerMysql(this);
                Bukkit.getConsoleSender().sendMessage(
                        MessageUtils.getColoredMessage("&4&lMaxStaff &8» &aSanctions system loaded: MySQL"));
                sendMysqlStatusReport();
            } catch (Exception e) {
                getLogger().severe("Error connecting to MySQL, switching to local system: " + e.getMessage());
                sendMysqlErrorReport(e);
                punishmentManager = new PunishmentManager(this);
            }
        } else {
            punishmentManager = new PunishmentManager(this);
            Bukkit.getConsoleSender().sendMessage(
                    MessageUtils.getColoredMessage("&4&lMaxStaff &8» &aSanctions system loaded: Local (YAML)"));
            Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                    "&4&lMaxStaff &8» &aYAML punishment storage is single-server only. &eNetwork-wide &asynchronization requires &edatabase.enabled=true."));
        }

        registerCommands();
        registerEvents();
        startUpdateChecks();

        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&4&lMaxStaff &8» &4   _____                   _________ __          _____  _____ "));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&4&lMaxStaff &8» &4  /     \\ _____  ___  ___ /   _____//  |______ _/ ____\\/ ____\\"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&4&lMaxStaff &8» &4 /  \\ /  \\\\__  \\ \\  \\/  / \\_____  \\\\   __\\__  \\\\   __\\\\   __\\ "));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&4&lMaxStaff &8» &4/    Y    \\/ __ \\_>    <  /        \\|  |  / __ \\|  |   |  |   "));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&4&lMaxStaff &8» &4\\____|__  (____  /__/\\_ \\/_______  /|__| (____  /__|   |__|   "));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&4&lMaxStaff &8» &4        \\/     \\/      \\/        \\/           \\/              "));

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new org.zkaleejoo.placeholders.MaxStaffExpansion(this).register();
            Bukkit.getConsoleSender().sendMessage(
                    MessageUtils.getColoredMessage("&4&lMaxStaff &8» &aPlaceholderAPI Hook successfully registered!"));
        } else {
            Bukkit.getConsoleSender().sendMessage(MessageUtils
                    .getColoredMessage("&4&lMaxStaff &8» &4PlaceholderAPI not found, placeholders will not work."));
        }

    }

    private void sendMysqlStatusReport() {
        Bukkit.getConsoleSender()
                .sendMessage(MessageUtils.getColoredMessage("&8&m----------------------------------------------"));
        Bukkit.getConsoleSender()
                .sendMessage(MessageUtils.getColoredMessage("&a&l[MySQL] &fConnection established successfully"));

        List<MainConfigManager.DatabaseEndpoint> endpoints = mainConfigManager.getDbStatusEndpoints();
        for (MainConfigManager.DatabaseEndpoint endpoint : endpoints) {
            Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&a✔ &fServer &e" + endpoint.getName()
                    + " &f(" + endpoint.getHost() + ":" + endpoint.getPort() + ") connected correctly."));
        }

        Bukkit.getConsoleSender()
                .sendMessage(MessageUtils.getColoredMessage("&8&m----------------------------------------------"));
    }

    private void sendMysqlErrorReport(Exception e) {
        Bukkit.getConsoleSender()
                .sendMessage(MessageUtils.getColoredMessage("&8&m----------------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&c&l[MySQL] Connection error"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&c✖ Main host: &f" + mainConfigManager.getDbHost() + ":" + mainConfigManager.getDbPort()));
        Bukkit.getConsoleSender().sendMessage(MessageUtils
                .getColoredMessage("&c✖ Detail: &f" + (e.getMessage() == null ? "Sin detalle" : e.getMessage())));
        Bukkit.getConsoleSender()
                .sendMessage(MessageUtils.getColoredMessage("&8&m----------------------------------------------"));
    }

    private void checkUpdates() {
        if (!getMainConfigManager().isUpdateCheckEnabled())
            return;

        new UpdateChecker(this).getVersion(version -> {
            if (this.getPluginMeta().getVersion().equalsIgnoreCase(version)) {
                this.latestVersion = null;
                Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                        "&4&lMaxStaff &8» &aA check for updates was performed and nothing was found."));
            } else {
                this.latestVersion = version;

                Bukkit.getConsoleSender()
                        .sendMessage(MessageUtils.getColoredMessage("&4&lMaxStaff &8» &f&lNEW VERSION: &7" + version));
                Bukkit.getConsoleSender().sendMessage(
                        MessageUtils.getColoredMessage(
                                "&4&lMaxStaff &8» &fDownload it now at the following link: &7https://modrinth.com/plugin/maxstaff"));
            }
        });
    }

    private void startUpdateChecks() {
        if (updateCheckTask != null) {
            updateCheckTask.cancel();
            updateCheckTask = null;
        }

        if (!getMainConfigManager().isUpdateCheckEnabled()) {
            return;
        }

        checkUpdates();
        updateCheckTask = Bukkit.getScheduler().runTaskTimer(this, this::checkUpdates,
                UPDATE_CHECK_INTERVAL_TICKS, UPDATE_CHECK_INTERVAL_TICKS);
    }

    private void syncMetricsState() {
        if (getMainConfigManager().isBStatsEnabled()) {
            if (metrics == null) {
                metrics = new Metrics(this, BSTATS_PLUGIN_ID);
            }
            return;
        }

        if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    @Override
    public void onDisable() {

        if (updateCheckTask != null) {
            updateCheckTask.cancel();
            updateCheckTask = null;
        }

        if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }

        if (punishmentManager != null) {
            punishmentManager.close();
        }

        if (staffManager != null) {
            staffManager.disableAllStaff();
            staffManager.forceFlush();
        }

        if (inventorySnapshotManager != null) {
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                inventorySnapshotManager.saveSnapshot(player);
            }
        }

        if (freezeManager != null) {
            freezeManager.removeAllDisplays();
        }

        if (reportManager != null) {
            reportManager.shutdown();
        }

        if (clientTrackerManager != null) {
            clientTrackerManager.unregisterChannels();
        }

        Bukkit.getConsoleSender()
                .sendMessage(MessageUtils.getColoredMessage("&4&lMaxStaff &8» &cIt was successfully deactivated"));
    }

    public void registerCommands() {
        if (commandRegistrar == null) {
            commandRegistrar = new MaxStaffCommandRegistrar(this);
        }
        commandRegistrar.registerCommands();
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

    public IPunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }

    public boolean isStaffChatToggled(UUID uuid) {
        return staffChatToggledPlayers.contains(uuid);
    }

    public boolean toggleStaffChat(UUID uuid) {
        if (staffChatToggledPlayers.contains(uuid)) {
            staffChatToggledPlayers.remove(uuid);
            return false;
        }

        staffChatToggledPlayers.add(uuid);
        return true;
    }

    public void disableStaffChat(UUID uuid) {
        staffChatToggledPlayers.remove(uuid);
    }

    public ReportManager getReportManager() {
        return reportManager;
    }

    public InventorySnapshotManager getInventorySnapshotManager() {
        return inventorySnapshotManager;
    }

    public ClientTrackerManager getClientTrackerManager() {
        return clientTrackerManager;
    }

    public AntiXrayListener getAntiXrayListener() {
        return antiXrayListener;
    }

    public void registerEvents() {
        if (isModuleEnabled("staff-mode") || isModuleEnabled("freeze")) {
            getServer().getPluginManager().registerEvents(new CommandBlockListener(this), this);
        }
        if (isModuleEnabled("staff-mode")) {
            getServer().getPluginManager().registerEvents(new StaffItemsListener(this), this);
            getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
            getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
            getServer().getPluginManager().registerEvents(new StaffModeListener(this), this);
            getServer().getPluginManager().registerEvents(new VanishProtectionListener(this), this);
        }
        if ((isModuleEnabled("sanctions-gui") && isModuleEnabled("punishments")) || isModuleEnabled("punishments")
                || isModuleEnabled("invsee") || isModuleEnabled("revive") || isModuleEnabled("alts")
                || isModuleEnabled("gamemode-gui") || isModuleEnabled("anti-xray")) {
            getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        }
        if (isModuleEnabled("freeze")) {
            getServer().getPluginManager().registerEvents(new FreezeListener(this), this);
        }
        if (isModuleEnabled("chat") || isModuleEnabled("punishments")) {
            getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        }
        if (isModuleEnabled("punishments")) {
            getServer().getPluginManager().registerEvents(new GlobalPunishmentListener(this), this);
        }
        if (isModuleEnabled("command-spy")) {
            getServer().getPluginManager().registerEvents(new CommandSpyListener(this), this);
        }
        if (isModuleEnabled("revive")) {
            getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        }
        if (isModuleEnabled("client-tracker")) {
            getServer().getPluginManager().registerEvents(clientTrackerManager, this);
        }
        if (mainConfigManager.isAntiXrayEnabled()) {
            antiXrayListener = new AntiXrayListener(this);
            getServer().getPluginManager().registerEvents(antiXrayListener, this);
        } else {
            antiXrayListener = null;
        }
    }

    public boolean isModuleEnabled(String moduleKey) {
        return mainConfigManager != null && mainConfigManager.isModuleEnabled(moduleKey, true);
    }

    public void reloadPluginState() {
        boolean wasStaffModeEnabled = isModuleEnabled("staff-mode");

        mainConfigManager.reloadConfig();
        syncMetricsState();
        startUpdateChecks();

        if (discordManager != null) {
            discordManager.reload();
        }

        if (wasStaffModeEnabled && !isModuleEnabled("staff-mode") && staffManager != null) {
            staffManager.disableAllStaff();
            staffManager.forceFlush();
        }

        if (clientTrackerManager != null) {
            if (isModuleEnabled("client-tracker")) {
                clientTrackerManager.registerChannels();
            } else {
                clientTrackerManager.unregisterChannels();
            }
        }

        if (freezeManager != null) {
            freezeManager.cleanupOrphanDisplays();
            freezeManager.refreshFrozenDisplays();
        }

        HandlerList.unregisterAll(this);
        registerCommands();
        registerEvents();
    }

}
