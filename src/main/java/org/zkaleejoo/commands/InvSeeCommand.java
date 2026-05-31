package org.zkaleejoo.commands;

import org.bukkit.Bukkit;
import org.zkaleejoo.utils.InspectionInventoryBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.zkaleejoo.MaxStaff;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.managers.InventorySnapshotManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.zkaleejoo.utils.MaxStaffHolder;
import org.zkaleejoo.utils.MessageUtils;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class InvSeeCommand implements CommandExecutor, TabCompleter {

    private static final int SNAPSHOT_GUI_SIZE = InspectionInventoryBuilder.getInventorySize();

    private final MaxStaff plugin;

    public InvSeeCommand(MaxStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MainConfigManager config = plugin.getMainConfigManager();

        if (!(sender instanceof Player staff)) {
            sender.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getMsgConsole()));
            return true;
        }

        boolean canViewInventory = staff.hasPermission("maxstaff.invsee");
        boolean canEditOnlineInventory = canViewInventory;
        if (!canViewInventory && !canEditOnlineInventory) {
            staff.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getNoPermission()));
            return true;
        }

        if (args.length < 1) {
            staff.sendMessage(
                    MessageUtils.getColoredMessage(config.getPrefix() + plugin.getMainConfigManager().getInvseeUse()));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null) {
            if (staff.getUniqueId().equals(target.getUniqueId())) {
                staff.sendMessage(MessageUtils
                        .getColoredMessage(config.getPrefix() + plugin.getMainConfigManager().getInvseeSelf()));
                return true;
            }

            Inventory inspection = InspectionInventoryBuilder.createOnlineInspection(
                    "INVSEE_ONLINE",
                    target,
                    config.getInvseeInspectionOnlineTitle().replace("{player}", target.getName()),
                    config,
                    canEditOnlineInventory);
            staff.openInventory(Objects.requireNonNull(inspection));
            staff.sendMessage(MessageUtils
                    .getColoredMessage(config.getPrefix() + plugin.getMainConfigManager().getInvseeCheck()));
            return true;
        }

        if (!canViewInventory) {
            staff.sendMessage(MessageUtils.getColoredMessage(config.getPrefix() + config.getMsgOffline()));
            return true;
        }

        openOfflineSnapshot(staff, args[0], config);
        return true;
    }

    private void openOfflineSnapshot(Player staff, String requestedName, MainConfigManager config) {
        InventorySnapshotManager snapshotManager = plugin.getInventorySnapshotManager();
        InventorySnapshotManager.InventorySnapshot snapshot = snapshotManager.loadSnapshotByName(requestedName)
                .orElse(null);

        if (snapshot == null) {
            staff.sendMessage(
                    MessageUtils.getColoredMessage(config.getPrefix() + config.getInvseeOfflineUnavailable()));
            return;
        }

        MaxStaffHolder holder = new MaxStaffHolder("INVSEE_OFFLINE", snapshot.playerName());
        Inventory inventory = Bukkit.createInventory(holder, SNAPSHOT_GUI_SIZE,
                LegacyComponentSerializer.legacySection().deserialize(
                        MessageUtils.getColoredMessage(
                                config.getInvseeInspectionOfflineTitle().replace("{player}", snapshot.playerName()))));
        holder.setInventory(inventory);

        inventory.setContents(createEmptyContents(SNAPSHOT_GUI_SIZE));

        InspectionInventoryBuilder.fillStorage(inventory, snapshot.storage());
        InspectionInventoryBuilder.fillArmor(inventory, snapshot.armor(), config);
        InspectionInventoryBuilder.setItemOrPlaceholder(inventory,
                InspectionInventoryBuilder.getOffhandSlot(config, inventory), snapshot.offhand(),
                config.getInspectOffhandLabel(), config, "invsee-inspection.layout.offhand-slot");
        InspectionInventoryBuilder.setItemOrPlaceholder(inventory,
                InspectionInventoryBuilder.getMainhandSlot(config, inventory), snapshot.mainHand(),
                config.getInspectMainhandLabel(), config, "invsee-inspection.layout.mainhand-slot");

        staff.openInventory(inventory);

        String formattedDate = snapshot.updatedAt() <= 0
                ? "unknown"
                : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(snapshot.updatedAt()));

        staff.sendMessage(MessageUtils.getColoredMessage(config.getPrefix()
                + config.getInvseeOfflineLoaded()
                        .replace("{player}", snapshot.playerName())
                        .replace("{date}", formattedDate)));

        if (!snapshot.hasMainHandData()) {
            staff.sendMessage(
                    MessageUtils.getColoredMessage(config.getPrefix() + config.getInvseeLegacyMainHandNotice()));
        }
    }

    private ItemStack[] createEmptyContents(int size) {
        return new ItemStack[size];
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("maxstaff.invsee") && !sender.hasPermission("maxstaff.revive")) {
            return Collections.emptyList();
        }

        if (args.length != 1) {
            return Collections.emptyList();
        }

        return CommandContextUtil.filterOnlinePlayerNamesByPrefix(args[0]);
    }
}
