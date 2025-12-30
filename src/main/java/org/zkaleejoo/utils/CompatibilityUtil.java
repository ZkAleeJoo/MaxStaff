package org.zkaleejoo.utils;

import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import java.lang.reflect.Method;

public class CompatibilityUtil {


    public static String getInventoryTitle(InventoryEvent event) {
    try {
        Object view = event.getView();
        Method getTitle = view.getClass().getMethod("getTitle");
        return (String) getTitle.invoke(view);
    } catch (Exception e) {
        return event.getInventory().getType().getDefaultTitle(); 
        }
    }

    public static Inventory getTopInventory(InventoryEvent event) {
        try {
            Object view = event.getView();
            Method getTopInventory = view.getClass().getMethod("getTopInventory");
            return (Inventory) getTopInventory.invoke(view);
        } catch (Exception e) {
            return event.getInventory();
        }
    }
}