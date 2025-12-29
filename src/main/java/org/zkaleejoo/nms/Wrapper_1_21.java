package org.zkaleejoo.nms;

import org.bukkit.inventory.InventoryView;

public class Wrapper_1_21 implements VersionWrapper {
    @Override
    public String getInventoryTitle(Object view) {
        if (view instanceof InventoryView) {
            return ((InventoryView) view).getTitle();
        }
        return "";
    }
}