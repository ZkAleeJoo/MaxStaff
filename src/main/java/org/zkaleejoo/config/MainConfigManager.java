package org.zkaleejoo.config;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

public class MainConfigManager {
    
    private CustomConfig configFile;
    private MaxStaff plugin;
    private boolean updateCheckEnabled;
    
    private String prefix, noPermission, pluginReload, subcommandInvalid, subcommandSpecified;
    private String msgConsole, noReason, playerMuted, msgTeleport, helpTitle, msgUsage;
    private List<String> helpLines;
    private String msgOffline, msgNotMuted, msgUnbanSuccess, msgUnmuteSuccess, msgInvalidMaterial;
    private String playerClickPls;

    private String staffModeEnabled, staffModeDisabled, inventorySaved, inventoryRestored;
    private String cannotDrop, cannotPlace, msgInspect, msgVanishOn, msgVanishOff;
    private String msgPunish, msgPlayers, msgFreezeStaff, msgUnfreezeStaff;
    private List<String> msgTargetFrozen;
    private String msgTargetUnfrozen;
    private String itemNamePunish, itemNameFreeze, itemNamePlayers, itemNameInspect, itemNameVanish;
    private Material matPunish, matFreeze, matPlayers, matInspect, matVanish;
    private String bcWarn, msgWarnReceived;
    private ConfigurationSection warnThresholds;
 
    private String guiPlayersTitle, guiHeadLore, guiSanctionsTitle;
    private String guiItemBanName; private List<String> guiItemBanLore;
    private String guiItemMuteName; private List<String> guiItemMuteLore;
    private String guiItemKickName; private List<String> guiItemKickLore;
    private Material borderMaterial;
     
    private String guiInfoTitle;
    private Material guiInfoStatsMat, guiInfoHistoryMat, guiInfoActionMat;
    private String guiInfoStatsName, guiInfoHistoryName, guiInfoActionName;
    private List<String> guiInfoStatsLore, guiInfoHistoryLore, guiInfoActionLore;

    private String guiReasonsTitle, guiReasonsItemName, guiReasonsDyeName, guiNavLoreBack, guiNavLorePage;
    private List<String> guiReasonsItemLore, guiReasonsDyeLore;
    private String navBackName, navNextName, navPrevName;
    private Material navBackMat, navNextMat, navPrevMat;
    private Material[] durationDyes = new Material[4];

    private boolean isBroadcastEnabled;
    private String bcBan, bcMute, bcKick, screenBan, screenKick, screenMute, screenUnmute, msgMutedChat;
    private String timeUnitPermanent, timeUnitDays, timeUnitHours, timeUnitMinutes, timeUnitSeconds;

    public MainConfigManager(MaxStaff plugin){
        this.plugin = plugin;
        configFile = new CustomConfig("config.yml", null, plugin, false);
        configFile.registerConfig();
        loadConfig();
    }

