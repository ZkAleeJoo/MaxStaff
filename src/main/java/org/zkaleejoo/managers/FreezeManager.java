package org.zkaleejoo.managers;

import org.bukkit.entity.Player; 
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FreezeManager {

    private final MaxStaff plugin;
    private final Set<UUID> frozenPlayers = new HashSet<>();

    public FreezeManager(MaxStaff plugin) {
        this.plugin = plugin;
    }

    public void toggleFreeze(Player staff, Player target) {
        if (isFrozen(target)) {
            setFrozen(target, false);
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgUnfreezeStaff().replace("{player}", target.getName())));
        } else {
            setFrozen(target, true);
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgFreezeStaff().replace("{player}", target.getName())));
        }
    }

    public void setFrozen(Player target, boolean freeze) {
        if (freeze) {
            frozenPlayers.add(target.getUniqueId());
            
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
            
            for (String line : plugin.getMainConfigManager().getMsgTargetFrozen()) {
                target.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + line));
            }
            target.closeInventory();
        } else {
            frozenPlayers.remove(target.getUniqueId());
            
            target.removePotionEffect(PotionEffectType.BLINDNESS);
            
            target.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgTargetUnfrozen()));
        }
    }

    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }
}