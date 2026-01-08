package org.zkaleejoo.nms;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public interface NMSHandler {
    // Para manejar el cambio de InventoryView en 1.21+
    String getInventoryTitle(InventoryClickEvent event);
    
    // Para manejar cambios en visibilidad de jugadores
    void hidePlayer(Player viewer, Player target);
}