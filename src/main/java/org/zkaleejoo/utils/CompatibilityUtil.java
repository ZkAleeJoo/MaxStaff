package org.zkaleejoo.utils;

import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import java.lang.reflect.Method;

public class CompatibilityUtil {

    public static String getInventoryTitle(InventoryEvent event) {
        try {
            return event.getView().getTitle();
        } catch (Throwable t) { 
            try {
                Object view = event.getView();
                Method getTitle = view.getClass().getMethod("getTitle");
                return (String) getTitle.invoke(view);
            } catch (Exception ex) {
                return ""; 
            }
        }
    }

    public static Inventory getTopInventory(InventoryEvent event) {
        try {
            return event.getInventory();
        } catch (Throwable t) {
            return null;
        }
    }
}