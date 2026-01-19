package org.zkaleejoo.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.zkaleejoo.MaxStaff;

public class PlayerQuitListener implements Listener {

    private final MaxStaff plugin;

    public PlayerQuitListener(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
    }

    
}