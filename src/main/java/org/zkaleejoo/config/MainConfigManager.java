package org.zkaleejoo.config;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

public class MainConfigManager {
    
    private CustomConfig configFile;
    private CustomConfig langFile;
    private MaxStaff plugin;
    private boolean updateCheckEnabled;
    private boolean isBroadcastEnabled;
    private String selectedLanguage;
    private Material matPunish, matFreeze, matPlayers, matInspect, matVanish;
    private Material borderMaterial, navBackMat, navNextMat, navPrevMat;
    private Material guiInfoStatsMat, guiInfoHistoryMat, guiInfoActionMat;
    private Material[] durationDyes = new Material[4];
    private ConfigurationSection warnThresholds;
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
    private String guiPlayersTitle, guiHeadLore, guiSanctionsTitle;
    private String guiItemBanName; private List<String> guiItemBanLore;
    private String guiItemMuteName; private List<String> guiItemMuteLore;
    private String guiItemKickName; private List<String> guiItemKickLore;
    private String guiInfoTitle;
    private String guiInfoStatsName, guiInfoHistoryName, guiInfoActionName;
    private List<String> guiInfoStatsLore, guiInfoHistoryLore, guiInfoActionLore;
    private String guiReasonsTitle, guiReasonsItemName, guiReasonsDyeName, guiNavLoreBack, guiNavLorePage;
    private List<String> guiReasonsItemLore, guiReasonsDyeLore;
    private String navBackName, navNextName, navPrevName;
    private String bcBan, bcMute, bcKick, bcWarn, msgWarnReceived;
    private String screenBan, screenKick, screenMute, screenUnmute, msgMutedChat;
    private String timeUnitPermanent, timeUnitDays, timeUnitHours, timeUnitMinutes, timeUnitSeconds;
    private String usestaffreset, usestafftake, takeNumberInvalid, playerNoHistory, msgResetSuccess, msgTakeSuccess;
    private String msgUpdateAvailable, msgUpdateCurrent, msgUpdateDownload;
    private String msgActionBar, statusEnabled, statusDisabled;
    private String guiHistoryTitle;
    private String guiHistoryBansName, guiHistoryMutesName, guiHistoryWarnsName, guiHistoryKicksName;
    private List<String> guiHistoryBansLore, guiHistoryMutesLore, guiHistoryWarnsLore, guiHistoryKicksLore, guiDetailedBackLore;
    private String guiDetailedTitle, guiDetailedItemName;
    private String guiDetailedDate, guiDetailedStaff, guiDetailedReason, guiDetailedDuration;
    private String msgNoIPFound, msgUnbanIPSuccess, msgInvalidIP, bcBanIP;
    private String staffChatFormat;
    private String msgCmdSpyEnabled, msgCmdSpyDisabled, msgCmdSpyFormat;
    private String msgGlobalMuteEnabled, msgGlobalMuteDisabled, msgChatIsMuted, msgChatCleared, msgChatUsage;
    private String guiGmTitle, guiGmFeedback;
    private String guiGmSurvivalName, guiGmCreativeName, guiGmAdventureName, guiGmSpectatorName;
    private List<String> guiGmSurvivalLore, guiGmCreativeLore, guiGmAdventureLore, guiGmSpectatorLore;
    private Material guiGmSurvivalMat, guiGmCreativeMat, guiGmAdventureMat, guiGmSpectatorMat;
    private boolean gmMenuEnabled;
    private String gmUse;
    private String gmModeInvalid;
    private String chatStaffUse;

    public MainConfigManager(MaxStaff plugin){
        this.plugin = plugin;
        configFile = new CustomConfig("config.yml", null, plugin, false);
        configFile.registerConfig();
        loadConfig();
    }

