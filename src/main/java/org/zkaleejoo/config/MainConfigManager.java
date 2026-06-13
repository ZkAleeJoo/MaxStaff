package org.zkaleejoo.config;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;
import java.util.Arrays;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.EnumSet;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.command.CommandSender;
import org.zkaleejoo.utils.TimeUtils;

public class MainConfigManager {

    private static final String GUI_SANCTIONS_TITLE_PATH = "gui.sanctions.title";
    private static final String DEFAULT_LANG_RESOURCE = "lang/messages_en.yml";

    private CustomConfig configFile;
    private CustomConfig langFile;
    private MaxStaff plugin;
    private boolean updateCheckEnabled;
    private boolean bStatsEnabled;
    private boolean isBroadcastEnabled;
    private String selectedLanguage;
    private Material matPunish, matFreeze, matPlayers, matInspect, matVanish, matWallCompass, matRandomTp;
    private Material borderMaterial, navBackMat, navNextMat, navPrevMat;
    private Material guiInfoStatsMat, guiInfoHistoryMat, guiInfoActionMat;
    private Material guiSanctionBanMat, guiSanctionMuteMat, guiSanctionKickMat;
    private Material guiHistoryBansMat, guiHistoryMutesMat, guiHistoryWarnsMat, guiHistoryKicksMat;
    private Material guiDetailedRecordMat;
    private Material guiActiveBanMat, guiActiveIpBanMat, guiActiveMuteMat, guiActiveEmptyMat, guiActivePageInfoMat;
    private Material guiConfirmYesMat, guiConfirmNoMat;
    private Material[] durationDyes = new Material[4];
    private ConfigurationSection warnThresholds;
    private String prefix, noPermission, pluginReload, subcommandInvalid, subcommandSpecified;
    private String msgConsole, noReason, playerMuted, msgTeleport, helpTitle, msgUsage;
    private List<String> helpLines;
    private String msgOffline, msgNotMuted, msgUnbanSuccess, msgUnmuteSuccess, msgPunishProtected, msgInvalidMaterial;
    private String playerClickPls;
    private String staffModeEnabled, staffModeDisabled, inventorySaved, inventoryRestored;
    private String cannotDrop, cannotPlace, msgInspect, msgVanishOn, msgVanishOff;
    private String msgPunish, msgPlayers, msgRandomTp, msgRandomTpNoTargets, msgFreezeStaff, msgUnfreezeStaff;
    private List<String> msgTargetFrozen;
    private String msgTargetUnfrozen;
    private String itemNamePunish, itemNameFreeze, itemNamePlayers, itemNameRandomTp, itemNameInspect, itemNameVanish,
            itemNameWallCompass;
    private String guiPlayersTitle, guiHeadLore, guiPlayersRandomTpName, guiPlayerOffline, guiSanctionsTitle;
    private String guiXrayTitle, guiXrayHeadName, guiXrayEmptyName, guiXrayTeleportMessage,
            guiXrayTargetOfflineMessage, antiXrayMenuDisabledMessage;
    private List<String> guiPlayersRandomTpLore, guiXrayHeadLore, guiXrayEmptyLore;
    private Material guiPlayersRandomTpMat, guiXrayEmptyMaterial;
    private String guiItemBanName;
    private List<String> guiItemBanLore;
    private String guiItemMuteName;
    private List<String> guiItemMuteLore;
    private String guiItemKickName;
    private List<String> guiItemKickLore;
    private String guiInfoTitle;
    private String guiInfoStatsName, guiInfoHistoryName, guiInfoActionName;
    private List<String> guiInfoStatsLore, guiInfoHistoryLore, guiInfoActionLore;
    private String guiConfirmTitle, guiConfirmYesName, guiConfirmNoName;
    private List<String> guiConfirmYesLore, guiConfirmNoLore;
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
    private List<String> guiHistoryBansLore, guiHistoryMutesLore, guiHistoryWarnsLore, guiHistoryKicksLore,
            guiHistoryBackLore, guiDetailedBackLore;
    private String guiDetailedTitle, guiDetailedItemName;
    private int guiHistoryBackSlot;
    private String guiDetailedDate, guiDetailedStaff, guiDetailedReason, guiDetailedDuration;
    private String guiActiveTitle, guiActiveItemName, guiActiveEmptyName, guiActivePageInfoName;
    private List<String> guiActiveItemLore, guiActiveEmptyLore, guiActivePageInfoLore;
    private int guiActiveMenuSize;
    private String msgNoIPFound, msgUnbanIPSuccess, msgInvalidIP, bcBanIP;
    private String staffChatFormat;
    private String msgCmdSpyEnabled, msgCmdSpyDisabled, msgCmdSpyFormat;
    private List<String> cmdSpySensitiveCommands, cmdSpySensitiveCommandAliases, cmdSpySensitiveCommandPatterns;
    private String cmdSpySensitiveBypassPermission, cmdSpyMaskedArgument;
    private String msgGlobalMuteEnabled, msgGlobalMuteDisabled, msgChatIsMuted, msgChatCleared, msgChatUsage;
    private String guiGmTitle, guiGmFeedback;
    private String guiGmSurvivalName, guiGmCreativeName, guiGmAdventureName, guiGmSpectatorName;
    private List<String> guiGmSurvivalLore, guiGmCreativeLore, guiGmAdventureLore, guiGmSpectatorLore;
    private Material guiGmSurvivalMat, guiGmCreativeMat, guiGmAdventureMat, guiGmSpectatorMat, healmaterialfreeze;
    private boolean gmMenuEnabled;
    private String gmUse;
    private String gmModeInvalid;
    private String flyUse, flyDisabled, flyEnabledDefault, flyEnabledLevel;
    private float flySpeedLevel1, flySpeedLevel2, flySpeedLevel3;
    private String chatStaffUse, staffChatToggleOn, staffChatToggleOff;
    private String placeholderTrue, placeholderFalse;
    private String placeholderPlaytimeFormat, restoredinventory;
    private String guiAltsTitle, guiAltsDynamic, guiAltsStatusOnline, guiAltsStatusOffline, guiAltsStatusBanned;
    private List<String> guiAltsLore;
    private String commandsanctionuse, commandsanctionself;
    private Material guiInfoAltsMat, guiInfoInvMat, guiInfoPermissionsMat;
    private String guiInfoHeadName, guiInfoAltsName, guiInfoInvName, guiInfoPermissionsName;
    private List<String> guiInfoHeadLore, guiInfoAltsLore, guiInfoInvLore, guiInfoPermissionsLore;
    private String guiPermissionsTitle, guiPermissionsItemName, guiPermissionsItemLore, guiPermissionsEmptyName;
    private List<String> guiPermissionsEmptyLore;
    private String statusOnline, statusOffline;
    private String freezeStaff, freezeAlready, unfreezeAlready, altsUse, frezzeUse;
    private boolean dbEnabled, dbUseSSL;
    private String dbHost, dbDatabase, dbUser, dbPassword, dbServerId;
    private int dbPort;
    private List<DatabaseEndpoint> dbStatusEndpoints = List.of();
    private boolean reportEnabled, reportRequireOnlineTarget, reportSelfAllowed, reportStorageEnabled,
            reportNotifyEnabled, reportStaffClickEnabled;
    private int reportCooldownSeconds, reportMinReasonLength, reportMaxReasonLength;
    private String reportCooldownBypassPermission, reportNotifyPermission, reportStaffClickCommandTemplate;
    private ClickActionType reportStaffClickActionType;
    private String reportUse, reportDisabled, reportSelf, reportOffline;
    private String reportReasonShort, reportReasonLong, reportCooldown, reportSent, reportStaffNotify;
    private List<String> reportStaffNotifyHover;
    private String invseeUse, invseeSelf, invseeCheck, invseeOfflineLoaded, invseeOfflineUnavailable;
    private String invseeInspectionOnlineTitle, invseeInspectionOfflineTitle, invseeLegacyMainHandNotice;
    private String inspectArmorBootsLabel, inspectArmorLeggingsLabel, inspectArmorChestplateLabel,
            inspectArmorHelmetLabel;
    private String inspectOffhandLabel, inspectMainhandLabel, inspectNoItemSuffix;
    private String inspectStatusTitle;
    private List<String> inspectStatusLore;
    private Material inspectStatusMaterial;
    private int inspectArmorStartSlot, inspectOffhandSlot, inspectMainhandSlot, inspectStatusSlot;
    private String reviveUse, reviveNoDeaths, reviveTargetOffline, reviveRestored;
    private String guiReviveTitle, guiReviveItemName, guiRevivePageInfoName;
    private List<String> guiReviveItemLore, guiRevivePageInfoLore;
    private int reviveMenuSize;
    private int revivePageSize;
    private int inventoryDeathSnapshotMaxAgeMinutes;
    private int inventorySnapshotCleanupIntervalMinutes;
    private boolean freezeBanOnDisconnectEnabled;
    private int freezeBanOnDisconnectDays;
    private String freezeBanOnDisconnectReason;
    private boolean freezeDisplayEnabled;
    private double freezeDisplayDistance;
    private double freezeDisplayHeightOffset;
    private long freezeDisplayUpdateTicks;
    private List<String> freezeDisplayLines;
    private double freezeDisplaySideOffset;
    private boolean freezeDisplayBackgroundEnabled;
    private boolean freezeDisplayTextShadowEnabled;
    private Color freezeDisplayBackgroundColor;
    private String freezeRiskLowLabel, freezeRiskMediumLabel, freezeRiskHighLabel;
    private String freezeRiskLowColor, freezeRiskMediumColor, freezeRiskHighColor;
    private int freezeRiskMediumMinScore, freezeRiskHighMinScore;
    private int freezeRiskLowBars, freezeRiskMediumBars, freezeRiskHighBars, freezeRiskTotalBars;
    private int wallCompassRange;
    private int staffPunishSlot, staffFreezeSlot, staffPlayersSlot, staffRandomTpSlot, staffWallCompassSlot,
            staffInspectSlot, staffVanishSlot;
    private boolean staffModeAllowHit;
    private boolean staffModeAllowContainerItemMove;
    private boolean vanishPersistenceEnabled;
    private boolean clientTrackerEnabled, clientTrackerNotifyEnabled;
    private int clientTrackerTimeoutTicks;
    private String clientTrackerUnknownName, clientTrackerNotifyPermission, clientTrackerJoinMessage;
    private Map<String, List<String>> clientTrackerCustomMappings = Map.of();
    private boolean antiXrayIgnoreCreative, antiXrayNotifyEnabled, antiXrayClickEnabled;
    private int antiXrayRateWindowSeconds, antiXrayMaterialThreshold, antiXrayTotalThreshold,
            antiXraySessionThreshold, antiXrayAlertCooldownSeconds;
    private String antiXrayBypassPermission, antiXrayNotifyPermission, antiXrayAlertMessage,
            antiXrayClickCommandTemplate;
    private ClickActionType antiXrayClickActionType;
    private List<String> antiXrayAlertHover;
    private Set<Material> antiXrayAlertBlocks = Set.of();
    private Map<Material, String> antiXrayDisplayNames = Map.of();
    private Map<String, Boolean> modules = Collections.emptyMap();
    private Set<String> disabledPluginCommands = Set.of();
    private Set<String> staffModeBlacklistedCommands = Set.of();
    private boolean freezeBlockAllCommands;
    private boolean punishmentSectionLimitsEnabled;
    private List<PunishmentLimitGroup> punishmentLimitGroups = List.of();
    private String msgPunishmentLimitExceeded;
    private String silentPunishmentUse, silentInvalid, silentFormatInvalid;
    private String staffModeCommandBlocked, freezeCommandBlocked;

    @SuppressWarnings("unused")
    private static final class PunishmentLimitGroup {
        private final String id;
        private final String permission;
        private final Map<String, Long> limits;

        private PunishmentLimitGroup(String id, String permission, Map<String, Long> limits) {
            this.id = id;
            this.permission = permission;
            this.limits = limits;
        }
    }

    public MainConfigManager(MaxStaff plugin) {
        this.plugin = plugin;
        configFile = new CustomConfig("config.yml", null, plugin, false);
        configFile.registerConfig();
        loadConfig();
    }

