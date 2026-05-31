package org.zkaleejoo.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.CustomConfig;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.scheduler.BukkitTask;

public class ReportManager {

    public record ReportRecordMeta(String id, long sequentialIndex, long createdAtEpochMillis, String timestamp) {
    }

    private final MaxStaff plugin;
    private final CustomConfig reportFile;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final AtomicLong reportSequence;
    private final BukkitTask cooldownCleanupTask;

    public ReportManager(MaxStaff plugin) {
        this.plugin = plugin;
        this.reportFile = new CustomConfig("reports.yml", null, plugin, true);
        this.reportFile.registerConfig();
        this.reportSequence = new AtomicLong(resolveStartingSequence(reportFile.getConfig()));
        this.cooldownCleanupTask = startCooldownCleanupTask();
    }

    public long getCooldownRemaining(UUID reporter) {
        int cooldownSeconds = plugin.getMainConfigManager().getReportCooldownSeconds();
        if (cooldownSeconds <= 0) {
            return 0;
        }
        Long last = cooldowns.get(reporter);
        if (last == null) {
            return 0;
        }
        long elapsedSeconds = (System.currentTimeMillis() - last) / 1000;
        long remaining = cooldownSeconds - elapsedSeconds;
        if (remaining <= 0) {
            cooldowns.remove(reporter);
            return 0;
        }
        return Math.max(remaining, 0);
    }

    public void markReported(UUID reporter) {
        cooldowns.put(reporter, System.currentTimeMillis());
    }

    public void clearCooldown(UUID reporter) {
        cooldowns.remove(reporter);
    }

    public ReportRecordMeta recordReport(String reporterName, UUID reporterUuid, String targetName, UUID targetUuid,
            String reason, String world, int x, int y, int z) {
        String id = UUID.randomUUID().toString();
        long sequentialIndex = reportSequence.getAndIncrement();
        long createdAtEpochMillis = System.currentTimeMillis();

        String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        if (!plugin.getMainConfigManager().isReportStorageEnabled()) {
            return new ReportRecordMeta(id, sequentialIndex, createdAtEpochMillis, timestamp);
        }
        FileConfiguration config = reportFile.getConfig();
        String basePath = "reports." + id;

        config.set("reports-meta.next-sequence", reportSequence.get());
        config.set("reports-meta.storage", "yaml");
        config.set("reports-meta.schema-version", 2);
        config.set(basePath + ".reporter", reporterName);
        config.set(basePath + ".reporter-uuid", reporterUuid != null ? reporterUuid.toString() : null);
        config.set(basePath + ".target", targetName);
        config.set(basePath + ".target-uuid", targetUuid != null ? targetUuid.toString() : null);
        config.set(basePath + ".reason", reason);
        config.set(basePath + ".world", world);
        config.set(basePath + ".x", x);
        config.set(basePath + ".y", y);
        config.set(basePath + ".z", z);
        config.set(basePath + ".id", id);
        config.set(basePath + ".sequential-index", sequentialIndex);
        config.set(basePath + ".created-at-epoch-ms", createdAtEpochMillis);
        config.set(basePath + ".timestamp", timestamp);
        reportFile.saveConfig();
        return new ReportRecordMeta(id, sequentialIndex, createdAtEpochMillis, timestamp);
    }

    private long resolveStartingSequence(FileConfiguration config) {
        long nextSequence = config.getLong("reports-meta.next-sequence", -1L);
        if (nextSequence > 0) {
            return nextSequence;
        }

        long maxExistingSequence = 0L;
        if (config.isConfigurationSection("reports")) {
            for (String reportId : config.getConfigurationSection("reports").getKeys(false)) {
                long existingSequence = config.getLong("reports." + reportId + ".sequential-index", -1L);
                if (existingSequence > maxExistingSequence) {
                    maxExistingSequence = existingSequence;
                }
            }
        }
        return maxExistingSequence + 1L;
    }

    private BukkitTask startCooldownCleanupTask() {
        return plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            int cooldownSeconds = plugin.getMainConfigManager().getReportCooldownSeconds();
            if (cooldownSeconds <= 0 || cooldowns.isEmpty()) {
                return;
            }

            long now = System.currentTimeMillis();
            long cooldownMillis = cooldownSeconds * 1000L;
            cooldowns.entrySet().removeIf(entry -> (now - entry.getValue()) >= cooldownMillis);
        }, 20L * 60L, 20L * 60L);
    }

    public void shutdown() {
        if (cooldownCleanupTask != null) {
            cooldownCleanupTask.cancel();
        }
        cooldowns.clear();
    }
}