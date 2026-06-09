package org.zkaleejoo.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.AbstractChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

public class ChatListener implements Listener {

    private final MaxStaff plugin;

    public ChatListener(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncChatEvent event) {
        if (handleMuteCheck(event.getPlayer(), event)) {
            return;
        }

        handleStaffChatToggle(event.getPlayer(), event);
    }

    private boolean handleMuteCheck(Player player, Cancellable event) {
        if (plugin.isModuleEnabled("punishments") && plugin.getPunishmentManager().isMuted(player.getUniqueId())) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getPlayerMuted())));
            return true;
        }

        if (plugin.isModuleEnabled("chat") && plugin.getChatManager().isGlobalMute()) {
            if (!player.hasPermission("maxstaff.staffchat")) {
                event.setCancelled(true);
                String msg = plugin.getMainConfigManager().getMsgChatIsMuted();
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(MessageUtils.getColoredMessage(
                        plugin.getMainConfigManager().getPrefix() + msg)));
                return true;
            }
        }

        return false;
    }

    private boolean handleStaffChatToggle(Player player, Cancellable event) {
        if (!player.hasPermission("maxstaff.staffchat") || !plugin.isStaffChatToggled(player.getUniqueId())) {
            return false;
        }

        String message = extractMessage(event);
        if (message == null || message.isBlank()) {
            return false;
        }

        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> sendStaffMessage(player, message));
        return true;
    }

    private void sendStaffMessage(Player sender, String message) {
        String formattedMessage = plugin.getMainConfigManager().getStaffChatFormat()
            .replace("{player}", sender.getName())
            .replace("{message}", message);

        String coloredMessage = MessageUtils.getColoredMessage(formattedMessage);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null) continue;
            if (online.hasPermission("maxstaff.staffchat")) {
                online.sendMessage(coloredMessage);
            }
        }

        Bukkit.getConsoleSender().sendMessage(coloredMessage);
    }

    private String extractMessage(Cancellable event) {
        if (event instanceof AbstractChatEvent modernEvent) {
            return PlainTextComponentSerializer.plainText().serialize(modernEvent.originalMessage());
        }

        return null;
    }
}