    public void loadConfig() {
        FileConfiguration config = configFile.getConfig();

        selectedLanguage = config.getString("general.language", "en");

        String langPath = "messages_" + selectedLanguage + ".yml";
        langFile = new CustomConfig(langPath, "lang", plugin, false);
        langFile.registerConfig();
        FileConfiguration lang = langFile.getConfig();

        prefix = config.getString("general.prefix", "&4&lMaxStaff &8» ");
        updateCheckEnabled = config.getBoolean("general.update-check", true);
        bStatsEnabled = config.getBoolean("general.bstats", true);
        modules = loadModules(config);
        disabledPluginCommands = loadDisabledCommands(config);
        staffModeBlacklistedCommands = loadCommandSet(config, "staff-mode.blacklist-commands");
        freezeBlockAllCommands = config.getBoolean("freeze.block-all-commands", true);
        isBroadcastEnabled = config.getBoolean("punishments.broadcast", true);
        matPunish = loadMaterial(config.getString("staff-mode.items.punish.material"), Material.NETHERITE_HOE);
        matFreeze = loadMaterial(config.getString("staff-mode.items.freeze.material"), Material.PACKED_ICE);
        matPlayers = loadMaterial(config.getString("staff-mode.items.players.material"), Material.CLOCK);
        matRandomTp = loadMaterial(config.getString("staff-mode.items.random-tp.material"), Material.ENDER_PEARL);
        matInspect = loadMaterial(config.getString("staff-mode.items.inspect.material"), Material.CHEST);
        matVanish = loadMaterial(config.getString("staff-mode.items.vanish.material"), Material.NETHER_STAR);
        matWallCompass = loadMaterial(config.getString("staff-mode.items.wall-compass.material"), Material.COMPASS);
        borderMaterial = loadMaterial(config.getString("gui-style.border-material"), Material.BLACK_STAINED_GLASS_PANE);
        navBackMat = loadMaterial(config.getString("gui-style.navigation.back-material"), Material.BOOK);
        navNextMat = loadMaterial(config.getString("gui-style.navigation.next-material"), Material.ARROW);
        navPrevMat = loadMaterial(config.getString("gui-style.navigation.prev-material"), Material.ARROW);
        guiConfirmYesMat = loadMaterial(config.getString("gui-style.confirmation.yes-material"), Material.LIME_WOOL);
        guiConfirmNoMat = loadMaterial(config.getString("gui-style.confirmation.no-material"), Material.RED_WOOL);
        durationDyes[0] = loadMaterial(config.getString("gui-style.unified-menu.duration-1-material"),
                Material.LIME_DYE);
        durationDyes[1] = loadMaterial(config.getString("gui-style.unified-menu.duration-2-material"),
                Material.YELLOW_DYE);
        durationDyes[2] = loadMaterial(config.getString("gui-style.unified-menu.duration-3-material"),
                Material.ORANGE_DYE);
        durationDyes[3] = loadMaterial(config.getString("gui-style.unified-menu.duration-4-material"),
                Material.RED_DYE);
        guiInfoStatsMat = loadMaterial(config.getString("gui.info.items.stats.material"), Material.BOOK);
        guiInfoHistoryMat = loadMaterial(config.getString("gui.info.items.history.material"), Material.PAPER);
        guiInfoActionMat = loadMaterial(config.getString("gui.info.items.action_punish.material"),
                Material.NETHERITE_SWORD);
        guiInfoPermissionsMat = loadMaterial(config.getString("gui.info.items.permissions.material"),
                Material.WRITABLE_BOOK);
        guiSanctionBanMat = loadMaterial(config.getString("gui.sanctions.items.ban.material"), Material.IRON_SWORD);
        guiSanctionMuteMat = loadMaterial(config.getString("gui.sanctions.items.mute.material"), Material.PAPER);
        guiSanctionKickMat = loadMaterial(config.getString("gui.sanctions.items.kick.material"), Material.FEATHER);
        guiHistoryBansMat = loadMaterial(config.getString("gui.history.items.bans.material"), Material.RED_WOOL);
        guiHistoryMutesMat = loadMaterial(config.getString("gui.history.items.mutes.material"), Material.ORANGE_WOOL);
        guiHistoryWarnsMat = loadMaterial(config.getString("gui.history.items.warns.material"), Material.YELLOW_WOOL);
        guiHistoryKicksMat = loadMaterial(config.getString("gui.history.items.kicks.material"),
                Material.LIGHT_GRAY_WOOL);
        guiHistoryBackSlot = Math.max(0, Math.min(26, config.getInt("gui.history.back-slot", 22)));
        guiDetailedRecordMat = loadMaterial(config.getString("gui.history-detailed.record-material"), Material.PAPER);
        guiActiveBanMat = loadMaterial(config.getString("gui.active-sanctions.items.ban.material"), Material.RED_WOOL);
        guiActiveIpBanMat = loadMaterial(config.getString("gui.active-sanctions.items.ip-ban.material"),
                Material.REDSTONE_BLOCK);
        guiActiveMuteMat = loadMaterial(config.getString("gui.active-sanctions.items.mute.material"),
                Material.ORANGE_WOOL);
        guiActiveEmptyMat = loadMaterial(config.getString("gui.active-sanctions.empty.material"), Material.BARRIER);
        guiActivePageInfoMat = loadMaterial(config.getString("gui.active-sanctions.page-info.material"),
                Material.PAPER);
        guiActiveMenuSize = Math.max(27, normalizeInventorySize(config.getInt("gui.active-sanctions.size", 54), 54));
        guiGmSurvivalMat = loadMaterial(config.getString("gui-gamemode.survival-material"), Material.GRASS_BLOCK);
        guiGmCreativeMat = loadMaterial(config.getString("gui-gamemode.creative-material"), Material.BEACON);
        guiGmAdventureMat = loadMaterial(config.getString("gui-gamemode.adventure-material"), Material.MAP);
        guiGmSpectatorMat = loadMaterial(config.getString("gui-gamemode.spectator-material"), Material.ENDER_EYE);
        guiPlayersRandomTpMat = loadMaterial(config.getString("gui.players.random-tp.material"), Material.ENDER_PEARL);
        gmMenuEnabled = config.getBoolean("gui-gamemode.enabled-menu", true);
        cmdSpySensitiveCommands = config.getStringList("command-spy.sensitive-commands");
        if (cmdSpySensitiveCommands == null || cmdSpySensitiveCommands.isEmpty()) {
            cmdSpySensitiveCommands = Arrays.asList("login", "register", "l", "reg", "token");
        }
        cmdSpySensitiveCommands = normalizeCommandEntries(cmdSpySensitiveCommands);
        cmdSpySensitiveCommandAliases = normalizeCommandEntries(
                config.getStringList("command-spy.sensitive-command-aliases"));
        cmdSpySensitiveCommandPatterns = normalizeCommandEntries(
                config.getStringList("command-spy.sensitive-command-patterns"));
        if (cmdSpySensitiveCommandPatterns.isEmpty()) {
            cmdSpySensitiveCommandPatterns = Arrays.asList("authme", "nlogin", "loginsecurity");
        }
        cmdSpySensitiveBypassPermission = config.getString("command-spy.sensitive-bypass-permission",
                "maxstaff.cmdspy.raw");
        cmdSpyMaskedArgument = config.getString("command-spy.masked-argument", "******");
        guiInfoAltsMat = loadMaterial(config.getString("gui.info.items.alts.material"), Material.COMPASS);
        guiInfoInvMat = loadMaterial(config.getString("gui.info.items.inventory.material"), Material.CHEST);
        healmaterialfreeze = loadMaterial(config.getString("staff-mode.items.freeze.helmet-material"),
                Material.PACKED_ICE);

        freezeBanOnDisconnectEnabled = config.getBoolean("staff-mode.items.freeze.ban-on-disconnect.enabled", true);
        freezeBanOnDisconnectDays = Math.max(1, config.getInt("staff-mode.items.freeze.ban-on-disconnect.days", 7));
        freezeBanOnDisconnectReason = config.getString("staff-mode.items.freeze.ban-on-disconnect.reason",
                "Disconnected while frozen");
        freezeDisplayEnabled = config.getBoolean("staff-mode.items.freeze.display.enabled", true);
        freezeDisplayDistance = config.getDouble("staff-mode.items.freeze.display.distance", 1.25D);
        freezeDisplayHeightOffset = config.getDouble("staff-mode.items.freeze.display.height-offset", -0.55D);
        freezeDisplayUpdateTicks = Math.max(1L, config.getLong("staff-mode.items.freeze.display.update-ticks", 10L));
        freezeDisplaySideOffset = config.getDouble("staff-mode.items.freeze.display.side-offset", 0.75D);
        freezeDisplayBackgroundEnabled = config.getBoolean("staff-mode.items.freeze.display.background.enabled", true);
        freezeDisplayTextShadowEnabled = config.getBoolean("staff-mode.items.freeze.display.text-shadow.enabled", true);
        String freezeDisplayBackgroundColorRaw = config.getString("staff-mode.items.freeze.display.background.color",
                "#0F172A");
        int freezeDisplayBackgroundOpacity = Math.max(0, Math.min(255,
                config.getInt("staff-mode.items.freeze.display.background.opacity", 180)));
        freezeDisplayBackgroundColor = parseColorOrDefault(freezeDisplayBackgroundColorRaw,
                freezeDisplayBackgroundOpacity,
                Color.fromARGB(180, 15, 23, 42));
        freezeRiskLowLabel = config.getString("staff-mode.items.freeze.display.risk.levels.low.label", "LOW");
        freezeRiskMediumLabel = config.getString("staff-mode.items.freeze.display.risk.levels.medium.label", "MEDIUM");
        freezeRiskHighLabel = config.getString("staff-mode.items.freeze.display.risk.levels.high.label", "HIGH");
        freezeRiskLowColor = config.getString("staff-mode.items.freeze.display.risk.levels.low.color", "&a");
        freezeRiskMediumColor = config.getString("staff-mode.items.freeze.display.risk.levels.medium.color", "&6");
        freezeRiskHighColor = config.getString("staff-mode.items.freeze.display.risk.levels.high.color", "&c");
        freezeRiskMediumMinScore = Math.max(0,
                config.getInt("staff-mode.items.freeze.display.risk.thresholds.medium-min-score", 5));
        freezeRiskHighMinScore = Math.max(freezeRiskMediumMinScore,
                config.getInt("staff-mode.items.freeze.display.risk.thresholds.high-min-score", 12));
        freezeRiskTotalBars = Math.max(1, config.getInt("staff-mode.items.freeze.display.risk.bar.total-bars", 5));
        freezeRiskLowBars = Math.max(0,
                Math.min(freezeRiskTotalBars, config.getInt("staff-mode.items.freeze.display.risk.bar.low-bars", 1)));
        freezeRiskMediumBars = Math.max(0, Math.min(freezeRiskTotalBars,
                config.getInt("staff-mode.items.freeze.display.risk.bar.medium-bars", 3)));
        freezeRiskHighBars = Math.max(0,
                Math.min(freezeRiskTotalBars, config.getInt("staff-mode.items.freeze.display.risk.bar.high-bars", 5)));
        freezeDisplayLines = config.getStringList("staff-mode.items.freeze.display.lines");
        if (freezeDisplayLines == null || freezeDisplayLines.isEmpty()) {
            freezeDisplayLines = Arrays.asList(
                    "&#FF3405&l❖ SANCTION PROFILE",
                    "&8&m---------------------------",
                    "&fPlayer: &b{name}",
                    "&fHistory: {risk_bar} &8(&7{total}&8)",
                    "&fRisk: {risk_color}&l{risk_label}",
                    "&cBans: &f{bans}  &6Mutes: &f{mutes}  &eKicks: &f{kicks}",
                    "&8&m---------------------------");
        }
        wallCompassRange = Math.max(5, config.getInt("staff-mode.items.wall-compass.range", 64));
        staffPunishSlot = config.getInt("staff-mode.items.punish.slot", 0);
        staffFreezeSlot = config.getInt("staff-mode.items.freeze.slot", 1);
        staffPlayersSlot = config.getInt("staff-mode.items.players.slot", 4);
        staffRandomTpSlot = config.getInt("staff-mode.items.random-tp.slot", 5);
        staffWallCompassSlot = config.getInt("staff-mode.items.wall-compass.slot", 6);
        staffInspectSlot = config.getInt("staff-mode.items.inspect.slot", 7);
        staffVanishSlot = config.getInt("staff-mode.items.vanish.slot", 8);
        staffModeAllowHit = config.getBoolean("staff-mode.combat.allow-hit", false);
        staffModeAllowContainerItemMove = config.getBoolean("staff-mode.containers.allow-item-move", false);
        vanishPersistenceEnabled = config.getBoolean("staff-mode.vanish.persist-across-servers", true);
        warnThresholds = config.getConfigurationSection("punishments.broadcasts.warns.thresholds");
        dbEnabled = config.getBoolean("database.enabled", false);
        dbHost = config.getString("database.host", "localhost");
        dbPort = config.getInt("database.port", 3306);
        dbDatabase = config.getString("database.database", "maxstaff_db");
        dbUser = config.getString("database.username", "root");
        dbPassword = config.getString("database.password", "");
        dbUseSSL = config.getBoolean("database.use-ssl", false);
        dbServerId = config.getString("database.server-id", "server-" + plugin.getServer().getPort());
        if (dbServerId == null || dbServerId.isBlank()) {
            dbServerId = "server-" + plugin.getServer().getPort();
        }
        dbStatusEndpoints = loadDbStatusEndpoints(config);
        reportEnabled = config.getBoolean("reports.enabled", true);
        reportCooldownSeconds = config.getInt("reports.cooldown-seconds", 60);
        reportCooldownBypassPermission = config.getString("reports.cooldown-bypass-permission",
                "maxstaff.report.bypass");
        reportRequireOnlineTarget = config.getBoolean("reports.require-online-target", true);
        reportSelfAllowed = config.getBoolean("reports.allow-self-report", false);
        reportMinReasonLength = config.getInt("reports.min-reason-length", 5);
        reportMaxReasonLength = config.getInt("reports.max-reason-length", 120);
        reportStorageEnabled = config.getBoolean("reports.store-reports", true);
        reportNotifyEnabled = config.getBoolean("reports.notify.enabled", true);
        reportNotifyPermission = config.getString("reports.notify.permission", "maxstaff.report.notify");
        reportStaffClickEnabled = config.getBoolean("reports.notify.click-action.enabled", true);
        reportStaffClickActionType = ClickActionType.fromConfig(
                config.getString("reports.notify.click-action.type", "SUGGEST_COMMAND"));
        reportStaffClickCommandTemplate = config.getString("reports.notify.click-action.command-template",
                "/tp {reporter}");
        clientTrackerEnabled = config.getBoolean("client-tracker.enabled", true);
        clientTrackerNotifyEnabled = config.getBoolean("client-tracker.notify-staff.enabled", true);
        clientTrackerNotifyPermission = config.getString("client-tracker.notify-staff.permission",
                "maxstaff.client.notify");
        clientTrackerJoinMessage = lang.getString("messages.client-detected",
                "&8[&dClient&8] &f{player} &7usa &d{client}");
        clientTrackerUnknownName = config.getString("client-tracker.unknown-client-name", "Unknown");
        clientTrackerTimeoutTicks = Math.max(20, config.getInt("client-tracker.detection-timeout-ticks", 80));
        clientTrackerCustomMappings = loadClientMappings(config);
        antiXrayIgnoreCreative = config.getBoolean("anti-xray.ignore-creative", true);
        antiXrayBypassPermission = config.getString("anti-xray.bypass-permission", "maxstaff.antixray.bypass");
        antiXrayNotifyEnabled = config.getBoolean("anti-xray.notify.enabled", true);
        antiXrayNotifyPermission = config.getString("anti-xray.notify.permission", "maxstaff.antixray.alert");
        antiXrayClickEnabled = config.getBoolean("anti-xray.notify.click-action.enabled", true);
        antiXrayClickActionType = ClickActionType.fromConfig(
                config.getString("anti-xray.notify.click-action.type", "RUN_COMMAND"));
        antiXrayClickCommandTemplate = config.getString("anti-xray.notify.click-action.command-template",
                "/tp {player}");
        antiXrayRateWindowSeconds = Math.max(5, config.getInt("anti-xray.rate.window-seconds", 120));
        antiXrayMaterialThreshold = Math.max(1, config.getInt("anti-xray.rate.material-threshold", 6));
        antiXrayTotalThreshold = Math.max(1, config.getInt("anti-xray.rate.total-threshold", 12));
        antiXraySessionThreshold = Math.max(1, config.getInt("anti-xray.rate.session-threshold", 16));
        antiXrayAlertCooldownSeconds = Math.max(0, config.getInt("anti-xray.notify.cooldown-seconds", 60));
        antiXrayAlertBlocks = loadAntiXrayAlertBlocks(config);
        antiXrayDisplayNames = loadAntiXrayDisplayNames(config);
        loadPunishmentSectionLimits(config);

        // TODO LO QUE TENGA QUE VER CON MENSAJES, ETC
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
        staffModeCommandBlocked = lang.getString("staff-mode.command-blocked",
                "&#FF3333&l✖ &#FF5555You cannot use that command while in staff mode.");
        freezeCommandBlocked = lang.getString("messages.frozen-command-blocked",
                "&#FF3333&l✖ &#FF5555You cannot use commands while frozen.");
        msgInspect = lang.getString("staff-mode.items.inspect.message");
        msgVanishOn = lang.getString("staff-mode.items.vanish.message-on");
        msgVanishOff = lang.getString("staff-mode.items.vanish.message-off");
        msgPunish = lang.getString("staff-mode.items.punish.message");
        msgPlayers = lang.getString("staff-mode.items.players.message");
        msgRandomTp = lang.getString("staff-mode.items.random-tp.message",
                "&aTeleported to random player: &e{player}");
        msgRandomTpNoTargets = lang.getString("staff-mode.items.random-tp.no-targets",
                "&cNo available players to teleport.");
        msgFreezeStaff = lang.getString("staff-mode.items.freeze.message-freeze");
        msgUnfreezeStaff = lang.getString("staff-mode.items.freeze.message-unfreeze");
        msgTargetFrozen = lang.getStringList("staff-mode.items.freeze.target-frozen");
        msgTargetUnfrozen = lang.getString("staff-mode.items.freeze.target-unfrozen");
        itemNamePunish = lang.getString("staff-mode.items.punish.name");
        itemNameFreeze = lang.getString("staff-mode.items.freeze.name");
        itemNamePlayers = lang.getString("staff-mode.items.players.name");
        itemNameRandomTp = lang.getString("staff-mode.items.random-tp.name", "&5&lRandom TP &7(Right Click)");
        itemNameInspect = lang.getString("staff-mode.items.inspect.name");
        itemNameVanish = lang.getString("staff-mode.items.vanish.name");
        itemNameWallCompass = lang.getString("staff-mode.items.wall-compass.name", "&d&lWall Compass &7(Right Click)");
        guiPlayerOffline = lang.getString("gui.player-offline",
                lang.getString("punishments.feedback.player-offline", "&cPlayer is offline"));
        guiPlayersTitle = lang.getString("gui.players.title");
        guiHeadLore = lang.getString("gui.players.head-lore");
        guiPlayersRandomTpName = lang.getString("gui.players.random-tp.name", "&d&lRandom TP");
        guiPlayersRandomTpLore = lang.getStringList("gui.players.random-tp.lore");
        if (guiPlayersRandomTpLore == null || guiPlayersRandomTpLore.isEmpty()) {
            guiPlayersRandomTpLore = Arrays.asList("&7Teleport to a random online player");
        }
        guiXrayTitle = lang.getString("gui.xray.title", "&8Possible Xray Alerts");
        guiXrayHeadName = lang.getString("gui.xray.head.name", "&c&l{player}");
        guiXrayHeadLore = lang.getStringList("gui.xray.head.lore");
        if (guiXrayHeadLore == null || guiXrayHeadLore.isEmpty()) {
            guiXrayHeadLore = Arrays.asList("&7Last ore: &f{mineral}", "&7Rate: &f{rate}", "&aClick to teleport");
        }
        guiXrayEmptyName = lang.getString("gui.xray.empty.name", "&aNo Xray alerts");
        guiXrayEmptyLore = lang.getStringList("gui.xray.empty.lore");
        if (guiXrayEmptyLore == null || guiXrayEmptyLore.isEmpty()) {
            guiXrayEmptyLore = Arrays.asList("&7No players have been flagged yet.");
        }
        guiXrayEmptyMaterial = loadMaterial(lang.getString("gui.xray.empty.material"), Material.BARRIER);
        guiXrayTeleportMessage = lang.getString("gui.xray.teleport-success", "&aTeleported to &e{player}&a.");
        guiXrayTargetOfflineMessage = lang.getString("gui.xray.target-offline", "&cThat suspect is offline.");
        antiXrayMenuDisabledMessage = lang.getString("messages.anti-xray-disabled", "&cAnti Xray is disabled.");
        guiSanctionsTitle = getConfiguredString(config, lang, GUI_SANCTIONS_TITLE_PATH, DEFAULT_LANG_RESOURCE);
        guiItemBanName = lang.getString("gui.sanctions.items.ban.name", "&c&lBAN");
        guiItemBanLore = lang.getStringList("gui.sanctions.items.ban.lore");
        if (guiItemBanLore == null || guiItemBanLore.isEmpty()) {
            guiItemBanLore = Arrays.asList("&7Click to select reason and duration");
        }
        guiItemMuteName = lang.getString("gui.sanctions.items.mute.name", "&e&lMUTE");
        guiItemMuteLore = lang.getStringList("gui.sanctions.items.mute.lore");
        if (guiItemMuteLore == null || guiItemMuteLore.isEmpty()) {
            guiItemMuteLore = Arrays.asList("&7Click to select reason and duration");
        }
        guiItemKickName = lang.getString("gui.sanctions.items.kick.name", "&b&lKICK");
        guiItemKickLore = lang.getStringList("gui.sanctions.items.kick.lore");
        if (guiItemKickLore == null || guiItemKickLore.isEmpty()) {
            guiItemKickLore = Arrays.asList("&7Click to kick now");
        }
        guiInfoTitle = lang.getString("gui.info.title", "&8Information: &0{target}");
        guiInfoStatsName = lang.getString("gui.info.items.stats.name");
        guiInfoStatsLore = lang.getStringList("gui.info.items.stats.lore");
        guiInfoHistoryName = lang.getString("gui.info.items.history.name");
        guiInfoHistoryLore = lang.getStringList("gui.info.items.history.lore");
        guiInfoActionName = lang.getString("gui.info.items.action_punish.name");
        guiInfoActionLore = lang.getStringList("gui.info.items.action_punish.lore");
        guiConfirmTitle = lang.getString("gui.confirm.title", "&8Confirm action");
        guiConfirmYesName = lang.getString("gui.confirm.items.yes.name", "&aConfirm");
        guiConfirmYesLore = lang.getStringList("gui.confirm.items.yes.lore");
        if (guiConfirmYesLore == null || guiConfirmYesLore.isEmpty()) {
            guiConfirmYesLore = Arrays.asList("&7Target: &f{target}", "&7Reason: &f{reason}",
                    "&7Duration: &f{duration}");
        }
        guiConfirmNoName = lang.getString("gui.confirm.items.no.name", "&cCancel");

        guiConfirmNoLore = lang.getStringList("gui.confirm.items.no.lore");
        if (guiConfirmNoLore == null || guiConfirmNoLore.isEmpty()) {
            guiConfirmNoLore = Arrays.asList("&7Target: &f{target}", "&7Reason: &f{reason}",
                    "&7Duration: &f{duration}", "", "&7Return to previous menu");
        }

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
        msgPunishProtected = lang.getString("punishments.feedback.target-protected",
                "&cYou cannot sanction &e{target}&c: protected player.");
        msgUnbanSuccess = lang.getString("punishments.feedback.unban-success");
        msgUnmuteSuccess = lang.getString("punishments.feedback.unmute-success");
        msgUsage = lang.getString("punishments.feedback.usage");
        msgPunishmentLimitExceeded = lang.getString("punishments.feedback.limit-exceeded",
                "&cYou cannot apply &e{type}&c for &e{duration}&c. Your maximum allowed is &e{max}&c.");
        usestaffreset = lang.getString("messages.use-staffreset");
        usestafftake = lang.getString("messages.use-stafftake");
        msgResetSuccess = lang.getString("messages.staff-reset-success");
        msgTakeSuccess = lang.getString("messages.staff-take-success");
        takeNumberInvalid = lang.getString("messages.take-number-invalid");
        playerNoHistory = lang.getString("messages.player-no-history");
        msgUpdateAvailable = lang.getString("messages.update-available",
                "&eA new version is available! (&b{version}&e)");
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
        guiHistoryBackLore = lang.getStringList("gui.history.back-lore");
        if (guiHistoryBackLore.isEmpty()) {
            guiHistoryBackLore = List.of("&7Volver a la información del jugador");
        }
        guiDetailedTitle = lang.getString("gui.history-detailed.title");
        guiDetailedItemName = lang.getString("gui.history-detailed.item-name");
        guiDetailedBackLore = lang.getStringList("gui.history-detailed.back-lore");
        guiDetailedDate = lang.getString("gui.history-detailed.item-lore.date");
        guiDetailedStaff = lang.getString("gui.history-detailed.item-lore.staff");
        guiDetailedReason = lang.getString("gui.history-detailed.item-lore.reason");
        guiDetailedDuration = lang.getString("gui.history-detailed.item-lore.duration");
        guiActiveTitle = lang.getString("gui.active-sanctions.title",
                "&8Active sanctions ({page}/{total})");
        guiActiveItemName = lang.getString("gui.active-sanctions.item-name", "&c&l{type} &8- &f{target}");
        guiActiveItemLore = lang.getStringList("gui.active-sanctions.item-lore");
        if (guiActiveItemLore == null || guiActiveItemLore.isEmpty()) {
            guiActiveItemLore = Arrays.asList("&7Staff: &f{staff}", "&7Reason: &f{reason}",
                    "&7Duration: &f{duration}", "&7Expires in: &f{remaining}");
        }
        guiActiveEmptyName = lang.getString("gui.active-sanctions.empty.name", "&aNo active sanctions");
        guiActiveEmptyLore = lang.getStringList("gui.active-sanctions.empty.lore");
        if (guiActiveEmptyLore == null || guiActiveEmptyLore.isEmpty()) {
            guiActiveEmptyLore = Arrays.asList("&7There are no active bans or mutes.");
        }
        guiActivePageInfoName = lang.getString("gui.active-sanctions.page-info.name",
                "&ePage &f{page}&7/&f{total}");
        guiActivePageInfoLore = lang.getStringList("gui.active-sanctions.page-info.lore");
        if (guiActivePageInfoLore == null || guiActivePageInfoLore.isEmpty()) {
            guiActivePageInfoLore = List.of("&7Active sanctions: &f{total-records}");
        }
        msgNoIPFound = lang.getString("punishments.feedback.no-ip-found", "&cNo IP found for this player.");
        msgUnbanIPSuccess = lang.getString("punishments.feedback.unban-ip-success", "&aIP &e{ip} &ahas been unbanned.");
        msgInvalidIP = lang.getString("punishments.feedback.invalid-ip", "&cCould not find a valid IP for: &e{target}");
        bcBanIP = lang.getString("punishments.broadcasts.ban-ip",
                "&c&lIP-BAN &8» &f{target} &7was IP banned by &c{staff} &7({duration}).");
        staffChatFormat = lang.getString("messages.staff-chat-format", "&8[&4&lSTAFF&8] &b{player}&8: &f{message}");
        msgCmdSpyEnabled = lang.getString("messages.command-spy-enabled", "&aModo Espía de Comandos ACTIVADO.");
        msgCmdSpyDisabled = lang.getString("messages.command-spy-disabled", "&cModo Espía de Comandos DESACTIVADO.");
        msgCmdSpyFormat = lang.getString("messages.command-spy-format", "&8[&6Spy&8] &e{player}&8: &f{command}");
        msgGlobalMuteEnabled = lang.getString("chat.global-mute-enabled",
                "&cEl chat global ha sido &lSILENCIADO &cpor &e{player}&c.");
        msgGlobalMuteDisabled = lang.getString("chat.global-mute-disabled",
                "&aEl chat global ha sido &lACTIVADO &apor &e{player}&a.");
        msgChatIsMuted = lang.getString("chat.chat-is-muted", "&cEl chat está silenciado globalmente en este momento.");
        msgChatCleared = lang.getString("chat.cleared", "&6&lCHAT LIMPIADO &7por &e{player}&7.");
        msgChatUsage = lang.getString("chat.usage", "&cUso: /chat <mute|clear>");
        guiGmTitle = lang.getString("gui.gamemode.title", "&8Selector de Modo de Juego");
        guiGmFeedback = lang.getString("gui.feedback.gamemode-changed",
                "&aTu modo de juego ha sido cambiado a &e{mode}&a.");
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
        flyUse = lang.getString("messages.fly-use", "&cUsage: /fly <1|2|3>");
        flyDisabled = lang.getString("messages.fly-disabled", "&cFly disabled.");
        flyEnabledDefault = lang.getString("messages.fly-enabled-default",
                "&aFly enabled &7(level 1). Use &f/fly <1|2|3> &7to change speed.");
        flyEnabledLevel = lang.getString("messages.fly-enabled-level",
                "&aFly enabled at level &e{level}&a. Speed: &e{speed}");
        flySpeedLevel1 = readFlySpeed(config, "fly.level-1-speed", 0.1F);
        flySpeedLevel2 = readFlySpeed(config, "fly.level-2-speed", 0.2F);
        flySpeedLevel3 = readFlySpeed(config, "fly.level-3-speed", 0.3F);
        chatStaffUse = lang.getString("messages.chatstaff-use",
                "&c&l(!) &cWrite a message for the staff. Usage: /sc <message>");
        staffChatToggleOn = lang.getString("messages.chatstaff-toggle-on",
                "&aStaff chat mode enabled. Your messages will now be sent to staff chat.");
        staffChatToggleOff = lang.getString("messages.chatstaff-toggle-off", "&cStaff chat mode disabled.");
        placeholderTrue = lang.getString("placeholders.status-true", "&aYes");
        placeholderFalse = lang.getString("placeholders.status-false", "&cNo");
        placeholderPlaytimeFormat = lang.getString("placeholders.playtime-format", "{hours}h {minutes}m");
        restoredinventory = lang.getString("messages.restore-inventory",
                "&6&l[!] &eYour inventory has been automatically restored after the restart.");
        guiAltsTitle = lang.getString("gui.alts.title", "&8Cuentas de: &0{target}");
        guiAltsDynamic = lang.getString("gui.alts.dynamic", "Dinámico");
        guiAltsStatusOnline = lang.getString("gui.alts.status-online", "&aEn línea");
        guiAltsStatusOffline = lang.getString("gui.alts.status-offline", "&7Desconectado");
        guiAltsStatusBanned = lang.getString("gui.alts.status-banned", "&c&lBANEADO");
        guiAltsLore = lang.getStringList("gui.alts.head-lore");
        silentPunishmentUse = lang.getString("messages.silent-punishment-use",
                "&cUse: &e/silent <ban|tempban|mute|tempmute|kick|warn|ban-ip|tempban-ip> <jugador/ip> [tiempo] [razón]");
        silentInvalid = lang.getString("messages.silent-invalid",
                "&cInvalid action. Use: &eban, tempban, mute, tempmute, kick, warn, ban-ip, tempban-ip");
        silentFormatInvalid = lang.getString("messages.silent-format-invalid",
                "&cInvalid duration format. Use: &e<number><s|m|h|d|w> &cor &eperm/permanent/permanentemente");
        commandsanctionuse = lang.getString("messages.command-sanction-use", "&cUsage: /sanction <player>");
        commandsanctionself = lang.getString("messages.command-sanction-self", "&cYou cannot sanction yourself.");
        guiInfoHeadName = lang.getString("gui.info.items.head.name", "&6&lVisualizando a: &e{target}");
        guiInfoHeadLore = lang.getStringList("gui.info.items.head.lore");
        guiInfoAltsName = lang.getString("gui.info.items.alts.name", "&b&lCuentas Relacionadas");
        guiInfoAltsLore = lang.getStringList("gui.info.items.alts.lore");
        guiInfoInvName = lang.getString("gui.info.items.inventory.name", "&6&lVer Inventario Real");
        guiInfoInvLore = lang.getStringList("gui.info.items.inventory.lore");
        guiInfoPermissionsName = lang.getString("gui.info.items.permissions.name", "&d&lPermisos");
        guiInfoPermissionsLore = lang.getStringList("gui.info.items.permissions.lore");
        guiPermissionsTitle = lang.getString("gui.permissions.title", "&8Permisos: &0{target} ({page}/{total})");
        guiPermissionsItemName = lang.getString("gui.permissions.permission-item-name", "&f{permission}");
        guiPermissionsItemLore = lang.getString("gui.permissions.permission-item-lore",
                "&7Click izquierdo para copiar visualmente");
        guiPermissionsEmptyName = lang.getString("gui.permissions.empty-name", "&cSin permisos");
        guiPermissionsEmptyLore = lang.getStringList("gui.permissions.empty-lore");
        statusOnline = lang.getString("gui.info.status.online", "&aOnline");
        statusOffline = lang.getString("gui.info.status.offline", "&cOffline");
        freezeStaff = lang.getString("messages.freeze-staff", "&cYou cannot freeze another staff member.");
        freezeAlready = lang.getString("messages.freeze-already", "&cThis player is already frozen.");
        unfreezeAlready = lang.getString("messages.unfreeze-already", "&cThis player is not frozen.");
        altsUse = lang.getString("messages.alts-use", "&cUse: /alts <player>");
        frezzeUse = lang.getString("messages.freeze-use", "&cUse: /freeze <player>");
        reportUse = lang.getString("messages.report-use", "&cUse: /report <player> <reason>");
        reportDisabled = lang.getString("messages.report-disabled", "&cReports are disabled.");
        reportSelf = lang.getString("messages.report-self", "&cYou cannot report yourself.");
        reportOffline = lang.getString("messages.report-offline", "&cThat player is not online.");
        reportReasonShort = lang.getString("messages.report-reason-short",
                "&cThe reason must be at least {min} characters.");
        reportReasonLong = lang.getString("messages.report-reason-long",
                "&cThe reason cannot exceed {max} characters.");
        reportCooldown = lang.getString("messages.report-cooldown", "&cYou must wait {seconds}s to report again.");
        reportSent = lang.getString("messages.report-sent", "&aYour report was sent. Thank you!");
        reportStaffNotify = lang.getString("messages.report-staff-notify",
                "&8[&cReport&8] &f#{sequence} &7{reporter} &8» &e{target} &7| &f{reason}");
        reportStaffNotifyHover = lang.getStringList("messages.report-staff-notify-hover");
        if (reportStaffNotifyHover == null || reportStaffNotifyHover.isEmpty()) {
            reportStaffNotifyHover = Arrays.asList("&eClick para preparar comando de atención",
                    "&7Reporte: &f#{sequence}",
                    "&7Reportante: &f{reporter}", "&7Reportado: &f{target}", "&7Ubicación: &f{world} ({x}, {y}, {z})");
        }
        antiXrayAlertMessage = lang.getString("messages.anti-xray-alert",
                "{prefix}&f{player} &7mined &e{mineral} &7in &f{world} &8- &7rate: &e{rate}");
        antiXrayAlertHover = lang.getStringList("messages.anti-xray-alert-hover");
        if (antiXrayAlertHover == null || antiXrayAlertHover.isEmpty()) {
            antiXrayAlertHover = Arrays.asList("&cAntiXray Alert", "&7Player: &f{player}",
                    "&7Mineral: &f{mineral}", "&7World: &f{world}", "&7Rate: &f{rate}",
                    "&aClick to prepare teleport");
        }
        invseeUse = lang.getString("messages.invsee-use", "&cUsage: /invsee <player>");
        invseeSelf = lang.getString("messages.invsee-self", "&cYou cannot view your own inventory.");
        invseeCheck = lang.getString("messages.invsee-check", "&cOpening the player's inventory...");
        invseeOfflineLoaded = lang.getString("messages.invsee-offline-loaded",
                "&eOpening last saved inventory of &6{player}&e (saved: &7{date}&e).");
        invseeOfflineUnavailable = lang.getString("messages.invsee-offline-unavailable",
                "&cNo saved inventory was found for that offline player.");
        invseeInspectionOnlineTitle = lang.getString("messages.invsee-inspection-online-title",
                "&8Inspección de &f{player}");
        invseeInspectionOfflineTitle = lang.getString("messages.invsee-inspection-offline-title",
                "&8Invsee Offline: &f{player}");
        invseeLegacyMainHandNotice = lang.getString("messages.invsee-legacy-mainhand-warning",
                "&eNota: este snapshot es antiguo y no guardaba la mano principal. "
                        + "Por eso puede aparecer como &7'Sin objeto'&e aunque el jugador sí la tuviera.");
        inspectArmorBootsLabel = lang.getString("messages.inspect-labels.armor-boots", "&bBoots");
        inspectArmorLeggingsLabel = lang.getString("messages.inspect-labels.armor-leggings", "&bLeggings");
        inspectArmorChestplateLabel = lang.getString("messages.inspect-labels.armor-chestplate", "&bChestplate");
        inspectArmorHelmetLabel = lang.getString("messages.inspect-labels.armor-helmet", "&bHelmet");
        inspectOffhandLabel = lang.getString("messages.inspect-labels.offhand", "&6Offhand");
        inspectMainhandLabel = lang.getString("messages.inspect-labels.mainhand", "&6Mainhand");
        inspectNoItemSuffix = lang.getString("messages.inspect-labels.no-item-suffix", "&8(No item)");
        inspectStatusTitle = lang.getString("messages.inspect-status.title", "&bInformation of {player}");
        inspectStatusLore = lang.getStringList("messages.inspect-status.lore");
        if (inspectStatusLore == null || inspectStatusLore.isEmpty()) {
            inspectStatusLore = Arrays.asList(
                    "&7Health: &f{health}",
                    "&7Food: &f{food}",
                    "&7Level: &f{level}",
                    "&7Gamemode: &f{gamemode}",
                    "&7World: &f{world}",
                    "&7Coordinates: &f{x}, {y}, {z}");
        }
        inspectStatusMaterial = loadMaterial(config.getString("invsee-inspection.status-item.material"),
                Material.KNOWLEDGE_BOOK);
        inspectArmorStartSlot = config.getInt("invsee-inspection.layout.armor-start-slot", 36);
        inspectOffhandSlot = config.getInt("invsee-inspection.layout.offhand-slot", 40);
        inspectMainhandSlot = config.getInt("invsee-inspection.layout.mainhand-slot", 41);
        inspectStatusSlot = config.getInt("invsee-inspection.layout.status-slot", 44);
        reviveUse = lang.getString("messages.revive-use", "&cUsage: /revive");
        reviveNoDeaths = lang.getString("messages.revive-no-deaths", "&cThere are no recent deaths to restore.");
        reviveTargetOffline = lang.getString("messages.revive-target-offline",
                "&c{player} is offline. They must be online to restore their inventory.");
        reviveRestored = lang.getString("messages.revive-restored",
                "&aRestored inventory to &e{player}&a. Cause: &f{cause}");

        guiReviveTitle = lang.getString("gui.revive.title", "&8Revive Menu");
        guiReviveItemName = lang.getString("gui.revive.item-name", "&a{player}");
        guiReviveItemLore = lang.getStringList("gui.revive.item-lore");
        if (guiReviveItemLore == null || guiReviveItemLore.isEmpty()) {
            guiReviveItemLore = Arrays.asList("&7Cause: &f{death-cause}", "&7Date: &f{date}", "",
                    "&eClick to restore inventory");
        }
        guiRevivePageInfoName = lang.getString("gui.revive.page-info-name", "&ePage &f{page}&7/&f{total}");
        guiRevivePageInfoLore = lang.getStringList("gui.revive.page-info-lore");
        if (guiRevivePageInfoLore == null || guiRevivePageInfoLore.isEmpty()) {
            guiRevivePageInfoLore = List.of("&7Snapshots on this page: &f{snapshots-on-page}");
        }

        reviveMenuSize = config.getInt("revive.menu-size", 54);
        if (reviveMenuSize < 9) {
            reviveMenuSize = 9;
        } else if (reviveMenuSize > 54) {
            reviveMenuSize = 54;
        }
        if (reviveMenuSize % 9 != 0) {
            reviveMenuSize = ((reviveMenuSize / 9) + 1) * 9;
            if (reviveMenuSize > 54) {
                reviveMenuSize = 54;
            }
        }

        int reviveMaxPageSize = Math.max(1, reviveMenuSize - 3);
        revivePageSize = Math.max(1, config.getInt("revive.page-size", reviveMaxPageSize));
        if (revivePageSize > reviveMaxPageSize) {
            revivePageSize = reviveMaxPageSize;
        }

        inventoryDeathSnapshotMaxAgeMinutes = Math.max(1,
                config.getInt("inventory-snapshots.death.max-age-minutes", 60));
        inventorySnapshotCleanupIntervalMinutes = Math.max(1,
                config.getInt("inventory-snapshots.death.cleanup-interval-minutes", 5));

    }

