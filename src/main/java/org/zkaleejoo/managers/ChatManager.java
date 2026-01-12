package org.zkaleejoo.managers;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

public class ChatManager {
    private final MaxStaff plugin;
    private boolean globalMute = false;

    public ChatManager(MaxStaff plugin) {
        this.plugin = plugin;
    }

    public boolean isGlobalMute() {
        return globalMute;
    }

    public void setGlobalMute(boolean status) {
        this.globalMute = status;
    }

    public void clearChat(CommandSender sender) {
        for (int i = 0; i < 100; i++) {
            Bukkit.broadcastMessage(" ");
        }
        
        String msg = plugin.getMainConfigManager().getMsgChatCleared()
                .replace("{player}", sender.getName());
        Bukkit.broadcastMessage(MessageUtils.getColoredMessage(msg));
    }
}