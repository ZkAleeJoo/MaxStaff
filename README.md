Copyright © 2025 ZkAleeJoo. All rights reserved. This code is viewable for educational purposes, but its use, modification, or distribution is not permitted without explicit permission.

# **MaxStaff**

**MaxStaff** It is a robust moderation suite designed specifically for Survival environments in Minecraft 1.19+. It offers a high-performance interface for staff members to manage players and maintain server integrity without the need for multiple external plugins.

---
![68747470733a2f2f6d65646961312e67697068792e636f6d2f6d656469612f76312e59326c6b505463354d4749334e6a4578625870754d486b3565577872595842775a6a526d656d7470615870704f44686b655756685a6d78344f4452356444466e4d6a6c7662795a6c63](https://github.com/user-attachments/assets/852bfa3d-8199-4488-9200-39d9583cc560)

![68747470733a2f2f6d65646961332e67697068792e636f6d2f6d656469612f76312e59326c6b505463354d4749334e6a4578647a55304d6a5673615456344e4739784e6a4a704d473536596a55354d6a427a62325a78644777794e33517a5958526a4e47787561695a6c63](https://github.com/user-attachments/assets/d80eda73-068e-4692-86ac-18423e6cdb35)

![68747470733a2f2f6d65646961342e67697068792e636f6d2f6d656469612f76312e59326c6b505463354d4749334e6a457859585a315a47513162544e6b616e687a633249314e7a6c7a64326c75597a5a6f65446c6f5a334d7963324e31595770315957643262795a6c63](https://github.com/user-attachments/assets/27c209b6-dd19-43e7-bd0d-7d9fb6c25d7d)

![68747470733a2f2f6d65646961312e67697068792e636f6d2f6d656469612f76312e59326c6b505463354d4749334e6a45785a4864764e48686c6348413063336b34596e6f7a4e7a49314d7a41325a47466d597a687a4e6e6f304e475179646d7469596d74704d695a6c63](https://github.com/user-attachments/assets/f68a77c7-00d7-4f62-9f0c-ab34464250e2)

![68747470733a2f2f6d65646961342e67697068792e636f6d2f6d656469612f76312e59326c6b505463354d4749334e6a45784d7a67326557396a6547527859584d7a4d336c76636d38794d4738305a6a4232656e517a4e446869596d316e5a584a35656d4e7961695a6c63](https://github.com/user-attachments/assets/75110ece-a146-422c-a14c-3acb2a95b892)

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
