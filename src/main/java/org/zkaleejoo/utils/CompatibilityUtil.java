package org.zkaleejoo.utils;

import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import java.lang.reflect.Method;

public class CompatibilityUtil {

    public static String getInventoryTitle(InventoryEvent event) {
        try {
            return event.getView().getTitle();
        } catch (NoSuchMethodError | Exception e) {
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
            return event.getView().getTopInventory();
        } catch (NoSuchMethodError | Exception e) {
            try {
                Object view = event.getView();
                Method m = view.getClass().getMethod("getTopInventory");
                return (Inventory) m.invoke(view);
            } catch (Exception ex) {
                return event.getInventory();
            }
        }
    }
}