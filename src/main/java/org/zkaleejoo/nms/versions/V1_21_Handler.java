package org.zkaleejoo.nms.versions;

import org.zkaleejoo.nms.NMSHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class V1_21_Handler implements NMSHandler {

    @Override
    public String getInventoryTitle(InventoryClickEvent event) {
        return event.getView().getTitle(); 
    }

    @Override
    public void hidePlayer(Player viewer, Player target) {
        viewer.hidePlayer(org.zkaleejoo.MaxStaff.getPlugin(org.zkaleejoo.MaxStaff.class), target);
    }
}