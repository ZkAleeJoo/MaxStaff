package org.zkaleejoo.utils;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import java.util.HashMap;
import java.util.Map;

public class MaxStaffHolder implements InventoryHolder {
    
    private Inventory inventory;
    private final String menuType; 
    private final String targetName; 
    private final Map<String, Object> data = new HashMap<>(); 

    public MaxStaffHolder(String menuType, String targetName) {
        this.menuType = menuType;
        this.targetName = targetName;
    }

    public void setData(String key, Object value) {
        this.data.put(key, value);
    }

    public Object getData(String key) {
        return this.data.get(key);
    }

    public String getMenuType() { return menuType; }
    public String getTargetName() { return targetName; }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}