package org.zkaleejoo.managers;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.CustomConfig;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.TimeUtils;

import java.util.Date;
import java.util.UUID;

public class PunishmentManager {

    private final MaxStaff plugin;
    private CustomConfig dataFile; 

    public PunishmentManager(MaxStaff plugin) {
        this.plugin = plugin;
        this.dataFile = new CustomConfig("data.yml", null, plugin, false);
        this.dataFile.registerConfig();
    }

    // --- KICK ---
    public void kickPlayer(CommandSender staff, String targetName, String reason) {
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            String kickMsg = plugin.getMainConfigManager().getPrefix() + "&cHas sido expulsado.\n&fRazón: &e" + reason;
            target.kickPlayer(MessageUtils.getColoredMessage(kickMsg));
            
            broadcast("&e" + target.getName() + " &fha sido expulsado por &c" + staff.getName() + "&f.");
        } else {
            staff.sendMessage(MessageUtils.getColoredMessage("&cEl jugador no está online."));
        }
    }

    // --- BAN / TEMPBAN ---
    public void banPlayer(CommandSender staff, String targetName, String reason, String durationStr) {
        long duration = TimeUtils.parseDuration(durationStr);
        Date expiry = (duration == -1) ? null : new Date(System.currentTimeMillis() + duration);
        
        Bukkit.getBanList(BanList.Type.NAME).addBan(targetName, reason, expiry, staff.getName());
        
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            String timeMsg = (duration == -1) ? "Permanente" : TimeUtils.getDurationString(duration);
            target.kickPlayer(MessageUtils.getColoredMessage("&cHas sido baneado.\n&fRazón: &e" + reason + "\n&fDuración: &e" + timeMsg));
        }

        String timeDisplay = (duration == -1) ? "Permanente" : TimeUtils.getDurationString(duration);
        broadcast("&c" + targetName + " &fha sido baneado por &c" + staff.getName() + " &f(&e" + timeDisplay + "&f).");
    }

    public void unbanPlayer(CommandSender staff, String targetName) {
        Bukkit.getBanList(BanList.Type.NAME).pardon(targetName);
        staff.sendMessage(MessageUtils.getColoredMessage("&aHas desbaneado a " + targetName));
    }

    // --- MUTE / TEMPMUTE ---
    public void mutePlayer(CommandSender staff, String targetName, String reason, String durationStr) {
        Player target = Bukkit.getPlayer(targetName);
        UUID uuid = (target != null) ? target.getUniqueId() : Bukkit.getOfflinePlayer(targetName).getUniqueId();
        
        long duration = TimeUtils.parseDuration(durationStr);
        long expiry = (duration == -1) ? -1 : System.currentTimeMillis() + duration;

        FileConfiguration data = dataFile.getConfig();
        data.set("mutes." + uuid + ".reason", reason);
        data.set("mutes." + uuid + ".expiry", expiry);
        data.set("mutes." + uuid + ".staff", staff.getName());
        dataFile.saveConfig();

        String timeDisplay = (duration == -1) ? "Permanente" : TimeUtils.getDurationString(duration);
        broadcast("&c" + targetName + " &fha sido muteado por &c" + staff.getName() + " &f(&e" + timeDisplay + "&f).");
        
        if (target != null) {
            target.sendMessage(MessageUtils.getColoredMessage("&c¡Has sido muteado por " + staff.getName() + "!"));
        }
    }

    public void unmutePlayer(CommandSender staff, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        UUID uuid = (target != null) ? target.getUniqueId() : Bukkit.getOfflinePlayer(targetName).getUniqueId();

        FileConfiguration data = dataFile.getConfig();
        if (data.contains("mutes." + uuid)) {
            data.set("mutes." + uuid, null);
            dataFile.saveConfig();
            staff.sendMessage(MessageUtils.getColoredMessage("&aHas desmuteado a " + targetName));
            if (target != null) target.sendMessage(MessageUtils.getColoredMessage("&a¡Ya puedes hablar de nuevo!"));
        } else {
            staff.sendMessage(MessageUtils.getColoredMessage("&cEse jugador no está muteado."));
        }
    }

    public boolean isMuted(UUID uuid) {
        FileConfiguration data = dataFile.getConfig();
        if (!data.contains("mutes." + uuid)) return false;

        long expiry = data.getLong("mutes." + uuid + ".expiry");
        if (expiry != -1 && System.currentTimeMillis() > expiry) {
            data.set("mutes." + uuid, null);
            dataFile.saveConfig();
            return false;
        }
        return true;
    }

    private void broadcast(String msg) {
        Bukkit.broadcastMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + msg));
    }
}