    public void loadConfig(){
        FileConfiguration config = configFile.getConfig();
        
        selectedLanguage = config.getString("general.language", "en");
    
        String langPath = "messages_" + selectedLanguage + ".yml";
        langFile = new CustomConfig(langPath, "lang", plugin, false);
        langFile.registerConfig();
        FileConfiguration lang = langFile.getConfig();

        prefix = config.getString("general.prefix", "&4&lMaxStaff &8» ");
        updateCheckEnabled = config.getBoolean("general.update-check", true);
        isBroadcastEnabled = config.getBoolean("punishments.broadcast", true);
        matPunish = loadMaterial(config.getString("staff-mode.items.punish.material"), Material.NETHERITE_HOE);
        matFreeze = loadMaterial(config.getString("staff-mode.items.freeze.material"), Material.PACKED_ICE);
        matPlayers = loadMaterial(config.getString("staff-mode.items.players.material"), Material.CLOCK);
        matInspect = loadMaterial(config.getString("staff-mode.items.inspect.material"), Material.CHEST);
        matVanish = loadMaterial(config.getString("staff-mode.items.vanish.material"), Material.NETHER_STAR);
        borderMaterial = loadMaterial(config.getString("gui-style.border-material"), Material.BLACK_STAINED_GLASS_PANE);
        navBackMat = loadMaterial(config.getString("gui-style.navigation.back-material"), Material.BOOK);
        navNextMat = loadMaterial(config.getString("gui-style.navigation.next-material"), Material.ARROW);
        navPrevMat = loadMaterial(config.getString("gui-style.navigation.prev-material"), Material.ARROW);
        durationDyes[0] = loadMaterial(config.getString("gui-style.unified-menu.duration-1-material"), Material.LIME_DYE);
        durationDyes[1] = loadMaterial(config.getString("gui-style.unified-menu.duration-2-material"), Material.YELLOW_DYE);
        durationDyes[2] = loadMaterial(config.getString("gui-style.unified-menu.duration-3-material"), Material.ORANGE_DYE);
        durationDyes[3] = loadMaterial(config.getString("gui-style.unified-menu.duration-4-material"), Material.RED_DYE);
        guiInfoStatsMat = loadMaterial(config.getString("gui.info.items.stats.material"), Material.BOOK);
        guiInfoHistoryMat = loadMaterial(config.getString("gui.info.items.history.material"), Material.PAPER);
        guiInfoActionMat = loadMaterial(config.getString("gui.info.items.action_punish.material"), Material.NETHERITE_SWORD);
        guiGmSurvivalMat = loadMaterial(config.getString("gui-gamemode.survival-material"), Material.GRASS_BLOCK);
        guiGmCreativeMat = loadMaterial(config.getString("gui-gamemode.creative-material"), Material.BEACON);
        guiGmAdventureMat = loadMaterial(config.getString("gui-gamemode.adventure-material"), Material.MAP);
        guiGmSpectatorMat = loadMaterial(config.getString("gui-gamemode.spectator-material"), Material.ENDER_EYE);
        gmMenuEnabled = config.getBoolean("gui-gamemode.enabled-menu", true);

        warnThresholds = config.getConfigurationSection("punishments.broadcasts.warns.thresholds");

        //MENSAJES
        noPermission = lang.getString("messages.no-permission");
        pluginReload = lang.getString("messages.plugin-reload");
        subcommandInvalid = lang.getString("messages.subcommand-invalid");
        subcommandSpecified = lang.getString("messages.subcommand-specified");
        msgConsole = lang.getString("messages.message-console", "&cOnly players!");
        noReason = lang.getString("messages.no-reason", "No reason");
        playerMuted = lang.getString("messages.player-muted");
        msgTeleport = lang.getString("messages.teleport-success");
        helpTitle = lang.getString("messages.command-help-title");
        helpLines = lang.getStringList("messages.command-help-list");
        msgInvalidMaterial = lang.getString("messages.invalid-material", "&cInvalid material: {path}");
        playerClickPls = lang.getString("messages.player-click-pls");
        staffModeEnabled = lang.getString("staff-mode.enabled");
        staffModeDisabled = lang.getString("staff-mode.disabled");
        inventorySaved = lang.getString("staff-mode.inventory-saved");
        inventoryRestored = lang.getString("staff-mode.inventory-restored");
        cannotDrop = lang.getString("staff-mode.cannot-drop");
        cannotPlace = lang.getString("staff-mode.cannot-place");
        msgInspect = lang.getString("staff-mode.items.inspect.message");
        msgVanishOn = lang.getString("staff-mode.items.vanish.message-on");
        msgVanishOff = lang.getString("staff-mode.items.vanish.message-off");
        msgPunish = lang.getString("staff-mode.items.punish.message");
        msgPlayers = lang.getString("staff-mode.items.players.message");
        msgFreezeStaff = lang.getString("staff-mode.items.freeze.message-freeze");
        msgUnfreezeStaff = lang.getString("staff-mode.items.freeze.message-unfreeze");
        msgTargetFrozen = lang.getStringList("staff-mode.items.freeze.target-frozen");
        msgTargetUnfrozen = lang.getString("staff-mode.items.freeze.target-unfrozen");
        itemNamePunish = lang.getString("staff-mode.items.punish.name");
        itemNameFreeze = lang.getString("staff-mode.items.freeze.name");
        itemNamePlayers = lang.getString("staff-mode.items.players.name");
        itemNameInspect = lang.getString("staff-mode.items.inspect.name");
        itemNameVanish = lang.getString("staff-mode.items.vanish.name");
        guiPlayersTitle = lang.getString("gui.players.title");
        guiHeadLore = lang.getString("gui.players.head-lore");
        guiSanctionsTitle = lang.getString("gui.sanctions.title");
        guiItemBanName = lang.getString("gui.sanctions.items.ban.name");
        guiItemBanLore = lang.getStringList("gui.sanctions.items.ban.lore");
        guiItemMuteName = lang.getString("gui.sanctions.items.mute.name");
        guiItemMuteLore = lang.getStringList("gui.sanctions.items.mute.lore");
        guiItemKickName = lang.getString("gui.sanctions.items.kick.name");
        guiItemKickLore = lang.getStringList("gui.sanctions.items.kick.lore");
        guiInfoTitle = lang.getString("gui.info.title", "&8Information: &0{target}");
        guiInfoStatsName = lang.getString("gui.info.items.stats.name");
        guiInfoStatsLore = lang.getStringList("gui.info.items.stats.lore");
        guiInfoHistoryName = lang.getString("gui.info.items.history.name");
        guiInfoHistoryLore = lang.getStringList("gui.info.items.history.lore");
        guiInfoActionName = lang.getString("gui.info.items.action_punish.name");
        guiInfoActionLore = lang.getStringList("gui.info.items.action_punish.lore");
        guiReasonsTitle = lang.getString("gui.reasons.title");
        guiReasonsItemName = lang.getString("gui.reasons.item-name");
        guiReasonsItemLore = lang.getStringList("gui.reasons.item-lore");
        guiReasonsDyeName = lang.getString("gui.reasons.dye-name");
        guiReasonsDyeLore = lang.getStringList("gui.reasons.dye-lore");
        guiNavLoreBack = lang.getString("gui.reasons.navigation.lore-back");
        guiNavLorePage = lang.getString("gui.reasons.navigation.lore-page");
        navBackName = lang.getString("gui-style.navigation.back-name");
        navNextName = lang.getString("gui-style.navigation.next-name");
        navPrevName = lang.getString("gui-style.navigation.prev-name");
        timeUnitPermanent = lang.getString("time-units.permanent", "Permanent");
        timeUnitDays = lang.getString("time-units.days", "days");
        timeUnitHours = lang.getString("time-units.hours", "hours");
        timeUnitMinutes = lang.getString("time-units.minutes", "minutes");
        timeUnitSeconds = lang.getString("time-units.seconds", "seconds");
        bcBan = lang.getString("punishments.broadcasts.ban");
        bcMute = lang.getString("punishments.broadcasts.mute");
        bcKick = lang.getString("punishments.broadcasts.kick");
        bcWarn = lang.getString("punishments.broadcasts.warns.broadcast");
        msgWarnReceived = lang.getString("punishments.broadcasts.warns.received");
        screenBan = lang.getString("punishments.screens.ban");
        screenKick = lang.getString("punishments.screens.kick");
        screenMute = lang.getString("punishments.screens.mute");
        screenUnmute = lang.getString("punishments.screens.unmute");
        msgMutedChat = lang.getString("punishments.screens.muted-chat");
        msgOffline = lang.getString("punishments.feedback.player-offline");
        msgNotMuted = lang.getString("punishments.feedback.not-muted");
        msgUnbanSuccess = lang.getString("punishments.feedback.unban-success");
        msgUnmuteSuccess = lang.getString("punishments.feedback.unmute-success");
        msgUsage = lang.getString("punishments.feedback.usage");
        usestaffreset = lang.getString("messages.use-staffreset");
        usestafftake = lang.getString("messages.use-stafftake");
        msgResetSuccess = lang.getString("messages.staff-reset-success");
        msgTakeSuccess = lang.getString("messages.staff-take-success");
        takeNumberInvalid = lang.getString("messages.take-number-invalid");
        playerNoHistory = lang.getString("messages.player-no-history");
        msgUpdateAvailable = lang.getString("messages.update-available", "&eA new version is available! (&b{version}&e)");
        msgUpdateCurrent = lang.getString("messages.update-current", "&7Your current version: &c{version}");
        msgUpdateDownload = lang.getString("messages.update-download", "&eDownload it to get improvements and fixes.");
        msgActionBar = lang.getString("staff-mode.action-bar", "&4&lSTAFF MODE &8| &fVanish: {status}");
        statusEnabled = lang.getString("staff-mode.status-enabled", "&aENABLED");
        statusDisabled = lang.getString("staff-mode.status-disabled", "&cDISABLED");
        guiHistoryTitle = lang.getString("gui.history.title");
        guiHistoryBansName = lang.getString("gui.history.items.bans.name");
        guiHistoryBansLore = lang.getStringList("gui.history.items.bans.lore");
        guiHistoryMutesName = lang.getString("gui.history.items.mutes.name");
        guiHistoryMutesLore = lang.getStringList("gui.history.items.mutes.lore");
        guiHistoryWarnsName = lang.getString("gui.history.items.warns.name");
        guiHistoryWarnsLore = lang.getStringList("gui.history.items.warns.lore");
        guiHistoryKicksName = lang.getString("gui.history.items.kicks.name");
        guiHistoryKicksLore = lang.getStringList("gui.history.items.kicks.lore");
        guiDetailedTitle = lang.getString("gui.history-detailed.title");
        guiDetailedItemName = lang.getString("gui.history-detailed.item-name");
        guiDetailedBackLore = lang.getStringList("gui.history-detailed.back-lore");
        guiDetailedDate = lang.getString("gui.history-detailed.item-lore.date");
        guiDetailedStaff = lang.getString("gui.history-detailed.item-lore.staff");
        guiDetailedReason = lang.getString("gui.history-detailed.item-lore.reason");
        guiDetailedDuration = lang.getString("gui.history-detailed.item-lore.duration");
        msgNoIPFound = lang.getString("punishments.feedback.no-ip-found", "&cNo IP found for this player.");
        msgUnbanIPSuccess = lang.getString("punishments.feedback.unban-ip-success", "&aIP &e{ip} &ahas been unbanned.");
        msgInvalidIP = lang.getString("punishments.feedback.invalid-ip", "&cCould not find a valid IP for: &e{target}");
        bcBanIP = lang.getString("punishments.broadcasts.ban-ip", "&c&lIP-BAN &8» &f{target} &7was IP banned by &c{staff} &7({duration}).");
        staffChatFormat = lang.getString("messages.staff-chat-format", "&8[&4&lSTAFF&8] &b{player}&8: &f{message}");
        msgCmdSpyEnabled = lang.getString("messages.command-spy-enabled", "&aModo Espía de Comandos ACTIVADO.");
        msgCmdSpyDisabled = lang.getString("messages.command-spy-disabled", "&cModo Espía de Comandos DESACTIVADO.");
        msgCmdSpyFormat = lang.getString("messages.command-spy-format", "&8[&6Spy&8] &e{player}&8: &f{command}");
        msgGlobalMuteEnabled = lang.getString("chat.global-mute-enabled", "&cEl chat global ha sido &lSILENCIADO &cpor &e{player}&c.");
        msgGlobalMuteDisabled = lang.getString("chat.global-mute-disabled", "&aEl chat global ha sido &lACTIVADO &apor &e{player}&a.");
        msgChatIsMuted = lang.getString("chat.chat-is-muted", "&cEl chat está silenciado globalmente en este momento.");
        msgChatCleared = lang.getString("chat.cleared", "&6&lCHAT LIMPIADO &7por &e{player}&7.");
        msgChatUsage = lang.getString("chat.usage", "&cUso: /chat <mute|clear>");
        guiGmTitle = lang.getString("gui.gamemode.title", "&8Selector de Modo de Juego");
        guiGmFeedback = lang.getString("gui.feedback.gamemode-changed", "&aTu modo de juego ha sido cambiado a &e{mode}&a.");
        guiGmSurvivalName = lang.getString("gui.gamemode.survival.name");
        guiGmSurvivalLore = lang.getStringList("gui.gamemode.survival.lore");
        guiGmCreativeName = lang.getString("gui.gamemode.creative.name");
        guiGmCreativeLore = lang.getStringList("gui.gamemode.creative.lore");
        guiGmAdventureName = lang.getString("gui.gamemode.adventure.name");
        guiGmAdventureLore = lang.getStringList("gui.gamemode.adventure.lore");
        guiGmSpectatorName = lang.getString("gui.gamemode.spectator.name");
        guiGmSpectatorLore = lang.getStringList("gui.gamemode.spectator.lore");
        gmUse = lang.getString("messages.gamemode-use", "&cUsage: /gm <0|1|2|3>");
        gmModeInvalid = lang.getString("messages.gamemode-modeinvalid", "&cInvalid game mode");
        chatStaffUse = lang.getString("messages.chatstaff-use", "&c&l(!) &cWrite a message for the staff. Usage: /sc <message>");
        

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

    public void reloadConfig(){ 
        configFile.reloadConfig(); 
        if(langFile != null) langFile.reloadConfig();
        loadConfig(); 
    }

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
    public String getUseStaffReset() { return usestaffreset; }
    public String getUseStaffTake() { return usestafftake; }
    public String getTakeNumberInvalid() { return takeNumberInvalid; }
    public String getPlayerNoHistory() { return playerNoHistory; }
    public String getMsgResetSuccess() { return msgResetSuccess; }
    public String getMsgTakeSuccess() { return msgTakeSuccess; }
    public String getMsgUpdateAvailable() { return msgUpdateAvailable; }
    public String getMsgUpdateCurrent() { return msgUpdateCurrent; }
    public String getMsgUpdateDownload() { return msgUpdateDownload; }
    public String getMsgActionBar() { return msgActionBar; }
    public String getStatusEnabled() { return statusEnabled; }
    public String getStatusDisabled() { return statusDisabled; }
    public String getGuiHistoryTitle() { return guiHistoryTitle; }
    public String getGuiHistoryBansName() { return guiHistoryBansName; }
    public List<String> getGuiHistoryBansLore() { return guiHistoryBansLore; }
    public String getGuiHistoryMutesName() { return guiHistoryMutesName; }
    public List<String> getGuiHistoryMutesLore() { return guiHistoryMutesLore; }
    public String getGuiHistoryWarnsName() { return guiHistoryWarnsName; }
    public List<String> getGuiHistoryWarnsLore() { return guiHistoryWarnsLore; }
    public String getGuiHistoryKicksName() { return guiHistoryKicksName; }
    public List<String> getGuiHistoryKicksLore() { return guiHistoryKicksLore; }
    public String getGuiDetailedTitle() { return guiDetailedTitle; }
    public String getGuiDetailedItemName() { return guiDetailedItemName; }
    public List<String> getGuiDetailedBackLore() { return guiDetailedBackLore; }
    public String getGuiDetailedDate() { return guiDetailedDate; }
    public String getGuiDetailedStaff() { return guiDetailedStaff; }
    public String getGuiDetailedReason() { return guiDetailedReason; }
    public String getGuiDetailedDuration() { return guiDetailedDuration; }
    public String getMsgNoIPFound() { return msgNoIPFound; }
    public String getMsgUnbanIPSuccess() { return msgUnbanIPSuccess; }
    public String getMsgInvalidIP() { return msgInvalidIP; }
    public String getBcBanIP() { return bcBanIP; }
    public String getStaffChatFormat() { return staffChatFormat; }  
    public String getMsgCmdSpyEnabled() { return msgCmdSpyEnabled; }
    public String getMsgCmdSpyDisabled() { return msgCmdSpyDisabled; }
    public String getMsgCmdSpyFormat() { return msgCmdSpyFormat; }
    public String getMsgGlobalMuteEnabled() { return msgGlobalMuteEnabled; }
    public String getMsgGlobalMuteDisabled() { return msgGlobalMuteDisabled; }
    public String getMsgChatIsMuted() { return msgChatIsMuted; }
    public String getMsgChatCleared() { return msgChatCleared; }
    public String getMsgChatUsage() { return msgChatUsage; }
    public String getGuiGmTitle() { return guiGmTitle; }
    public String getGuiGmFeedback() { return guiGmFeedback; }
    public String getGuiGmSurvivalName() { return guiGmSurvivalName; }
    public List<String> getGuiGmSurvivalLore() { return guiGmSurvivalLore; }
    public Material getGuiGmSurvivalMat() { return guiGmSurvivalMat; }
    public String getGuiGmCreativeName() { return guiGmCreativeName; }
    public List<String> getGuiGmCreativeLore() { return guiGmCreativeLore; }
    public Material getGuiGmCreativeMat() { return guiGmCreativeMat; }
    public String getGuiGmAdventureName() { return guiGmAdventureName; }
    public List<String> getGuiGmAdventureLore() { return guiGmAdventureLore; }
    public Material getGuiGmAdventureMat() { return guiGmAdventureMat; }
    public String getGuiGmSpectatorName() { return guiGmSpectatorName; }
    public List<String> getGuiGmSpectatorLore() { return guiGmSpectatorLore; }
    public Material getGuiGmSpectatorMat() { return guiGmSpectatorMat; }
    public boolean isGmMenuEnabled() { return gmMenuEnabled; }
    public String getGmUse() { return gmUse; }
    public String getGmModeInvalid() { return gmModeInvalid; }
    public String getChatStaffUse() { return chatStaffUse; }
    

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