    public void loadConfig(){
        FileConfiguration config = configFile.getConfig();
        
        prefix = config.getString("general.prefix", "&4&lMaxStaff &8» ");
        noPermission = config.getString("messages.no-permission");
        pluginReload = config.getString("messages.plugin-reload");
        subcommandInvalid = config.getString("messages.subcommand-invalid");
        subcommandSpecified = config.getString("messages.subcommand-specified");
        msgConsole = config.getString("messages.message-console", "&cOnly players!");
        noReason = config.getString("messages.no-reason", "No reason");
        playerMuted = config.getString("messages.player-muted");
        msgTeleport = config.getString("messages.teleport-success");
        helpTitle = config.getString("messages.command-help-title");
        helpLines = config.getStringList("messages.command-help-list");
        msgInvalidMaterial = config.getString("messages.invalid-material", "&cInvalid material: {path}");

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
        msgFreezeStaff = config.getString("staff-mode.items.freeze.message-freeze");
        msgUnfreezeStaff = config.getString("staff-mode.items.freeze.message-unfreeze");
        msgTargetFrozen = config.getStringList("staff-mode.items.freeze.target-frozen");
        msgTargetUnfrozen = config.getString("staff-mode.items.freeze.target-unfrozen");
        itemNamePunish = config.getString("staff-mode.items.punish.name");
        itemNameFreeze = config.getString("staff-mode.items.freeze.name");
        itemNamePlayers = config.getString("staff-mode.items.players.name");
        itemNameInspect = config.getString("staff-mode.items.inspect.name");
        itemNameVanish = config.getString("staff-mode.items.vanish.name");
        matPunish = loadMaterial(config.getString("staff-mode.items.punish.material"), Material.NETHERITE_HOE);
        matFreeze = loadMaterial(config.getString("staff-mode.items.freeze.material"), Material.PACKED_ICE);
        matPlayers = loadMaterial(config.getString("staff-mode.items.players.material"), Material.CLOCK);
        matInspect = loadMaterial(config.getString("staff-mode.items.inspect.material"), Material.CHEST);
        matVanish = loadMaterial(config.getString("staff-mode.items.vanish.material"), Material.NETHER_STAR);

        guiPlayersTitle = config.getString("gui.players.title");
        guiHeadLore = config.getString("gui.players.head-lore");
        guiSanctionsTitle = config.getString("gui.sanctions.title");
        guiItemBanName = config.getString("gui.sanctions.items.ban.name");
        guiItemBanLore = config.getStringList("gui.sanctions.items.ban.lore");
        guiItemMuteName = config.getString("gui.sanctions.items.mute.name");
        guiItemMuteLore = config.getStringList("gui.sanctions.items.mute.lore");
        guiItemKickName = config.getString("gui.sanctions.items.kick.name");
        guiItemKickLore = config.getStringList("gui.sanctions.items.kick.lore");
        
        guiInfoTitle = config.getString("gui.info.title", "&8Information: &0{target}");
        guiInfoStatsMat = loadMaterial(config.getString("gui.info.items.stats.material"), Material.BOOK);
        guiInfoStatsName = config.getString("gui.info.items.stats.name");
        guiInfoStatsLore = config.getStringList("gui.info.items.stats.lore");
        guiInfoHistoryMat = loadMaterial(config.getString("gui.info.items.history.material"), Material.PAPER);
        guiInfoHistoryName = config.getString("gui.info.items.history.name");
        guiInfoHistoryLore = config.getStringList("gui.info.items.history.lore");
        guiInfoActionMat = loadMaterial(config.getString("gui.info.items.action_punish.material"), Material.NETHERITE_SWORD);
        guiInfoActionName = config.getString("gui.info.items.action_punish.name");
        guiInfoActionLore = config.getStringList("gui.info.items.action_punish.lore");

        guiReasonsTitle = config.getString("gui.reasons.title");
        guiReasonsItemName = config.getString("gui.reasons.item-name");
        guiReasonsItemLore = config.getStringList("gui.reasons.item-lore");
        guiReasonsDyeName = config.getString("gui.reasons.dye-name");
        guiReasonsDyeLore = config.getStringList("gui.reasons.dye-lore");
        guiNavLoreBack = config.getString("gui.reasons.navigation.lore-back");
        guiNavLorePage = config.getString("gui.reasons.navigation.lore-page");
        
        borderMaterial = loadMaterial(config.getString("gui-style.border-material"), Material.BLACK_STAINED_GLASS_PANE);
        navBackName = config.getString("gui-style.navigation.back-name");
        navNextName = config.getString("gui-style.navigation.next-name");
        navPrevName = config.getString("gui-style.navigation.prev-name");
        navBackMat = loadMaterial(config.getString("gui-style.navigation.back-material"), Material.BOOK);
        navNextMat = loadMaterial(config.getString("gui-style.navigation.next-material"), Material.ARROW);
        navPrevMat = loadMaterial(config.getString("gui-style.navigation.prev-material"), Material.ARROW);
        durationDyes[0] = loadMaterial(config.getString("gui-style.unified-menu.duration-1-material"), Material.LIME_DYE);
        durationDyes[1] = loadMaterial(config.getString("gui-style.unified-menu.duration-2-material"), Material.YELLOW_DYE);
        durationDyes[2] = loadMaterial(config.getString("gui-style.unified-menu.duration-3-material"), Material.ORANGE_DYE);
        durationDyes[3] = loadMaterial(config.getString("gui-style.unified-menu.duration-4-material"), Material.RED_DYE);

        timeUnitPermanent = config.getString("time-units.permanent", "Permanent");
        timeUnitDays = config.getString("time-units.days", "days");
        timeUnitHours = config.getString("time-units.hours", "hours");
        timeUnitMinutes = config.getString("time-units.minutes", "minutes");
        timeUnitSeconds = config.getString("time-units.seconds", "seconds");

        isBroadcastEnabled = config.getBoolean("punishments.broadcast");
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
        playerClickPls = config.getString("messages.player-click-pls");

        updateCheckEnabled = config.getBoolean("general.update-check", true);
        bcWarn = config.getString("punishments.broadcasts.warns.broadcast");
        msgWarnReceived = config.getString("punishments.broadcasts.warns.received");
        warnThresholds = config.getConfigurationSection("punishments.broadcasts.warns.thresholds");
    }

