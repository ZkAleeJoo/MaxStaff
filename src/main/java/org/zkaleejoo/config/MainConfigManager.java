package org.zkaleejoo.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.zkaleejoo.MaxStaff;
import java.util.List;

public class MainConfigManager {
    
    private CustomConfig configFile;
    private MaxStaff plugin;

    // --- 1. VARIABLES GENERALES ---
    private String prefix;
    private String noPermission;
    private String pluginReload;
    private String subcommandInvalid;
    private String subcommandSpecified;

    // --- 2. VARIABLES STAFF MODE ---
    private String staffModeEnabled;
    private String staffModeDisabled;
    private String inventorySaved;
    private String inventoryRestored;
    private String cannotDrop;
    private String cannotPlace;

    // --- 3. VARIABLES DE ÍTEMS ---
    private String msgInspect;
    private String msgVanishOn;
    private String msgVanishOff;
    private String msgPunish;       
    private String msgPlayers;
    private String msgFreezeStaff;
    private String msgUnfreezeStaff;
    private List<String> msgTargetFrozen;
    private String msgTargetUnfrozen;

    // --- 4. VARIABLES GUI (MENÚS) ---
    private String guiPlayersTitle;
    private String guiHeadLore;
    
    private String guiSanctionsTitle;
    private String guiItemBanName; private List<String> guiItemBanLore;
    private String guiItemMuteName; private List<String> guiItemMuteLore;
    private String guiItemKickName; private List<String> guiItemKickLore;
    
    private String guiDurationTitle;
    private String guiTime1hName; private String guiTime1hLore;
    private String guiTime1dName; private String guiTime1dLore;
    private String guiTime7dName; private String guiTime7dLore;
    private String guiTimePermName; private String guiTimePermLore;
    private String guiBackName;

    // --- 5. VARIABLES DE CASTIGOS ---
    private boolean isBroadcastEnabled; 
    private String defaultReason;
    private String bcBan;
    private String bcMute;
    private String bcKick;
    private String screenBan;
    private String screenKick;
    private String screenMute;
    private String screenUnmute;
    private String msgMutedChat;
    private String msgOffline;
    private String msgNotMuted;
    private String msgUnbanSuccess;
    private String msgUnmuteSuccess;
    private String msgUsage;

    public MainConfigManager(MaxStaff plugin){
        this.plugin = plugin;
        configFile = new CustomConfig("config.yml", null, plugin, false);
        configFile.registerConfig();
        loadConfig();
    }

    public void loadConfig(){
        FileConfiguration config = configFile.getConfig();
        
        // 1. General
        prefix = config.getString("general.prefix");
        noPermission = config.getString("messages.no-permission");
        pluginReload = config.getString("messages.plugin-reload");
        subcommandInvalid = config.getString("messages.subcommand-invalid");
        subcommandSpecified = config.getString("messages.subcommand-specified");

        // 2. Staff Mode
        staffModeEnabled = config.getString("staff-mode.enabled");
        staffModeDisabled = config.getString("staff-mode.disabled");
        inventorySaved = config.getString("staff-mode.inventory-saved");
        inventoryRestored = config.getString("staff-mode.inventory-restored");
        cannotDrop = config.getString("staff-mode.cannot-drop");
        cannotPlace = config.getString("staff-mode.cannot-place");

        // 3. Items Messages 
        msgInspect = config.getString("staff-mode.items.inspect.message");
        msgVanishOn = config.getString("staff-mode.items.vanish.message-on");
        msgVanishOff = config.getString("staff-mode.items.vanish.message-off");
        msgPunish = config.getString("staff-mode.items.punish.message"); 
        msgPlayers = config.getString("staff-mode.items.players.message");
        
        // Freeze Items
        msgFreezeStaff = config.getString("staff-mode.items.freeze.message-freeze");
        msgUnfreezeStaff = config.getString("staff-mode.items.freeze.message-unfreeze");
        msgTargetFrozen = config.getStringList("staff-mode.items.freeze.target-frozen");
        msgTargetUnfrozen = config.getString("staff-mode.items.freeze.target-unfrozen");

        // 4. GUI Players
        guiPlayersTitle = config.getString("gui.players.title");
        guiHeadLore = config.getString("gui.players.head-lore");
        
        // GUI Sanctions
        guiSanctionsTitle = config.getString("gui.sanctions.title");
        guiItemBanName = config.getString("gui.sanctions.items.ban.name");
        guiItemBanLore = config.getStringList("gui.sanctions.items.ban.lore");
        guiItemMuteName = config.getString("gui.sanctions.items.mute.name");
        guiItemMuteLore = config.getStringList("gui.sanctions.items.mute.lore");
        guiItemKickName = config.getString("gui.sanctions.items.kick.name");
        guiItemKickLore = config.getStringList("gui.sanctions.items.kick.lore");
        
        // GUI Duration
        guiDurationTitle = config.getString("gui.duration.title");
        guiTime1hName = config.getString("gui.duration.items.1h.name");
        guiTime1hLore = config.getString("gui.duration.items.1h.lore");
        guiTime1dName = config.getString("gui.duration.items.1d.name");
        guiTime1dLore = config.getString("gui.duration.items.1d.lore");
        guiTime7dName = config.getString("gui.duration.items.7d.name");
        guiTime7dLore = config.getString("gui.duration.items.7d.lore");
        guiTimePermName = config.getString("gui.duration.items.perm.name");
        guiTimePermLore = config.getString("gui.duration.items.perm.lore");
        guiBackName = config.getString("gui.duration.items.back");

        // 5. Punishments
        isBroadcastEnabled = config.getBoolean("punishments.broadcast");
        defaultReason = config.getString("punishments.default-reason");
        bcBan = config.getString("punishments.broadcasts.ban");
        bcMute = config.getString("punishments.broadcasts.mute");
        bcKick = config.getString("punishments.broadcasts.kick");
        screenBan = config.getString("punishments.screens.ban");
        screenKick = config.getString("punishments.screens.kick");
        screenMute = config.getString("punishments.screens.mute");
        screenUnmute = config.getString("punishments.screens.unmute");
        msgMutedChat = config.getString("punishments.screens.muted-chat");
        msgOffline = config.getString("punishments.feedback.player-offline");
        msgNotMuted = config.getString("punishments.feedback.not-muted");
        msgUnbanSuccess = config.getString("punishments.feedback.unban-success");
        msgUnmuteSuccess = config.getString("punishments.feedback.unmute-success");
        msgUsage = config.getString("punishments.feedback.usage");
    }

