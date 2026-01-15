Copyright © 2025 ZkAleeJoo. All rights reserved. This code is viewable for educational purposes, but its use, modification, or distribution is not permitted without explicit permission.

# **MaxStaff**

**MaxStaff** It is a robust moderation suite designed specifically for Survival environments in Minecraft 1.19+. It offers a high-performance interface for staff members to manage players and maintain server integrity without the need for multiple external plugins.

---

### **Main Features**

#### **Integrated Staff Mode**
Activating Staff Mode provides a specialized environment for administrators:
* **Inventory Management**: Automatically saves and restores the player's inventory and armor upon entering or exiting the mode.
* **Operational Utilities**: Enables flight, invulnerability, and interaction tools compatible with survival mode.
* **Vanish System**: Built-in invisibility functionality for monitoring players undetected.

#### **Advanced Sanctions System**
A logical, data-driven system that handles sanctions with precision:
* **Multi-Type Sanctions**: Full support for Bans, Mutes, Kicks, and Warns.
* **Persistent History**: Infraction storage accessible via GUI for each player.
* **Automated Thresholds**: Execution of configurable commands when a player reaches a specific number of warnings.
* **Duration Processing**: Supports flexible time strings (e.g., `1h`, `7d`, `perm`) converted into precise expirations.

#### **Visual Intuition (GUIs)**
Designed for speed, the plugin uses a hierarchical menu system:
* **User Information**: View detailed statistics such as playtime, UUID, and total penalties.
* **Motif Selection**: Paginated menu to select motifs and predefined durations with a single click.
* **Player Navigator**: Head-based visual menu for quick teleportation to online players.

#### **Inspection and Interaction Tools**
* **Freeze Mechanics**: Blocks a player's movement, interactions, and block manipulation during investigations.
* **Silent Inspection**: Allows you to view and modify inventories or containers (chests, ender chests) silently without animations.

---

### **Technical Specifications**

```text
+-----------------+---------------------------------------+
| Requirement     | Specification                         |
+-----------------+---------------------------------------+
| Java version    | Java 17                               |
| Minimum API     | Spigot API 1.19                       |
| Configuration   | Based on YAML (CustomConfig API)      |
| Data Management | Persistent Data Containers            |
+-----------------+---------------------------------------+
```

### **Commands and Permissions**
```yaml
/maxstaff reload - maxstaff.admin
/maxstaff mode - maxstaff.mode
/maxstaff help - maxstaff.mode o maxstaff.admin
/maxstaff reset - maxstaff.admin
/maxstaff take - maxstaff.admin
/ban - maxstaff.punish.ban
/tempban - maxstaff.punish.ban
/mute - maxstaff.punish.mute
/tempmute - maxstaff.punish.mute
/kick - maxstaff.punish.kick
/warn - maxstaff.punish.warn
/unban - maxstaff.punish.unban
/unmute - maxstaff.punish.unmute
/history - maxstaff.history
/vanish - maxstaff.vanish
/sc - maxstaff.staffchat
/cmdspy - maxstaff.cmdspy
/chat [mute] [clear] - maxstaff.chat.admin
/gm - maxstaff.gamemode
/ban-ip - maxstaff.punish.banip
/tempban-ip - maxstaff.punish.banip
/unban-ip - maxstaff.punish.unbanip

maxstaff.admin - It gives you access to the entire plugin
maxstaff.see.vanish - It allows you to see other staff on Vanish
```

### **Placeholders**
```yaml
%maxstaff_in_staff_mode%      - Returns whether the staff member is in staff mode.
%maxstaff_vanished%          - Returns whether the staff member is vanished.
%maxstaff_frozen%            - Returns whether the player is frozen.
%maxstaff_is_spy%            - Returns whether the staff member has CommandSpy active.
%maxstaff_warn_count%        - Number of warnings for the player.
%maxstaff_ban_count%         - Number of bans in the history.
%maxstaff_mute_count%        - Number of mutes in the history.
%maxstaff_kick_count%        - Number of kicks in the history.
%maxstaff_total_punishments% - Total sum of sanctions (Ban + Mute + Kick).
%maxstaff_playtime%          - Formatted playtime (H and M).
```

---

### **Configuration and Localization**
**MaxStaff** is fully customizable through the `config.yml` files and the language files in the `lang/` folder:
* **Dynamic Materials**: Choose any Minecraft material for the GUI borders and staff tools.
* **Language Support**: Includes pre-configured templates for **English** (`en`) and **Spanish** (`es`).
* **Update Checker**: Automatic notifications for administrators.