    private Material loadMaterial(String materialName, Material defaultMat) {
        if (materialName == null) return defaultMat;
        Material matched = Material.matchMaterial(materialName);
        if (matched == null) {
            plugin.getServer().getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + msgInvalidMaterial.replace("{path}", materialName).replace("{default}", defaultMat.name())));
            return defaultMat;
        }
        return matched;
    }

    public void reloadConfig(){ configFile.reloadConfig(); loadConfig(); }

    public String getPrefix() { return prefix; }
    public String getNoPermission() { return noPermission; }
    public String getPluginReload() { return pluginReload; }
    public String getSubcommandInvalid() { return subcommandInvalid; }
    public String getMsgTeleport() { return msgTeleport; }
    public String getHelpTitle() { return helpTitle; }
    public List<String> getHelpLines() { return helpLines; }
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
    public String getGuiReasonsTitle() { return guiReasonsTitle; }
    public String getGuiReasonsItemName() { return guiReasonsItemName; }
    public List<String> getGuiReasonsItemLore() { return guiReasonsItemLore; }
    public String getGuiReasonsDyeName() { return guiReasonsDyeName; }
    public List<String> getGuiReasonsDyeLore() { return guiReasonsDyeLore; }
    public String getGuiNavLoreBack() { return guiNavLoreBack; }
    public String getGuiNavLorePage() { return guiNavLorePage; }
    public String getTimeUnitPermanent() { return timeUnitPermanent; }
    public String getTimeUnitDays() { return timeUnitDays; }
    public String getTimeUnitHours() { return timeUnitHours; }
    public String getTimeUnitMinutes() { return timeUnitMinutes; }
    public String getTimeUnitSeconds() { return timeUnitSeconds; }
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
    public boolean isBroadcastEnabled() { return isBroadcastEnabled; }
    public String getNoReason() { return noReason; }
    public String getPlayerMuted() { return playerMuted; }
    public String getMsgConsole() { return msgConsole; }
    public Material getMatPunish() { return matPunish; }
    public Material getMatFreeze() { return matFreeze; }
    public Material getMatPlayers() { return matPlayers; }
    public Material getMatInspect() { return matInspect; }
    public Material getMatVanish() { return matVanish; }
    public String getItemNamePunish() { return itemNamePunish; }
    public String getItemNameFreeze() { return itemNameFreeze; }
    public String getItemNamePlayers() { return itemNamePlayers; }
    public String getItemNameInspect() { return itemNameInspect; }
    public String getItemNameVanish() { return itemNameVanish; }
    public Material getBorderMaterial() { return borderMaterial; }
    public String getNavBackName() { return navBackName; }
    public String getNavNextName() { return navNextName; }
    public String getNavPrevName() { return navPrevName; }
    public Material getNavBackMat() { return navBackMat; }
    public Material getNavNextMat() { return navNextMat; }
    public Material getNavPrevMat() { return navPrevMat; }
    public Material getDurationDye(int index) { return durationDyes[index]; }
    public String getPlayerClickPls() {return playerClickPls;}

    public String getGuiInfoTitle() { return guiInfoTitle; }
    public Material getGuiInfoStatsMat() { return guiInfoStatsMat; }
    public String getGuiInfoStatsName() { return guiInfoStatsName; }
    public List<String> getGuiInfoStatsLore() { return guiInfoStatsLore; }
    public Material getGuiInfoHistoryMat() { return guiInfoHistoryMat; }
    public String getGuiInfoHistoryName() { return guiInfoHistoryName; }
    public List<String> getGuiInfoHistoryLore() { return guiInfoHistoryLore; }
    public Material getGuiInfoActionMat() { return guiInfoActionMat; }
    public String getGuiInfoActionName() { return guiInfoActionName; }
    public List<String> getGuiInfoActionLore() { return guiInfoActionLore; }
    public boolean isUpdateCheckEnabled() { return updateCheckEnabled; }
    public String getBcWarn() { return bcWarn; }
    public String getMsgWarnReceived() { return msgWarnReceived; }
    public ConfigurationSection getWarnThresholds() { return warnThresholds; }

    public ConfigurationSection getReasons(String type) {
        return configFile.getConfig().getConfigurationSection("punishment-reasons." + type);
    }
    public String getReasonName(String type, String reasonId) {
        return configFile.getConfig().getString("punishment-reasons." + type + "." + reasonId + ".name");
    }
    public List<String> getReasonDurations(String type, String reasonId) {
        return configFile.getConfig().getStringList("punishment-reasons." + type + "." + reasonId + ".durations");
    }
    public Material getReasonMaterial(String type, String reasonId) {
        String matName = configFile.getConfig().getString("punishment-reasons." + type + "." + reasonId + ".material");
        return Material.matchMaterial(matName != null ? matName : "PAPER");
    }
}