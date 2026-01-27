package org.zkaleejoo.managers;

import org.bukkit.Sound;
import org.bukkit.entity.Player; 
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.utils.MessageUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FreezeManager {

    private final MaxStaff plugin;
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private final Map<UUID, ItemStack> savedHelmets = new HashMap<>();

    public FreezeManager(MaxStaff plugin) {
        this.plugin = plugin;
    }

    public void toggleFreeze(Player staff, Player target) {
        if (isFrozen(target)) {
            setFrozen(target, false);
            staff.playSound(staff.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.5f);
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgUnfreezeStaff().replace("{player}", target.getName())));
        } else {
            setFrozen(target, true);
            staff.playSound(staff.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            staff.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgFreezeStaff().replace("{player}", target.getName())));
        }
    }

    public void setFrozen(Player target, boolean freeze) {
        if (freeze) {
            frozenPlayers.add(target.getUniqueId());
            
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
            
            ItemStack currentHelmet = target.getInventory().getHelmet();
            savedHelmets.put(target.getUniqueId(), currentHelmet); 
            
            target.getInventory().setHelmet(new ItemStack(plugin.getMainConfigManager().getMatHealFreeze()));
            
            target.updateInventory();

            for (String line : plugin.getMainConfigManager().getMsgTargetFrozen()) {
                target.sendMessage(MessageUtils.getColoredMessage(line));
            }
            
            target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 2.0f, 0.5f);
            target.closeInventory();

        } else {
            frozenPlayers.remove(target.getUniqueId());
            
            target.removePotionEffect(PotionEffectType.BLINDNESS);
            
            if (savedHelmets.containsKey(target.getUniqueId())) {
                ItemStack original = savedHelmets.remove(target.getUniqueId());
                target.getInventory().setHelmet(original);
            } else {
                target.getInventory().setHelmet(null); 
            }
            target.updateInventory();

            target.sendMessage(MessageUtils.getColoredMessage(plugin.getMainConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgTargetUnfrozen()));
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        }
    }

    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }
    
    public void handleDisconnect(Player player) {
        if (isFrozen(player)) {
            savedHelmets.remove(player.getUniqueId());
            frozenPlayers.remove(player.getUniqueId());
        }
    }
}