    public void reloadConfig(){
        configFile.reloadConfig();
        loadConfig();
    }
    
    // GETTERS
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
    public String getMsgFreezeStaff() { return msgFreezeStaff; }
    public String getMsgUnfreezeStaff() { return msgUnfreezeStaff; }
    public List<String> getMsgTargetFrozen() { return msgTargetFrozen; }
    public String getMsgTargetUnfrozen() { return msgTargetUnfrozen; }

    public String getGuiPlayersTitle() { return guiPlayersTitle; }
    public String getGuiHeadLore() { return guiHeadLore; }
    public String getGuiSanctionsTitle() { return guiSanctionsTitle; }
    public String getGuiItemBanName() { return guiItemBanName; }
    public List<String> getGuiItemBanLore() { return guiItemBanLore; }
    public String getGuiItemMuteName() { return guiItemMuteName; }
    public List<String> getGuiItemMuteLore() { return guiItemMuteLore; }
    public String getGuiItemKickName() { return guiItemKickName; }
    public List<String> getGuiItemKickLore() { return guiItemKickLore; }
    
    public String getGuiDurationTitle() { return guiDurationTitle; }
    public String getGuiTime1hName() { return guiTime1hName; } public String getGuiTime1hLore() { return guiTime1hLore; }
    public String getGuiTime1dName() { return guiTime1dName; } public String getGuiTime1dLore() { return guiTime1dLore; }
    public String getGuiTime7dName() { return guiTime7dName; } public String getGuiTime7dLore() { return guiTime7dLore; }
    public String getGuiTimePermName() { return guiTimePermName; } public String getGuiTimePermLore() { return guiTimePermLore; }
    public String getGuiBackName() { return guiBackName; }

    public boolean isBroadcastEnabled() { return isBroadcastEnabled; }
    public String getDefaultReason() { return defaultReason; }
    public String getBcBan() { return bcBan; }
    public String getBcMute() { return bcMute; }
    public String getBcKick() { return bcKick; }
    public String getScreenBan() { return screenBan; }
    public String getScreenKick() { return screenKick; }
    public String getScreenMute() { return screenMute; }
    public String getScreenUnmute() { return screenUnmute; }
    public String getMsgMutedChat() { return msgMutedChat; }
    public String getMsgOffline() { return msgOffline; }
    public String getMsgNotMuted() { return msgNotMuted; }
    public String getMsgUnbanSuccess() { return msgUnbanSuccess; }
    public String getMsgUnmuteSuccess() { return msgUnmuteSuccess; }
    public String getMsgUsage() { return msgUsage; }
}