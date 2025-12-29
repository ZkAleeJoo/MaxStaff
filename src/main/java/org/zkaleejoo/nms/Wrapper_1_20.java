package org.zkaleejoo.nms;

import org.bukkit.inventory.InventoryView;
import java.lang.reflect.Method;

public class Wrapper_1_20 implements VersionWrapper {
    @Override
    public String getInventoryTitle(InventoryView view) {
        try {
            Method getTitleMethod = view.getClass().getMethod("getTitle");
            return (String) getTitleMethod.invoke(view);
        } catch (Exception e) {
            return "";
        }
    }
}