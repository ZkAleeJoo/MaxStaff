package org.zkaleejoo.nms.versions;

import org.zkaleejoo.nms.NMSHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.zkaleejoo.MaxStaff;

public class V1_19_Handler implements NMSHandler {
    private final MaxStaff plugin;
    public V1_19_Handler(MaxStaff plugin) { this.plugin = plugin; }

    @Override
    public String getInventoryTitle(InventoryClickEvent event) {
        return event.getView().getTitle();
    }

    @Override
    public void hidePlayer(Player viewer, Player target) {
        viewer.hidePlayer(plugin, target);
    }
}