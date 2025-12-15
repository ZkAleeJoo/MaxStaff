package org.zkaleejoo.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

public class ChatListener implements Listener {

    private final MaxStaff plugin;

    public ChatListener(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (plugin.getPunishmentManager().isMuted(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MessageUtils.getColoredMessage("&c¡Estás muteado! No puedes hablar."));
        }
    }
}