    private Material loadMaterial(String materialName, Material defaultMat) {
        if (materialName == null)
            return defaultMat;
        Material matched = Material.matchMaterial(materialName);
        if (matched == null) {
            plugin.getServer().getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix
                    + msgInvalidMaterial.replace("{path}", materialName).replace("{default}", defaultMat.name())));
            return defaultMat;
        }
        return matched;
    }

    private int normalizeInventorySize(int configuredSize, int fallbackSize) {
        int size = configuredSize <= 0 ? fallbackSize : configuredSize;
        if (size < 9) {
            size = 9;
        } else if (size > 54) {
            size = 54;
        }

        if (size % 9 != 0) {
            size = ((size / 9) + 1) * 9;
            if (size > 54) {
                size = 54;
            }
        }
        return size;
    }

    private String getConfiguredString(FileConfiguration config, FileConfiguration lang, String path, String fallbackResourcePath) {
        String configuredValue = config.getString(path);
        if (hasText(configuredValue)) {
            return configuredValue;
        }

        String localizedValue = lang.getString(path);
        if (hasText(localizedValue)) {
            return localizedValue;
        }

        return getBundledString(fallbackResourcePath, path);
    }

    private String getBundledString(String resourcePath, String path) {
        if (resourcePath == null) {
            return "";
        }
        try (InputStream resourceStream = plugin.getResource(Objects.requireNonNull(resourcePath))) {
            if (resourceStream == null) {
                return "";
            }

            YamlConfiguration bundledConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
            String bundledValue = bundledConfig.getString(path);
            return hasText(bundledValue) ? bundledValue : "";
        } catch (IOException e) {
            return "";
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public void reloadConfig() {
        configFile.reloadConfig();
        if (langFile != null)
            langFile.reloadConfig();
        loadConfig();
    }

    public String getPrefix() {
        return prefix;
    }

    public String getNoPermission() {
        return noPermission;
    }

    public String getPluginReload() {
        return pluginReload;
    }

    public String getSubcommandInvalid() {
        return subcommandInvalid;
    }

    public String getMsgTeleport() {
        return msgTeleport;
    }

    public String getHelpTitle() {
        return helpTitle;
    }

    public List<String> getHelpLines() {
        return helpLines;
    }

    public String getStaffModeEnabled() {
        return staffModeEnabled;
    }

    public String getStaffModeDisabled() {
        return staffModeDisabled;
    }

    public String getSubCommand() {
        return subcommandSpecified;
    }

    public String getInventorySaved() {
        return inventorySaved;
    }

    public String getInventoryRestored() {
        return inventoryRestored;
    }

    public String getCannotDrop() {
        return cannotDrop;
    }

    public String getCannotPlace() {
        return cannotPlace;
    }

    public String getStaffModeCommandBlocked() {
        return staffModeCommandBlocked;
    }

    public String getFreezeCommandBlocked() {
        return freezeCommandBlocked;
    }

    public String getMsgInspect() {
        return msgInspect;
    }

    public String getMsgVanishOn() {
        return msgVanishOn;
    }

    public String getMsgVanishOff() {
        return msgVanishOff;
    }

    public String getMsgPunish() {
        return msgPunish;
    }

    public String getMsgPlayers() {
        return msgPlayers;
    }

    public String getMsgRandomTp() {
        return msgRandomTp;
    }

    public String getMsgRandomTpNoTargets() {
        return msgRandomTpNoTargets;
    }

    public String getMsgFreezeStaff() {
        return msgFreezeStaff;
    }

    public String getMsgUnfreezeStaff() {
        return msgUnfreezeStaff;
    }

    public List<String> getMsgTargetFrozen() {
        return msgTargetFrozen;
    }

    public String getMsgTargetUnfrozen() {
        return msgTargetUnfrozen;
    }

    public String getGuiPlayersTitle() {
        return guiPlayersTitle;
    }

    public String getGuiPlayerOffline() {
        return guiPlayerOffline;
    }

    public String getGuiHeadLore() {
        return guiHeadLore;
    }

    public Material getGuiPlayersRandomTpMat() {
        return guiPlayersRandomTpMat;
    }

    public String getGuiPlayersRandomTpName() {
        return guiPlayersRandomTpName;
    }

    public List<String> getGuiPlayersRandomTpLore() {
        return guiPlayersRandomTpLore;
    }

    public String getGuiXrayTitle() {
        return guiXrayTitle;
    }

    public String getGuiXrayHeadName() {
        return guiXrayHeadName;
    }

    public List<String> getGuiXrayHeadLore() {
        return guiXrayHeadLore;
    }

    public Material getGuiXrayEmptyMaterial() {
        return guiXrayEmptyMaterial;
    }

    public String getGuiXrayEmptyName() {
        return guiXrayEmptyName;
    }

    public List<String> getGuiXrayEmptyLore() {
        return guiXrayEmptyLore;
    }

    public String getGuiXrayTeleportMessage() {
        return guiXrayTeleportMessage;
    }

    public String getGuiXrayTargetOfflineMessage() {
        return guiXrayTargetOfflineMessage;
    }

    public String getAntiXrayMenuDisabledMessage() {
        return antiXrayMenuDisabledMessage;
    }

    public String getGuiSanctionsTitle() {
        if (hasText(guiSanctionsTitle)) {
            return guiSanctionsTitle;
        }
        return getBundledString(DEFAULT_LANG_RESOURCE, GUI_SANCTIONS_TITLE_PATH);
    }

    public String getGuiItemBanName() {
        return guiItemBanName;
    }

    public List<String> getGuiItemBanLore() {
        return guiItemBanLore;
    }

    public String getGuiItemMuteName() {
        return guiItemMuteName;
    }

    public List<String> getGuiItemMuteLore() {
        return guiItemMuteLore;
    }

    public String getGuiItemKickName() {
        return guiItemKickName;
    }

    public List<String> getGuiItemKickLore() {
        return guiItemKickLore;
    }

    public String getGuiReasonsTitle() {
        return guiReasonsTitle;
    }

    public String getGuiReasonsItemName() {
        return guiReasonsItemName;
    }

    public List<String> getGuiReasonsItemLore() {
        return guiReasonsItemLore;
    }

    public String getGuiReasonsDyeName() {
        return guiReasonsDyeName;
    }

    public List<String> getGuiReasonsDyeLore() {
        return guiReasonsDyeLore;
    }

    public String getGuiNavLoreBack() {
        return guiNavLoreBack;
    }

    public String getGuiNavLorePage() {
        return guiNavLorePage;
    }

    public String getTimeUnitPermanent() {
        return timeUnitPermanent;
    }

    public String getTimeUnitDays() {
        return timeUnitDays;
    }

    public String getTimeUnitHours() {
        return timeUnitHours;
    }

    public String getTimeUnitMinutes() {
        return timeUnitMinutes;
    }

    public String getTimeUnitSeconds() {
        return timeUnitSeconds;
    }

    public String getBcBan() {
        return bcBan;
    }

    public String getBcMute() {
        return bcMute;
    }

    public String getBcKick() {
        return bcKick;
    }

    public String getScreenBan() {
        return screenBan;
    }

    public String getScreenKick() {
        return screenKick;
    }

    public String getScreenMute() {
        return screenMute;
    }

    public String getScreenUnmute() {
        return screenUnmute;
    }

    public String getMsgMutedChat() {
        return msgMutedChat;
    }

    public String getMsgOffline() {
        return msgOffline;
    }

    public String getMsgNotMuted() {
        return msgNotMuted;
    }

    public String getMsgPunishProtected() {
        return msgPunishProtected;
    }

    public String getMsgUnbanSuccess() {
        return msgUnbanSuccess;
    }

    public String getMsgUnmuteSuccess() {
        return msgUnmuteSuccess;
    }

    public String getMsgUsage() {
        return msgUsage;
    }

    public boolean isBroadcastEnabled() {
        return isBroadcastEnabled;
    }

    public String getNoReason() {
        return noReason;
    }

    public String getPlayerMuted() {
        return playerMuted;
    }

    public String getMsgConsole() {
        return msgConsole;
    }

    public Material getMatPunish() {
        return matPunish;
    }

    public Material getMatFreeze() {
        return matFreeze;
    }

    public Material getMatPlayers() {
        return matPlayers;
    }

    public Material getMatInspect() {
        return matInspect;
    }

    public Material getMatVanish() {
        return matVanish;
    }

    public String getItemNamePunish() {
        return itemNamePunish;
    }

    public String getItemNameFreeze() {
        return itemNameFreeze;
    }

    public String getItemNamePlayers() {
        return itemNamePlayers;
    }

    public String getItemNameInspect() {
        return itemNameInspect;
    }

    public String getItemNameVanish() {
        return itemNameVanish;
    }

    public Material getBorderMaterial() {
        return borderMaterial;
    }

    public String getNavBackName() {
        return navBackName;
    }

    public String getNavNextName() {
        return navNextName;
    }

    public String getNavPrevName() {
        return navPrevName;
    }

    public Material getNavBackMat() {
        return navBackMat;
    }

    public Material getNavNextMat() {
        return navNextMat;
    }

    public Material getNavPrevMat() {
        return navPrevMat;
    }

    public Material getDurationDye(int index) {
        return durationDyes[index];
    }

    public String getPlayerClickPls() {
        return playerClickPls;
    }

    public String getGuiInfoTitle() {
        return guiInfoTitle;
    }

    public Material getGuiInfoStatsMat() {
        return guiInfoStatsMat;
    }

    public String getGuiInfoStatsName() {
        return guiInfoStatsName;
    }

    public List<String> getGuiInfoStatsLore() {
        return guiInfoStatsLore;
    }

    public Material getGuiInfoHistoryMat() {
        return guiInfoHistoryMat;
    }

    public String getGuiInfoHistoryName() {
        return guiInfoHistoryName;
    }

    public List<String> getGuiInfoHistoryLore() {
        return guiInfoHistoryLore;
    }

    public Material getGuiInfoActionMat() {
        return guiInfoActionMat;
    }

    public String getGuiInfoActionName() {
        return guiInfoActionName;
    }

    public List<String> getGuiInfoActionLore() {
        return guiInfoActionLore;
    }

    public boolean isUpdateCheckEnabled() {
        return updateCheckEnabled;
    }

    public boolean isBStatsEnabled() {
        return bStatsEnabled;
    }

    public String getBcWarn() {
        return bcWarn;
    }

    public String getMsgWarnReceived() {
        return msgWarnReceived;
    }

    public ConfigurationSection getWarnThresholds() {
        return warnThresholds;
    }

    public String getUseStaffReset() {
        return usestaffreset;
    }

    public String getUseStaffTake() {
        return usestafftake;
    }

    public String getTakeNumberInvalid() {
        return takeNumberInvalid;
    }

    public String getPlayerNoHistory() {
        return playerNoHistory;
    }

    public String getMsgResetSuccess() {
        return msgResetSuccess;
    }

    public String getMsgTakeSuccess() {
        return msgTakeSuccess;
    }

    public String getMsgUpdateAvailable() {
        return msgUpdateAvailable;
    }

    public String getMsgUpdateCurrent() {
        return msgUpdateCurrent;
    }

    public String getMsgUpdateDownload() {
        return msgUpdateDownload;
    }

    public String getMsgActionBar() {
        return msgActionBar;
    }

    public String getStatusEnabled() {
        return statusEnabled;
    }

    public String getStatusDisabled() {
        return statusDisabled;
    }

    public String getGuiHistoryTitle() {
        return guiHistoryTitle;
    }

    public String getGuiHistoryBansName() {
        return guiHistoryBansName;
    }

    public List<String> getGuiHistoryBansLore() {
        return guiHistoryBansLore;
    }

    public String getGuiHistoryMutesName() {
        return guiHistoryMutesName;
    }

    public List<String> getGuiHistoryMutesLore() {
        return guiHistoryMutesLore;
    }

    public String getGuiHistoryWarnsName() {
        return guiHistoryWarnsName;
    }

    public List<String> getGuiHistoryWarnsLore() {
        return guiHistoryWarnsLore;
    }

    public String getGuiHistoryKicksName() {
        return guiHistoryKicksName;
    }

    public List<String> getGuiHistoryKicksLore() {
        return guiHistoryKicksLore;
    }

    public List<String> getGuiHistoryBackLore() {
        return guiHistoryBackLore;
    }

    public int getGuiHistoryBackSlot() {
        return guiHistoryBackSlot;
    }

    public String getGuiDetailedTitle() {
        return guiDetailedTitle;
    }

    public String getGuiDetailedItemName() {
        return guiDetailedItemName;
    }

    public List<String> getGuiDetailedBackLore() {
        return guiDetailedBackLore;
    }

    public String getGuiDetailedDate() {
        return guiDetailedDate;
    }

    public String getGuiDetailedStaff() {
        return guiDetailedStaff;
    }

    public String getGuiDetailedReason() {
        return guiDetailedReason;
    }

    public String getGuiDetailedDuration() {
        return guiDetailedDuration;
    }

    public String getMsgNoIPFound() {
        return msgNoIPFound;
    }

    public String getMsgUnbanIPSuccess() {
        return msgUnbanIPSuccess;
    }

    public String getMsgInvalidIP() {
        return msgInvalidIP;
    }

    public String getBcBanIP() {
        return bcBanIP;
    }

    public String getStaffChatFormat() {
        return staffChatFormat;
    }

    public String getMsgCmdSpyEnabled() {
        return msgCmdSpyEnabled;
    }

    public String getMsgCmdSpyDisabled() {
        return msgCmdSpyDisabled;
    }

    public String getMsgCmdSpyFormat() {
        return msgCmdSpyFormat;
    }

    public List<String> getCmdSpySensitiveCommands() {
        return cmdSpySensitiveCommands;
    }

    public List<String> getCmdSpySensitiveCommandAliases() {
        return cmdSpySensitiveCommandAliases;
    }

    public List<String> getCmdSpySensitiveCommandPatterns() {
        return cmdSpySensitiveCommandPatterns;
    }

    public String getCmdSpySensitiveBypassPermission() {
        return cmdSpySensitiveBypassPermission;
    }

    public String getCmdSpyMaskedArgument() {
        return cmdSpyMaskedArgument;
    }

    public String getMsgGlobalMuteEnabled() {
        return msgGlobalMuteEnabled;
    }

    public String getMsgGlobalMuteDisabled() {
        return msgGlobalMuteDisabled;
    }

    public String getMsgChatIsMuted() {
        return msgChatIsMuted;
    }

    public String getMsgChatCleared() {
        return msgChatCleared;
    }

    public String getMsgChatUsage() {
        return msgChatUsage;
    }

    public String getGuiGmTitle() {
        return guiGmTitle;
    }

    public String getGuiGmFeedback() {
        return guiGmFeedback;
    }

    public String getGuiGmSurvivalName() {
        return guiGmSurvivalName;
    }

    public List<String> getGuiGmSurvivalLore() {
        return guiGmSurvivalLore;
    }

    public Material getGuiGmSurvivalMat() {
        return guiGmSurvivalMat;
    }

    public String getGuiGmCreativeName() {
        return guiGmCreativeName;
    }

    public List<String> getGuiGmCreativeLore() {
        return guiGmCreativeLore;
    }

    public Material getGuiGmCreativeMat() {
        return guiGmCreativeMat;
    }

    public String getGuiGmAdventureName() {
        return guiGmAdventureName;
    }

    public List<String> getGuiGmAdventureLore() {
        return guiGmAdventureLore;
    }

    public Material getGuiGmAdventureMat() {
        return guiGmAdventureMat;
    }

    public String getGuiGmSpectatorName() {
        return guiGmSpectatorName;
    }

    public List<String> getGuiGmSpectatorLore() {
        return guiGmSpectatorLore;
    }

    public Material getGuiGmSpectatorMat() {
        return guiGmSpectatorMat;
    }

    public boolean isGmMenuEnabled() {
        return gmMenuEnabled;
    }

    public String getGmUse() {
        return gmUse;
    }

    public String getGmModeInvalid() {
        return gmModeInvalid;
    }

    public String getFlyUse() {
        return flyUse;
    }

    public String getFlyDisabled() {
        return flyDisabled;
    }

    public String getFlyEnabledDefault() {
        return flyEnabledDefault;
    }

    public String getFlyEnabledLevel() {
        return flyEnabledLevel;
    }

    public float getFlySpeedLevel1() {
        return flySpeedLevel1;
    }

    public float getFlySpeedLevel2() {
        return flySpeedLevel2;
    }

    public float getFlySpeedLevel3() {
        return flySpeedLevel3;
    }

    public String getChatStaffUse() {
        return chatStaffUse;
    }

    public String getStaffChatToggleOn() {
        return staffChatToggleOn;
    }

    public String getStaffChatToggleOff() {
        return staffChatToggleOff;
    }

    public String getPlaceholderTrue() {
        return placeholderTrue;
    }

    public String getPlaceholderFalse() {
        return placeholderFalse;
    }

    public String getPlaceholderPlaytimeFormat() {
        return placeholderPlaytimeFormat;
    }

    public String getRestoredInventory() {
        return restoredinventory;
    }

    public String getGuiAltsTitle() {
        return guiAltsTitle;
    }

    public String getGuiAltsDynamic() {
        return guiAltsDynamic;
    }

    public String getGuiAltsStatusOnline() {
        return guiAltsStatusOnline;
    }

    public String getGuiAltsStatusOffline() {
        return guiAltsStatusOffline;
    }

    public String getGuiAltsStatusBanned() {
        return guiAltsStatusBanned;
    }

    public List<String> getGuiAltsLore() {
        return guiAltsLore;
    }

    public String getCommandSanctionUse() {
        return commandsanctionuse;
    }

    public String getCommandSanctionSelf() {
        return commandsanctionself;
    }

    public Material getGuiInfoAltsMat() {
        return guiInfoAltsMat;
    }

    public Material getGuiInfoInvMat() {
        return guiInfoInvMat;
    }

    public Material getGuiInfoPermissionsMat() {
        return guiInfoPermissionsMat;
    }

    public String getGuiInfoHeadName() {
        return guiInfoHeadName;
    }

    public List<String> getGuiInfoHeadLore() {
        return guiInfoHeadLore;
    }

    public String getGuiInfoAltsName() {
        return guiInfoAltsName;
    }

    public List<String> getGuiInfoAltsLore() {
        return guiInfoAltsLore;
    }

    public String getGuiInfoInvName() {
        return guiInfoInvName;
    }

    public List<String> getGuiInfoInvLore() {
        return guiInfoInvLore;
    }

    public String getGuiInfoPermissionsName() {
        return guiInfoPermissionsName;
    }

    public List<String> getGuiInfoPermissionsLore() {
        return guiInfoPermissionsLore;
    }

    public String getGuiPermissionsTitle() {
        return guiPermissionsTitle;
    }

    public String getGuiPermissionsItemName() {
        return guiPermissionsItemName;
    }

    public String getGuiPermissionsItemLore() {
        return guiPermissionsItemLore;
    }

    public String getGuiPermissionsEmptyName() {
        return guiPermissionsEmptyName;
    }

    public List<String> getGuiPermissionsEmptyLore() {
        return guiPermissionsEmptyLore;
    }

    public String getStatusOnline() {
        return statusOnline;
    }

    public String getStatusOffline() {
        return statusOffline;
    }

    public String getFreezeStaff() {
        return freezeStaff;
    }

    public String getFreezeAlready() {
        return freezeAlready;
    }

    public String getUnfreezeAlready() {
        return unfreezeAlready;
    }

    public String getAltsUse() {
        return altsUse;
    }

    public String getFreezeUse() {
        return frezzeUse;
    }

    public boolean isDbEnabled() {
        return dbEnabled;
    }

    public String getDbHost() {
        return dbHost;
    }

    public int getDbPort() {
        return dbPort;
    }

    public String getDbDatabase() {
        return dbDatabase;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public boolean isDbUseSSL() {
        return dbUseSSL;
    }

    public String getDbServerId() {
        return dbServerId;
    }

    public List<DatabaseEndpoint> getDbStatusEndpoints() {
        return dbStatusEndpoints;
    }

    public Material getMatHealFreeze() {
        return healmaterialfreeze;
    }

    public String getGuiConfirmTitle() {
        return guiConfirmTitle;
    }

    public String getGuiConfirmYesName() {
        return guiConfirmYesName;
    }

    public List<String> getGuiConfirmYesLore() {
        return guiConfirmYesLore;
    }

    public String getGuiConfirmNoName() {
        return guiConfirmNoName;
    }

    public List<String> getGuiConfirmNoLore() {
        return guiConfirmNoLore;
    }

    public Material getGuiConfirmYesMat() {
        return guiConfirmYesMat;
    }

    public Material getGuiConfirmNoMat() {
        return guiConfirmNoMat;
    }

    public boolean isReportEnabled() {
        return reportEnabled;
    }

    public int getReportCooldownSeconds() {
        return reportCooldownSeconds;
    }

    public String getReportCooldownBypassPermission() {
        return reportCooldownBypassPermission;
    }

    public boolean isReportRequireOnlineTarget() {
        return reportRequireOnlineTarget;
    }

    public boolean isReportSelfAllowed() {
        return reportSelfAllowed;
    }

    public int getReportMinReasonLength() {
        return reportMinReasonLength;
    }

    public int getReportMaxReasonLength() {
        return reportMaxReasonLength;
    }

    public boolean isReportStorageEnabled() {
        return reportStorageEnabled;
    }

    public boolean isReportNotifyEnabled() {
        return reportNotifyEnabled;
    }

    public String getReportNotifyPermission() {
        return reportNotifyPermission;
    }

    public String getReportUse() {
        return reportUse;
    }

    public String getReportDisabled() {
        return reportDisabled;
    }

    public String getReportSelf() {
        return reportSelf;
    }

    public String getReportOffline() {
        return reportOffline;
    }

    public String getReportReasonShort() {
        return reportReasonShort;
    }

    public String getReportReasonLong() {
        return reportReasonLong;
    }

    public String getReportCooldown() {
        return reportCooldown;
    }

    public String getReportSent() {
        return reportSent;
    }

    public String getReportStaffNotify() {
        return reportStaffNotify;
    }

    public boolean isReportStaffClickEnabled() {
        return reportStaffClickEnabled;
    }

    public String getReportStaffClickCommandTemplate() {
        return reportStaffClickCommandTemplate;
    }

    public ClickActionType getReportStaffClickActionType() {
        return reportStaffClickActionType;
    }

    public List<String> getReportStaffNotifyHover() {
        return reportStaffNotifyHover;
    }

    public boolean isAntiXrayEnabled() {
        return isModuleEnabled("anti-xray", false);
    }

    public boolean isAntiXrayIgnoreCreative() {
        return antiXrayIgnoreCreative;
    }

    public String getAntiXrayBypassPermission() {
        return antiXrayBypassPermission;
    }

    public boolean isAntiXrayNotifyEnabled() {
        return antiXrayNotifyEnabled;
    }

    public String getAntiXrayNotifyPermission() {
        return antiXrayNotifyPermission;
    }

    public boolean isAntiXrayClickEnabled() {
        return antiXrayClickEnabled;
    }

    public ClickActionType getAntiXrayClickActionType() {
        return antiXrayClickActionType;
    }

    public String getAntiXrayClickCommandTemplate() {
        return antiXrayClickCommandTemplate;
    }

    public String getAntiXrayAlertMessage() {
        return antiXrayAlertMessage;
    }

    public List<String> getAntiXrayAlertHover() {
        return antiXrayAlertHover;
    }

    public int getAntiXrayRateWindowSeconds() {
        return antiXrayRateWindowSeconds;
    }

    public int getAntiXrayMaterialThreshold() {
        return antiXrayMaterialThreshold;
    }

    public int getAntiXrayTotalThreshold() {
        return antiXrayTotalThreshold;
    }

    public int getAntiXraySessionThreshold() {
        return antiXraySessionThreshold;
    }

    public int getAntiXrayAlertCooldownSeconds() {
        return antiXrayAlertCooldownSeconds;
    }

    public Set<Material> getAntiXrayAlertBlocks() {
        return antiXrayAlertBlocks;
    }

    public String getAntiXrayDisplayName(Material material) {
        if (material == null) {
            return "";
        }
        return antiXrayDisplayNames.getOrDefault(material, prettifyMaterialName(material));
    }

    public enum ClickActionType {
        RUN_COMMAND,
        SUGGEST_COMMAND;

        private static ClickActionType fromConfig(String value) {
            if (value == null || value.isBlank()) {
                return SUGGEST_COMMAND;
            }

            try {
                return ClickActionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return SUGGEST_COMMAND;
            }
        }
    }

    public Material getGuiSanctionBanMat() {
        return guiSanctionBanMat;
    }

    public Material getGuiSanctionMuteMat() {
        return guiSanctionMuteMat;
    }

    public Material getGuiSanctionKickMat() {
        return guiSanctionKickMat;
    }

    public Material getGuiHistoryBansMat() {
        return guiHistoryBansMat;
    }

    public Material getGuiHistoryMutesMat() {
        return guiHistoryMutesMat;
    }

    public Material getGuiHistoryWarnsMat() {
        return guiHistoryWarnsMat;
    }

    public Material getGuiHistoryKicksMat() {
        return guiHistoryKicksMat;
    }

    public Material getGuiDetailedRecordMat() {
        return guiDetailedRecordMat;
    }

    public Material getGuiActiveBanMat() {
        return guiActiveBanMat;
    }

    public Material getGuiActiveIpBanMat() {
        return guiActiveIpBanMat;
    }

    public Material getGuiActiveMuteMat() {
        return guiActiveMuteMat;
    }

    public Material getGuiActiveEmptyMat() {
        return guiActiveEmptyMat;
    }

    public Material getGuiActivePageInfoMat() {
        return guiActivePageInfoMat;
    }

    public int getGuiActiveMenuSize() {
        return guiActiveMenuSize;
    }

    public String getGuiActiveTitle() {
        return guiActiveTitle;
    }

    public String getGuiActiveItemName() {
        return guiActiveItemName;
    }

    public List<String> getGuiActiveItemLore() {
        return guiActiveItemLore;
    }

    public String getGuiActiveEmptyName() {
        return guiActiveEmptyName;
    }

    public List<String> getGuiActiveEmptyLore() {
        return guiActiveEmptyLore;
    }

    public String getGuiActivePageInfoName() {
        return guiActivePageInfoName;
    }

    public List<String> getGuiActivePageInfoLore() {
        return guiActivePageInfoLore;
    }

    public String getInvseeUse() {
        return invseeUse;
    }

    public String getInvseeSelf() {
        return invseeSelf;
    }

    public String getInvseeCheck() {
        return invseeCheck;
    }

    public boolean isFreezeBanOnDisconnectEnabled() {
        return freezeBanOnDisconnectEnabled;
    }

    public int getFreezeBanOnDisconnectDays() {
        return freezeBanOnDisconnectDays;
    }

    public String getFreezeBanOnDisconnectReason() {
        return freezeBanOnDisconnectReason;
    }

    public boolean isFreezeDisplayEnabled() {
        return freezeDisplayEnabled;
    }

    public double getFreezeDisplayDistance() {
        return freezeDisplayDistance;
    }

    public double getFreezeDisplayHeightOffset() {
        return freezeDisplayHeightOffset;
    }

    public long getFreezeDisplayUpdateTicks() {
        return freezeDisplayUpdateTicks;
    }

    public List<String> getFreezeDisplayLines() {
        return freezeDisplayLines;
    }

    public double getFreezeDisplaySideOffset() {
        return freezeDisplaySideOffset;
    }

    public boolean isFreezeDisplayBackgroundEnabled() {
        return freezeDisplayBackgroundEnabled;
    }

    public boolean isFreezeDisplayTextShadowEnabled() {
        return freezeDisplayTextShadowEnabled;
    }

    public Color getFreezeDisplayBackgroundColor() {
        return freezeDisplayBackgroundColor;
    }

    public String getFreezeRiskLowLabel() {
        return freezeRiskLowLabel;
    }

    public String getFreezeRiskMediumLabel() {
        return freezeRiskMediumLabel;
    }

    public String getFreezeRiskHighLabel() {
        return freezeRiskHighLabel;
    }

    public String getFreezeRiskLowColor() {
        return freezeRiskLowColor;
    }

    public String getFreezeRiskMediumColor() {
        return freezeRiskMediumColor;
    }

    public String getFreezeRiskHighColor() {
        return freezeRiskHighColor;
    }

    public int getFreezeRiskMediumMinScore() {
        return freezeRiskMediumMinScore;
    }

    public int getFreezeRiskHighMinScore() {
        return freezeRiskHighMinScore;
    }

    public int getFreezeRiskLowBars() {
        return freezeRiskLowBars;
    }

    public int getFreezeRiskMediumBars() {
        return freezeRiskMediumBars;
    }

    public int getFreezeRiskHighBars() {
        return freezeRiskHighBars;
    }

    public int getFreezeRiskTotalBars() {
        return freezeRiskTotalBars;
    }

    public String getInvseeOfflineLoaded() {
        return invseeOfflineLoaded;
    }

    public String getInvseeOfflineUnavailable() {
        return invseeOfflineUnavailable;
    }

    public String getInvseeInspectionOnlineTitle() {
        return invseeInspectionOnlineTitle;
    }

    public String getInvseeInspectionOfflineTitle() {
        return invseeInspectionOfflineTitle;
    }

    public String getInvseeLegacyMainHandNotice() {
        return invseeLegacyMainHandNotice;
    }

    public String getInspectArmorBootsLabel() {
        return inspectArmorBootsLabel;
    }

    public String getInspectArmorLeggingsLabel() {
        return inspectArmorLeggingsLabel;
    }

    public String getInspectArmorChestplateLabel() {
        return inspectArmorChestplateLabel;
    }

    public String getInspectArmorHelmetLabel() {
        return inspectArmorHelmetLabel;
    }

    public String getInspectOffhandLabel() {
        return inspectOffhandLabel;
    }

    public String getInspectMainhandLabel() {
        return inspectMainhandLabel;
    }

    public String getInspectNoItemSuffix() {
        return inspectNoItemSuffix;
    }

    public String getInspectStatusTitle() {
        return inspectStatusTitle;
    }

    public List<String> getInspectStatusLore() {
        return inspectStatusLore;
    }

    public Material getInspectStatusMaterial() {
        return inspectStatusMaterial;
    }

    public int getInspectArmorStartSlot() {
        return inspectArmorStartSlot;
    }

    public int getInspectOffhandSlot() {
        return inspectOffhandSlot;
    }

    public int getInspectMainhandSlot() {
        return inspectMainhandSlot;
    }

    public int getInspectStatusSlot() {
        return inspectStatusSlot;
    }

    public String getReviveUse() {
        return reviveUse;
    }

    public String getReviveNoDeaths() {
        return reviveNoDeaths;
    }

    public String getReviveTargetOffline() {
        return reviveTargetOffline;
    }

    public String getReviveRestored() {
        return reviveRestored;
    }

    private Color parseColorOrDefault(String rawHex, int alpha, Color fallback) {
        if (rawHex == null || rawHex.isBlank()) {
            return fallback;
        }

        String hex = rawHex.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }

        if (hex.length() != 6) {
            plugin.getLogger().warning("Invalid freeze display background color '" + rawHex + "'. Using fallback.");
            return fallback;
        }

        try {
            int rgb = Integer.parseInt(hex, 16);
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;
            return Color.fromARGB(alpha, red, green, blue);
        } catch (NumberFormatException exception) {
            plugin.getLogger().warning("Invalid freeze display background color '" + rawHex + "'. Using fallback.");
            return fallback;
        }
    }

    public String getGuiReviveTitle() {
        return guiReviveTitle;
    }

    public String getGuiReviveItemName() {
        return guiReviveItemName;
    }

    public List<String> getGuiReviveItemLore() {
        return guiReviveItemLore;
    }

    public String getGuiRevivePageInfoName() {
        return guiRevivePageInfoName;
    }

    public List<String> getGuiRevivePageInfoLore() {
        return guiRevivePageInfoLore;
    }

    public int getReviveMenuSize() {
        return reviveMenuSize;
    }

    public int getRevivePageSize() {
        return revivePageSize;
    }

    public int getInventoryDeathSnapshotMaxAgeMinutes() {
        return inventoryDeathSnapshotMaxAgeMinutes;
    }

    public int getInventorySnapshotCleanupIntervalMinutes() {
        return inventorySnapshotCleanupIntervalMinutes;
    }

    public Material getMatWallCompass() {
        return matWallCompass;
    }

    public String getItemNameWallCompass() {
        return itemNameWallCompass;
    }

    public int getWallCompassRange() {
        return wallCompassRange;
    }

    public int getStaffPunishSlot() {
        return staffPunishSlot;
    }

    public int getStaffFreezeSlot() {
        return staffFreezeSlot;
    }

    public int getStaffPlayersSlot() {
        return staffPlayersSlot;
    }

    public int getStaffWallCompassSlot() {
        return staffWallCompassSlot;
    }

    public int getStaffInspectSlot() {
        return staffInspectSlot;
    }

    public int getStaffVanishSlot() {
        return staffVanishSlot;
    }

    public boolean isStaffModeAllowHit() {
        return staffModeAllowHit;
    }

    public boolean isStaffModeAllowContainerItemMove() {
        return staffModeAllowContainerItemMove;
    }

    public boolean isVanishPersistenceEnabled() {
        return vanishPersistenceEnabled;
    }

    public boolean isClientTrackerEnabled() {
        return clientTrackerEnabled;
    }

    public boolean isClientTrackerNotifyEnabled() {
        return clientTrackerNotifyEnabled;
    }

    public int getClientTrackerTimeoutTicks() {
        return clientTrackerTimeoutTicks;
    }

    public String getClientTrackerUnknownName() {
        return clientTrackerUnknownName;
    }

    public String getClientTrackerNotifyPermission() {
        return clientTrackerNotifyPermission;
    }

    public String getClientTrackerJoinMessage() {
        return clientTrackerJoinMessage;
    }

    public Map<String, List<String>> getClientTrackerCustomMappings() {
        return clientTrackerCustomMappings;
    }

    public boolean isModuleEnabled(String moduleKey) {
        return modules.getOrDefault(moduleKey, true);
    }

    public boolean isModuleEnabled(String moduleKey, boolean defaultValue) {
        return modules.getOrDefault(moduleKey, defaultValue);
    }

    public Map<String, Boolean> getModules() {
        return modules;
    }

    public Set<String> getDisabledPluginCommands() {
        return disabledPluginCommands;
    }

    public Set<String> getStaffModeBlacklistedCommands() {
        return staffModeBlacklistedCommands;
    }

    public boolean isFreezeBlockAllCommands() {
        return freezeBlockAllCommands;
    }

    public boolean isPluginCommandDisabled(String commandLabel) {
        if (commandLabel == null || commandLabel.isBlank()) {
            return false;
        }

        String normalized = commandLabel.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        return disabledPluginCommands.contains(normalized);
    }

    public Material getMatRandomTp() {
        return matRandomTp;
    }

    public String getItemNameRandomTp() {
        return itemNameRandomTp;
    }

    public int getStaffRandomTpSlot() {
        return staffRandomTpSlot;
    }

    public String getSilentPunishmentUse() {
        return silentPunishmentUse;
    }

    public String getSilentInvalid() {
        return silentInvalid;
    }

    public String getSilentFormatInvalid() {
        return silentFormatInvalid;
    }

    private List<String> normalizeCommandEntries(List<String> commands) {
        if (commands == null || commands.isEmpty()) {
            return List.of();
        }

        return commands.stream()
                .map(cmd -> cmd == null ? "" : cmd.trim().toLowerCase(Locale.ROOT))
                .map(cmd -> cmd.startsWith("/") ? cmd.substring(1) : cmd)
                .filter(cmd -> !cmd.isEmpty())
                .toList();
    }

    private Map<String, List<String>> loadClientMappings(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("client-tracker.client-signatures");
        if (section == null) {
            return Map.of();
        }

        Map<String, List<String>> mappings = new LinkedHashMap<>();
        for (String clientName : section.getKeys(false)) {
            List<String> signatures = section.getStringList(clientName);
            if (signatures == null || signatures.isEmpty()) {
                continue;
            }

            List<String> normalized = signatures.stream()
                    .map(value -> value == null ? "" : value.trim().toLowerCase(Locale.ROOT))
                    .filter(value -> !value.isEmpty())
                    .toList();

            if (!normalized.isEmpty()) {
                mappings.put(clientName, normalized);
            }
        }

        return mappings.isEmpty() ? Map.of() : Collections.unmodifiableMap(mappings);
    }

    private Set<Material> loadAntiXrayAlertBlocks(FileConfiguration config) {
        List<String> configuredBlocks = config.getStringList("anti-xray.alert-blocks");
        if (!config.isList("anti-xray.alert-blocks")) {
            configuredBlocks = defaultAntiXrayBlocks();
        }

        if (configuredBlocks == null || configuredBlocks.isEmpty()) {
            return Set.of();
        }

        EnumSet<Material> parsedBlocks = EnumSet.noneOf(Material.class);
        for (String rawBlock : configuredBlocks) {
            if (rawBlock == null || rawBlock.isBlank()) {
                continue;
            }

            Material material = Material.matchMaterial(rawBlock.trim());
            if (material == null || !material.isBlock()) {
                plugin.getLogger().warning("Invalid anti-xray alert block '" + rawBlock + "'. Skipping.");
                continue;
            }

            parsedBlocks.add(material);
        }

        return parsedBlocks.isEmpty() ? Set.of() : Collections.unmodifiableSet(parsedBlocks);
    }

    private Map<Material, String> loadAntiXrayDisplayNames(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("anti-xray.display-names");
        if (section == null) {
            return Map.of();
        }

        Map<Material, String> displayNames = new LinkedHashMap<>();
        for (String materialKey : section.getKeys(false)) {
            Material material = Material.matchMaterial(materialKey);
            String displayName = section.getString(materialKey, "").trim();
            if (material == null || displayName.isEmpty()) {
                continue;
            }
            displayNames.put(material, displayName);
        }

        return displayNames.isEmpty() ? Map.of() : Collections.unmodifiableMap(displayNames);
    }

    private List<String> defaultAntiXrayBlocks() {
        return Arrays.asList(
                "DIAMOND_ORE",
                "DEEPSLATE_DIAMOND_ORE",
                "REDSTONE_ORE",
                "DEEPSLATE_REDSTONE_ORE",
                "ANCIENT_DEBRIS",
                "EMERALD_ORE",
                "DEEPSLATE_EMERALD_ORE",
                "LAPIS_ORE",
                "DEEPSLATE_LAPIS_ORE",
                "GOLD_ORE",
                "DEEPSLATE_GOLD_ORE",
                "NETHER_GOLD_ORE",
                "IRON_ORE",
                "DEEPSLATE_IRON_ORE",
                "COPPER_ORE",
                "DEEPSLATE_COPPER_ORE",
                "COAL_ORE",
                "DEEPSLATE_COAL_ORE");
    }

    private String prettifyMaterialName(Material material) {
        String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder(material.name().length());
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.toString();
    }

    private float readFlySpeed(FileConfiguration config, String path, float defaultValue) {
        double raw = config.getDouble(path, defaultValue);
        if (raw < 0.1D || raw > 1.0D) {
            plugin.getLogger().warning("Invalid fly speed at '" + path + "': " + raw + ". Clamping to valid range.");
        }
        return (float) Math.max(0.1D, Math.min(1.0D, raw));
    }

    private Map<String, Boolean> loadModules(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("modules");
        if (section == null) {
            return Collections.emptyMap();
        }

        Map<String, Boolean> parsedModules = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            if (!section.isBoolean(key)) {
                continue;
            }
            parsedModules.put(key.toLowerCase(Locale.ROOT), section.getBoolean(key, true));
        }

        return Collections.unmodifiableMap(parsedModules);
    }

    private Set<String> loadDisabledCommands(FileConfiguration config) {
        LinkedHashSet<String> disabledCommands = new LinkedHashSet<>();
        disabledCommands.addAll(normalizeCommandEntries(config.getStringList("disable-commands")));
        disabledCommands.addAll(normalizeCommandEntries(config.getStringList("modules.disable-commands")));

        if (disabledCommands.isEmpty()) {
            return Set.of();
        }

        return Collections.unmodifiableSet(disabledCommands);
    }

    private Set<String> loadCommandSet(FileConfiguration config, String path) {
        LinkedHashSet<String> commands = new LinkedHashSet<>(normalizeCommandEntries(config.getStringList(path)));
        return commands.isEmpty() ? Set.of() : Collections.unmodifiableSet(commands);
    }

    private List<DatabaseEndpoint> loadDbStatusEndpoints(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("database.status-servers");
        if (section == null) {
            return List.of(new DatabaseEndpoint("Main", dbHost, dbPort));
        }

        List<DatabaseEndpoint> endpoints = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection serverSection = section.getConfigurationSection(key);
            if (serverSection == null) {
                continue;
            }

            String name = serverSection.getString("name", key);
            String host = serverSection.getString("host", "-");
            int port = serverSection.getInt("port", dbPort);
            endpoints.add(new DatabaseEndpoint(name, host, port));
        }

        if (endpoints.isEmpty()) {
            endpoints.add(new DatabaseEndpoint("Main", dbHost, dbPort));
        }

        return Collections.unmodifiableList(endpoints);
    }

    private void loadPunishmentSectionLimits(FileConfiguration config) {
        ConfigurationSection base = config.getConfigurationSection("punishments.section-limits");
        if (base == null) {
            punishmentSectionLimitsEnabled = config.getBoolean("punishments.section-limited", false);
            punishmentLimitGroups = List.of();
            return;
        }

        punishmentSectionLimitsEnabled = base.getBoolean("enabled",
                config.getBoolean("punishments.section-limited", false));

        ConfigurationSection groupsSection = base.getConfigurationSection("groups");
        if (groupsSection == null) {
            groupsSection = base;
        }

        List<PunishmentLimitGroup> parsedGroups = new ArrayList<>();
        for (String groupId : groupsSection.getKeys(false)) {
            if (groupId.equalsIgnoreCase("enabled") || groupId.equalsIgnoreCase("section-limited")
                    || groupId.equalsIgnoreCase("groups")) {
                continue;
            }

            ConfigurationSection groupSection = groupsSection.getConfigurationSection(groupId);
            if (groupSection == null) {
                continue;
            }

            String permission = groupSection.getString("permission", "").trim();
            if (permission.isEmpty()) {
                plugin.getLogger().warning("Punishment section-limit group '" + groupId
                        + "' is missing a permission node. Skipping.");
                continue;
            }

            ConfigurationSection limitsSection = groupSection.getConfigurationSection("limits");
            if (limitsSection == null) {
                continue;
            }

            Map<String, Long> limits = new LinkedHashMap<>();
            for (String limitTypeRaw : limitsSection.getKeys(false)) {
                String token = limitsSection.getString(limitTypeRaw, "").trim().toLowerCase(Locale.ROOT);
                if (!TimeUtils.isValidDurationToken(token)) {
                    plugin.getLogger()
                            .warning("Invalid duration token '" + token + "' in punishment section-limit group '"
                                    + groupId + "' for type '" + limitTypeRaw + "'.");
                    continue;
                }
                limits.put(limitTypeRaw.toLowerCase(Locale.ROOT), TimeUtils.parseDuration(token));
            }

            if (!limits.isEmpty()) {
                parsedGroups.add(new PunishmentLimitGroup(groupId, permission, Collections.unmodifiableMap(limits)));
            }
        }

        punishmentLimitGroups = Collections.unmodifiableList(parsedGroups);
    }

    public boolean isPunishmentSectionLimitsEnabled() {
        return punishmentSectionLimitsEnabled;
    }

    public String getMsgPunishmentLimitExceeded() {
        return msgPunishmentLimitExceeded;
    }

    public long getPunishmentDurationLimit(CommandSender sender, String punishmentType) {
        if (!punishmentSectionLimitsEnabled || sender == null || punishmentType == null || punishmentType.isBlank()) {
            return Long.MIN_VALUE;
        }

        String normalizedType = punishmentType.toLowerCase(Locale.ROOT).trim();
        if (normalizedType.equals("tempban")) {
            normalizedType = "ban";
        } else if (normalizedType.equals("tempmute")) {
            normalizedType = "mute";
        }

        long selectedLimit = Long.MIN_VALUE;
        for (PunishmentLimitGroup group : punishmentLimitGroups) {
            if (!sender.hasPermission(group.permission)) {
                continue;
            }

            Long direct = group.limits.get(punishmentType.toLowerCase(Locale.ROOT).trim());
            Long normalized = group.limits.get(normalizedType);
            Long candidate = direct != null ? direct : normalized;
            if (candidate == null) {
                continue;
            }

            if (selectedLimit == Long.MIN_VALUE || compareLimit(candidate, selectedLimit) > 0) {
                selectedLimit = candidate;
            }
        }
        return selectedLimit;
    }

    public boolean isPunishmentDurationAllowed(CommandSender sender, String punishmentType, String durationToken) {
        long requested = TimeUtils.parseDuration(durationToken);
        if (requested == 0) {
            return false;
        }

        long limit = getPunishmentDurationLimit(sender, punishmentType);
        if (limit == Long.MIN_VALUE || limit == -1) {
            return true;
        }
        if (requested == -1) {
            return false;
        }
        return requested <= limit;
    }

    private int compareLimit(long left, long right) {
        if (left == right) {
            return 0;
        }
        if (left == -1) {
            return 1;
        }
        if (right == -1) {
            return -1;
        }
        return Long.compare(left, right);
    }

    public static class DatabaseEndpoint {
        private final String name;
        private final String host;
        private final int port;

        public DatabaseEndpoint(String name, String host, int port) {
            this.name = name;
            this.host = host;
            this.port = port;
        }

        public String getName() {
            return name;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }

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
