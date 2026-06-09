package org.zkaleejoo.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class FreezeManager {
    private static final long DISPLAY_INFO_REFRESH_TICKS = 20L;
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final String SOUND_UNFREEZE_STAFF = "minecraft:block.fire.extinguish";
    private static final String SOUND_FREEZE_STAFF = "minecraft:block.glass.break";
    private static final String SOUND_FREEZE_TARGET = "minecraft:block.note_block.pling";
    private static final String SOUND_UNFREEZE_TARGET = "minecraft:entity.player.levelup";

    private final MaxStaff plugin;
    private final NamespacedKey freezeDisplayOwnerKey;
    private final NamespacedKey freezeDisplayFlagKey;
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private final Map<UUID, ItemStack> savedHelmets = new HashMap<>();
    private final Map<UUID, FrozenDisplayTracker> frozenDisplays = new HashMap<>();

    public FreezeManager(MaxStaff plugin) {
        this.plugin = plugin;
        this.freezeDisplayOwnerKey = new NamespacedKey(plugin, "freeze_display_owner");
        this.freezeDisplayFlagKey = new NamespacedKey(plugin, "freeze_display");
    }

    public void toggleFreeze(Player staff, Player target) {
        if (isFrozen(target)) {
            setFrozen(target, false, staff.getName());
            playSound(staff, SOUND_UNFREEZE_STAFF, 1.0f, 1.5f);
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix()
                    + plugin.getMainConfigManager().getMsgUnfreezeStaff().replace("{player}", target.getName())));
        } else {
            setFrozen(target, true, staff.getName());
            playSound(staff, SOUND_FREEZE_STAFF, 1.0f, 0.5f);
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix()
                    + plugin.getMainConfigManager().getMsgFreezeStaff().replace("{player}", target.getName())));
        }
    }

    public void setFrozen(Player target, boolean freeze) {
        setFrozen(target, freeze, "Console");
    }

    public void setFrozen(Player target, boolean freeze, CommandSender actor) {
        setFrozen(target, freeze, actor != null ? actor.getName() : "Console");
    }

    public void setFrozen(Player target, boolean freeze, String staffName) {
        String actorName = (staffName == null || staffName.trim().isEmpty()) ? "Console" : staffName;

        if (freeze) {

            plugin.getDiscordManager().sendWebhook("freeze", target.getName(), actorName, null, null, null);

            frozenPlayers.add(target.getUniqueId());

            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));

            ItemStack currentHelmet = target.getInventory().getHelmet();
            savedHelmets.put(target.getUniqueId(), currentHelmet);

            target.getInventory().setHelmet(new ItemStack(getFreezeHelmetMaterial()));

            target.updateInventory();
            spawnFrozenDisplay(target);

            for (String line : plugin.getMainConfigManager().getMsgTargetFrozen()) {
                target.sendMessage(MessageUtils.getColoredMessage(line));
            }

            playSound(target, SOUND_FREEZE_TARGET, 2.0f, 0.5f);
            target.closeInventory();

        } else {
            clearFrozenState(target);

            plugin.getDiscordManager().sendWebhook("unfreeze", target.getName(), actorName, null, null, null);

            target.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgTargetUnfrozen()));
            playSound(target, SOUND_UNFREEZE_TARGET, 1.0f, 2.0f);
        }
    }

    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }

    public void handleDisconnect(Player player) {
        if (!isFrozen(player)) {
            return;
        }

        clearFrozenState(player);

        if (plugin.getMainConfigManager().isFreezeBanOnDisconnectEnabled()) {
            int banDays = plugin.getMainConfigManager().getFreezeBanOnDisconnectDays();
            String duration = banDays + "d";
            String reason = plugin.getMainConfigManager().getFreezeBanOnDisconnectReason().replace("{days}",
                    String.valueOf(banDays));

            plugin.getPunishmentManager().banPlayer(Bukkit.getConsoleSender(), player.getName(), reason, duration);
        }
    }

    private void clearFrozenState(Player target) {
        frozenPlayers.remove(target.getUniqueId());

        target.removePotionEffect(PotionEffectType.BLINDNESS);

        if (savedHelmets.containsKey(target.getUniqueId())) {
            ItemStack original = savedHelmets.remove(target.getUniqueId());
            target.getInventory().setHelmet(original);
        } else {
            target.getInventory().setHelmet(null);
        }

        target.updateInventory();
        removeFrozenDisplay(target.getUniqueId());
    }

    private void spawnFrozenDisplay(Player target) {
        removeFrozenDisplay(target.getUniqueId());

        if (!plugin.getMainConfigManager().isFreezeDisplayEnabled()) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int bans = plugin.getPunishmentManager().getHistoryCount(target.getName(), "BAN");
            int mutes = plugin.getPunishmentManager().getHistoryCount(target.getName(), "MUTE");
            int kicks = plugin.getPunishmentManager().getHistoryCount(target.getName(), "KICK");

            Bukkit.getScheduler().runTask(plugin, () -> {

                if (!target.isOnline() || !isFrozen(target)) {
                    return;
                }

                try {
                    long updateTicks = Math.max(1L, plugin.getMainConfigManager().getFreezeDisplayUpdateTicks());
                    long refreshEveryRuns = Math.max(1L, DISPLAY_INFO_REFRESH_TICKS / updateTicks);

                    String initialText = getFrozenDisplayText(target, bans, mutes, kicks);

                    TextDisplay textDisplay = target.getWorld().spawn(calculateDisplayLocation(target),
                            TextDisplay.class,
                            display -> {
                                applyDisplayStyle(display);
                                display.getPersistentDataContainer().set(freezeDisplayFlagKey, PersistentDataType.BYTE,
                                        (byte) 1);
                                display.getPersistentDataContainer().set(freezeDisplayOwnerKey, PersistentDataType.STRING,
                                        target.getUniqueId().toString());
                                display.text(toComponent(initialText));
                            });

                    updateDisplayVisibility(textDisplay);

                    FrozenDisplayTracker tracker = new FrozenDisplayTracker(textDisplay, null, initialText,
                            refreshEveryRuns);
                    tracker.cachedBans = bans;
                    tracker.cachedMutes = mutes;
                    tracker.cachedKicks = kicks;

                    BukkitTask trackingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                        if (!plugin.isEnabled() || !target.isOnline() || !isFrozen(target) || !textDisplay.isValid()) {
                            removeFrozenDisplay(target.getUniqueId());
                            return;
                        }

                        tracker.tickCounter++;
                        if (tracker.tickCounter >= tracker.refreshEveryRuns) {
                            tracker.tickCounter = 0L;
                            updateDisplayVisibility(textDisplay);
                            applyDisplayStyle(textDisplay);

                            String updatedText = getFrozenDisplayText(target, tracker.cachedBans, tracker.cachedMutes,
                                    tracker.cachedKicks);

                            if (!updatedText.equals(tracker.lastText)) {
                                textDisplay.text(toComponent(updatedText));
                                tracker.lastText = updatedText;
                            }
                        }

                        textDisplay.teleport(calculateDisplayLocation(target));
                    }, 0L, updateTicks);

                    tracker.task = trackingTask;
                    frozenDisplays.put(target.getUniqueId(), tracker);

                } catch (Throwable throwable) {
                    plugin.getLogger().warning("Failed to create freeze TextDisplay for " + target.getName() + ": "
                            + throwable.getMessage());
                }
            });
        });
    }

    private String getFrozenDisplayText(Player target, int bans, int mutes, int kicks) {
        List<String> lines = new ArrayList<>(plugin.getMainConfigManager().getFreezeDisplayLines());
        if (lines.isEmpty()) {
            lines.add("&#FF3405&l❖ SANCTION PROFILE");
            lines.add("&8&m---------------------------");
            lines.add("&fPlayer: &b{name}");
            lines.add("&fHistory: {risk_bar} &8(&7{total}&8)");
            lines.add("&fRisk: {risk_color}&l{risk_label}");
            lines.add("&cBans: &f{bans}  &6Mutes: &f{mutes}  &eKicks: &f{kicks}");
            lines.add("&8&m---------------------------");
        }

        int total = bans + mutes + kicks;
        RiskLevel riskLevel = resolveRiskLevel(bans, mutes, kicks);
        String riskLabel = resolveRiskLabel(riskLevel);
        String riskColor = resolveRiskColor(riskLevel);
        String riskBar = buildRiskBar(riskLevel);

        for (int i = 0; i < lines.size(); i++) {
            String rawLine = Objects.toString(lines.get(i), "");
            String parsed = rawLine
                    .replace("{name}", target.getName())
                    .replace("{bans}", String.valueOf(bans))
                    .replace("{mutes}", String.valueOf(mutes))
                    .replace("{kicks}", String.valueOf(kicks))
                    .replace("{total}", String.valueOf(total))
                    .replace("{risk_label}", riskLabel)
                    .replace("{risk_color}", riskColor)
                    .replace("{risk_bar}", riskBar);
            lines.set(i, MessageUtils.getColoredMessage(parsed));
        }

        return String.join("\n", lines);
    }

    private RiskLevel resolveRiskLevel(int bans, int mutes, int kicks) {
        int score = (bans * 3) + (mutes * 2) + kicks;
        if (score >= plugin.getMainConfigManager().getFreezeRiskHighMinScore()) {
            return RiskLevel.HIGH;
        }
        if (score >= plugin.getMainConfigManager().getFreezeRiskMediumMinScore()) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private String resolveRiskLabel(RiskLevel riskLevel) {
        if (riskLevel == RiskLevel.HIGH) {
            return plugin.getMainConfigManager().getFreezeRiskHighLabel();
        }
        if (riskLevel == RiskLevel.MEDIUM) {
            return plugin.getMainConfigManager().getFreezeRiskMediumLabel();
        }
        return plugin.getMainConfigManager().getFreezeRiskLowLabel();
    }

    private String resolveRiskColor(RiskLevel riskLevel) {
        if (riskLevel == RiskLevel.HIGH) {
            return plugin.getMainConfigManager().getFreezeRiskHighColor();
        }
        if (riskLevel == RiskLevel.MEDIUM) {
            return plugin.getMainConfigManager().getFreezeRiskMediumColor();
        }
        return plugin.getMainConfigManager().getFreezeRiskLowColor();
    }

    private String buildRiskBar(RiskLevel riskLevel) {
        String activeColor = resolveRiskColor(riskLevel);

        int activeBars;
        if (riskLevel == RiskLevel.HIGH) {
            activeBars = plugin.getMainConfigManager().getFreezeRiskHighBars();
        } else if (riskLevel == RiskLevel.MEDIUM) {
            activeBars = plugin.getMainConfigManager().getFreezeRiskMediumBars();
        } else {
            activeBars = plugin.getMainConfigManager().getFreezeRiskLowBars();
        }

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < plugin.getMainConfigManager().getFreezeRiskTotalBars(); i++) {
            if (i < activeBars) {
                bar.append(activeColor).append("█");
            } else {
                bar.append("&7").append("█");
            }
        }
        return bar.toString();
    }

    private enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    private Location calculateDisplayLocation(Player target) {
        Location eyeLocation = target.getEyeLocation();
        Vector lookDirection = eyeLocation.getDirection().normalize();
        Vector direction = lookDirection.clone().multiply(plugin.getMainConfigManager().getFreezeDisplayDistance());
        Vector horizontalRight = new Vector(-lookDirection.getZ(), 0.0D, lookDirection.getX());
        if (horizontalRight.lengthSquared() > 0.0D) {
            horizontalRight.normalize();
        }
        Vector right = horizontalRight.multiply(plugin.getMainConfigManager().getFreezeDisplaySideOffset());
        return eyeLocation
                .add(direction)
                .add(right)
                .add(0.0D, plugin.getMainConfigManager().getFreezeDisplayHeightOffset(), 0.0D);
    }

    private void playSound(Player player, String soundName, float volume, float pitch) {
        if (soundName != null && !soundName.isEmpty()) {
            player.playSound(player, soundName, volume, pitch);
        }
    }

    private Component toComponent(String text) {
        return LEGACY_SERIALIZER.deserialize(text);
    }

    private Material getFreezeHelmetMaterial() {
        return Objects.requireNonNullElse(plugin.getMainConfigManager().getMatHealFreeze(), Material.PACKED_ICE);
    }

    private void applyDisplayStyle(TextDisplay display) {
        if (display == null) {
            return;
        }

        display.setBillboard(Display.Billboard.CENTER);
        display.setSeeThrough(true);
        display.setShadowed(plugin.getMainConfigManager().isFreezeDisplayTextShadowEnabled());
        applyDisplayBackground(display);
    }

    private void applyDisplayBackground(TextDisplay display) {
        boolean backgroundEnabled = plugin.getMainConfigManager().isFreezeDisplayBackgroundEnabled();
        display.setDefaultBackground(false);
        if (backgroundEnabled) {
            display.setBackgroundColor(plugin.getMainConfigManager().getFreezeDisplayBackgroundColor());
        } else {
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        }
    }

    private void removeFrozenDisplay(UUID uuid) {
        FrozenDisplayTracker tracker = frozenDisplays.remove(uuid);
        if (tracker != null) {
            if (tracker.task != null) {
                tracker.task.cancel();
            }

            if (tracker.display != null && tracker.display.isValid()) {
                tracker.display.remove();
            }
        }

        removeOrphanDisplaysFor(uuid);
    }

    private void updateDisplayVisibility(TextDisplay display) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null) continue;
            if (canViewFrozenStats(online)) {
                online.showEntity(Objects.requireNonNull(plugin), Objects.requireNonNull(display));
            } else {
                online.hideEntity(Objects.requireNonNull(plugin), Objects.requireNonNull(display));
            }
        }
    }

    private boolean canViewFrozenStats(Player viewer) {
        return viewer.hasPermission("maxstaff.freeze") || viewer.hasPermission("maxstaff.admin");
    }

    public void removeAllDisplays() {
        List<UUID> uuids = new ArrayList<>(frozenDisplays.keySet());
        for (UUID uuid : uuids) {
            removeFrozenDisplay(uuid);
        }
    }

    public void refreshFrozenDisplays() {
        for (UUID uuid : new HashSet<>(frozenPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                removeFrozenDisplay(uuid);
                continue;
            }
            spawnFrozenDisplay(player);
        }
    }

    public void cleanupOrphanDisplays() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof TextDisplay display)) {
                    continue;
                }

                if (!isFreezeDisplayEntity(display)) {
                    continue;
                }

                String ownerRaw = display.getPersistentDataContainer().get(Objects.requireNonNull(freezeDisplayOwnerKey),
                        Objects.requireNonNull(PersistentDataType.STRING));
                if (ownerRaw == null) {
                    display.remove();
                    continue;
                }

                UUID ownerUuid;
                try {
                    ownerUuid = UUID.fromString(ownerRaw);
                } catch (IllegalArgumentException ex) {
                    display.remove();
                    continue;
                }

                Player owner = Bukkit.getPlayer(ownerUuid);
                if (owner == null || !owner.isOnline() || !isFrozen(owner)) {
                    display.remove();
                }
            }
        }
    }

    private void removeOrphanDisplaysFor(UUID ownerUuid) {
        String owner = ownerUuid.toString();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof TextDisplay display)) {
                    continue;
                }

                if (!isFreezeDisplayEntity(display)) {
                    continue;
                }

                String displayOwner = display.getPersistentDataContainer().get(Objects.requireNonNull(freezeDisplayOwnerKey),
                        Objects.requireNonNull(PersistentDataType.STRING));
                if (owner.equals(displayOwner)) {
                    display.remove();
                }
            }
        }
    }

    private boolean isFreezeDisplayEntity(TextDisplay display) {
        Byte marker = display.getPersistentDataContainer().get(Objects.requireNonNull(freezeDisplayFlagKey), Objects.requireNonNull(PersistentDataType.BYTE));
        return marker != null && marker == (byte) 1;
    }

    private static class FrozenDisplayTracker {
        private final TextDisplay display;
        private BukkitTask task;
        private String lastText;
        private long tickCounter;
        private final long refreshEveryRuns;

        public int cachedBans = 0;
        public int cachedMutes = 0;
        public int cachedKicks = 0;

        private FrozenDisplayTracker(TextDisplay display, BukkitTask task, String lastText, long refreshEveryRuns) {
            this.display = display;
            this.task = task;
            this.lastText = lastText;
            this.refreshEveryRuns = refreshEveryRuns;
        }
    }
}
