# MaxStaff

MaxStaff is a staff-management plugin for modern Minecraft servers. It provides staff mode, vanish, staff chat, command spy, punishments, punishment GUIs, reports, freeze tools, invsee, death-inventory restore, client detection, Anti Xray alerts, Discord webhooks, PlaceholderAPI support, and local or MySQL-backed punishment storage.

## Requirements

| Requirement | Details |
| --- | --- |
| Server | Paper/Spigot compatible with `api-version: 1.21` |
| Java | Java 21 or newer |
| Optional dependency | PlaceholderAPI for placeholders |
| Bundled libraries | bStats, HikariCP, MySQL Connector/J |

## Installation

1. Place the MaxStaff JAR file in your server's `plugins/` folder.
2. Restart the server to generate `config.yml`, `discord.yml`, `data.yml`, `staff_data.yml`, and the other data files.
3. Configure `config.yml` and `discord.yml`.
4. Use `/maxstaff reload` after making configuration changes while the server is running.

## Main Files

| File | Purpose |
| --- | --- |
| `config.yml` | General settings, modules, dynamic permissions, GUI settings, staff mode, reports, database, Anti Xray, and punishments. |
| `discord.yml` | Discord webhooks and embeds for punishments, reports, chat actions, freeze actions, staff mode, and Anti Xray. |
| `lang/messages_en.yml` | English messages. |
| `lang/messages_es.yml` | Spanish messages. |
| `data.yml` | Local punishment data when MySQL is disabled. |
| `staff_data.yml` | Staff mode data, saved inventories, and persistent staff states. |
| `reports.yml` | Saved reports when `reports.store-reports` is enabled. |

## Modules

Modules are controlled from the `modules` section in `config.yml`.

| Module | Default | Purpose |
| --- | --- | --- |
| `staff-mode` | `true` | Enables `/staff`, `/vanish`, staff items, vanish protection, and staff mode events. |
| `command-spy` | `true` | Enables `/cmdspy` and command-spy logging. |
| `chat` | `true` | Enables `/chat`, staff chat, and global chat mute handling. |
| `gamemode-gui` | `true` | Enables `/gamemode` and `/gm`. |
| `alts` | `true` | Enables `/alts` and IP-based alt account lookup. |
| `sanctions-gui` | `true` | Enables `/sanction` and punishment menus. Requires `punishments`. |
| `freeze` | `true` | Enables `/freeze`, `/unfreeze`, `/ss`, `/uss`, and frozen-player restrictions. |
| `reports` | `true` | Enables `/report`, report cooldowns, report storage, and staff notifications. |
| `invsee` | `true` | Enables `/invsee` and online/offline inventory inspection. |
| `revive` | `true` | Enables `/revive` and recent-death inventory snapshots. |
| `punishments` | `true` | Enables bans, mutes, kicks, warns, history, silent punishments, and punishment listeners. |
| `client-tracker` | `true` | Detects player client brands and notifies staff. |
| `anti-xray` | `false` | Enables Anti Xray alerts and `/xray`. |

### Releasing MaxStaff Commands

`modules.disable-commands` lets MaxStaff release commands or aliases so another plugin can own them. By default:

```yaml
modules:
  disable-commands:
    - fly
    - gamemode
    - gm
    - invsee
```

If a command is listed there, MaxStaff attempts to unregister it even if the related module is enabled. Remove a command from that list if you want MaxStaff to handle it.

## Features

### Staff Mode

`/staff` toggles staff mode. When enabled, MaxStaff saves the staff member's inventory and game mode, gives configured staff tools, and applies staff-specific protections.

Default staff tools:

| Tool | Default slot | Purpose |
| --- | ---: | --- |
| Punish | `0` | Opens the target player's information and punishment menu. |
| Freeze | `1` | Freezes or unfreezes players. |
| Players | `4` | Opens the online players menu. |
| Random TP | `5` | Teleports to a random online player. |
| Wall Compass | `6` | Teleports through blocks or to a visible destination. |
| Inspect | `7` | Inspects players or containers. |
| Vanish | `8` | Toggles vanish. |

Important options:

- `staff-mode.combat.allow-hit`: allows or blocks hits while in staff mode.
- `staff-mode.containers.allow-item-move`: allows or blocks item movement in inspected containers.
- `staff-mode.vanish.persist-across-servers`: persists vanish across servers with MySQL, or locally with YAML storage.

### Vanish

`/vanish` hides staff members from players without `maxstaff.see.vanish`. The vanish state can persist depending on configuration and can be enabled automatically on join with `maxstaff.vanish.join`.

### Staff Chat

`/sc <message>` sends a message to staff chat. If a player runs `/sc` without arguments, MaxStaff toggles staff-chat mode so their normal chat messages are sent to staff chat.

### Command Spy

`/cmdspy` toggles command spying. Commands listed in `command-spy.sensitive-commands` have their arguments masked unless the staff member has the permission configured in `command-spy.sensitive-bypass-permission`.

### Global Chat Management

`/chat clear` clears global chat. `/chat mute` toggles global mute; players without `maxstaff.staffchat` cannot speak while global mute is active.

### Punishments

MaxStaff supports bans, tempbans, IP bans, mutes, tempmutes, kicks, warns, history menus, GUI-based punishments, and silent punishments.

Accepted duration tokens:

- `s`: seconds
- `m`: minutes
- `h`: hours
- `d`: days
- `w`: weeks
- `perm`, `permanent`, `permanently`, or `permanentemente`: permanent

Examples:

```text
/ban Steve 7d Xray
/mute Steve 1h Spam
/kick Steve Bad behavior
/warn Steve Toxicity
/ban-ip Steve 30d Multiaccounts
/silent ban Steve 7d Xray
```

Important options:

- `punishments.broadcast`: broadcasts punishments globally.
- `punishments.broadcasts.warns.thresholds`: runs automatic commands when a player reaches configured warning counts.
- `punishments.section-limits.enabled`: limits punishment durations by permission group.
- `maxstaff.punish.protected`: protects a player from lower staff punishment actions.
- `maxstaff.punish.override`: bypasses punishment protection.

### Punishment GUI

`/sanction <player>` opens the advanced user menu with stats, history, punishment actions, alts, inventory inspection, and active permissions. `/sanction list` opens the active punishments menu.

Punishment reasons, materials, and GUI durations are configured in `punishment-reasons`.

### Freeze

`/freeze <player>` or `/ss <player>` freezes or toggles a target's frozen state. `/unfreeze <player>` or `/uss <player>` unfreezes the target. Players with `maxstaff.admin` or `maxstaff.freeze` cannot be frozen.

Important behavior:

- Frozen players are restricted from normal movement and interactions.
- The freeze display uses configurable TextDisplay lines.
- `staff-mode.items.freeze.ban-on-disconnect` can automatically ban players who disconnect while frozen.

### Reports

`/report <player> <reason>` creates a report with cooldowns, reason validation, optional online-target requirement, self-report blocking, Discord logging, report storage, and clickable staff notifications.

Important options:

- `reports.cooldown-seconds`
- `reports.cooldown-bypass-permission`
- `reports.require-online-target`
- `reports.allow-self-report`
- `reports.min-reason-length`
- `reports.max-reason-length`
- `reports.store-reports`
- `reports.notify.permission`

### InvSee

`/invsee <player>` opens an inventory inspection. If the target is online, changes can be synchronized to the target inventory. If the target is offline, MaxStaff attempts to load the last saved inventory snapshot.

Inventory snapshots are saved when players leave and when the plugin shuts down.

### Revive

`/revive` opens a recent-deaths menu. Staff can restore inventory, armor, offhand item, and experience to an online player from a death snapshot.

Options:

- `inventory-snapshots.death.max-age-minutes`
- `inventory-snapshots.death.cleanup-interval-minutes`
- `revive.menu-size`
- `revive.page-size`

### GameMode

`/gamemode` or `/gm` opens the game mode GUI when `gui-gamemode.enabled-menu` is enabled. Direct mode changes are also supported:

```text
/gm 0
/gm survival
/gm s
/gm 1
/gm creative
/gm c
/gm 2
/gm adventure
/gm a
/gm 3
/gm spectator
/gm sp
```

### Fly

`/fly` toggles flight. `/fly <1|2|3>` enables flight with the configured speed level:

- `fly.level-1-speed`
- `fly.level-2-speed`
- `fly.level-3-speed`

Minecraft limits flight speed between `0.1` and `1.0`.

### Alts

`/alts <player>` opens a menu of accounts related by registered IP address.

### Client Tracker

Client Tracker detects player client brands through plugin messaging channels and notifies staff with `maxstaff.client.notify`. Custom signatures can be configured in `client-tracker.client-signatures`.

### Anti Xray

When `anti-xray` is enabled, MaxStaff monitors configured ores and alerts staff when a player reaches material, total, or session thresholds.

Important options:

- `anti-xray.alert-blocks`
- `anti-xray.rate.window-seconds`
- `anti-xray.rate.material-threshold`
- `anti-xray.rate.total-threshold`
- `anti-xray.rate.session-threshold`
- `anti-xray.notify.permission`
- `anti-xray.bypass-permission`
- `anti-xray.notify.cooldown-seconds`

`/xray` opens the suspects menu if the module is enabled.

### Discord Webhooks

`discord.yml` can send embeds for:

- Punishments: ban, mute, kick, warn, and IP ban.
- Staff actions: freeze, unfreeze, chat clear, global chat mute/unmute, staff mode, and Anti Xray.
- Reports.

Each section supports `enabled`, `username`, `avatar-url`, `content`, `author`, `title`, `description`, `color`, `thumbnail`, `image`, `footer`, `timestamp`, and `fields`.

### Update Check And bStats

From `general`:

- `update-check`: checks for updates on startup and every hour.
- `bstats`: enables anonymous bStats metrics.

## Commands

| Command | Aliases | Permission | Purpose |
| --- | --- | --- | --- |
| `/maxstaff` | `/ms` | `maxstaff.admin` | Main plugin command. |
| `/maxstaff help` | `/ms help` | `maxstaff.admin` | Shows help. |
| `/maxstaff reload` | `/ms reload` | `maxstaff.admin` | Reloads configuration, messages, commands, and listeners. |
| `/maxstaff cleanupbans` | `/ms cleanupbans` | `maxstaff.admin` | Cleans expired punishments in MySQL mode. |
| `/maxstaff reset <player> <BAN|MUTE|KICK|WARN|ALL>` | `/ms reset` | `maxstaff.admin` | Resets punishment history. |
| `/maxstaff take <player> <BAN|MUTE|KICK|WARN> [amount]` | `/ms take` | `maxstaff.admin` | Removes entries from punishment history. |
| `/maxstaff debugperm <player> <mute|ban|kick|warn>` | `/ms debugperm` | `maxstaff.admin` | Diagnoses punishment permissions for a player. |
| `/staff` | `/s`, `/staffmode` | `maxstaff.command.staff` | Toggles staff mode. |
| `/vanish` | - | `maxstaff.vanish` | Toggles vanish. |
| `/sc [message]` | `/staffchat` | `maxstaff.staffchat` | Sends a message or toggles staff chat mode. |
| `/cmdspy` | - | `maxstaff.cmdspy` | Toggles command spy. |
| `/chat clear` | - | `maxstaff.chat.admin` | Clears global chat. |
| `/chat mute` | - | `maxstaff.chat.admin` | Toggles global chat mute. |
| `/gamemode [mode]` | `/gm` | `maxstaff.gamemode` | Opens the GUI or changes game mode. |
| `/alts <player>` | `/alt`, `/accs` | `maxstaff.alts` | Shows related accounts. |
| `/sanction <player>` | - | `maxstaff.punish` | Opens the punishment/info GUI. |
| `/sanction list` | - | `maxstaff.sanctions.list` | Lists active punishments. |
| `/freeze <player>` | `/ss` | `maxstaff.freeze` | Freezes or toggles the target. |
| `/unfreeze <player>` | `/uss` | `maxstaff.freeze` | Unfreezes the target. |
| `/report <player> <reason>` | `/reportar` | `maxstaff.report` | Sends a player report. |
| `/invsee <player>` | - | `maxstaff.invsee` | Inspects online/offline inventory. |
| `/revive` | - | `maxstaff.revive` | Opens the death-restore menu. |
| `/fly [1|2|3]` | - | `maxstaff.fly` | Toggles flight or changes speed. |
| `/xray` | - | `maxstaff.antixray.alert` | Opens the Anti Xray menu. |
| `/ban <player> [time] [reason]` | - | `maxstaff.punish.ban` | Bans a player. |
| `/tempban <player> [time] [reason]` | - | `maxstaff.punish.ban` | Temporarily bans a player. |
| `/mute <player> [time] [reason]` | - | `maxstaff.punish.mute` | Mutes a player. |
| `/tempmute <player> [time] [reason]` | - | `maxstaff.punish.mute` | Temporarily mutes a player. |
| `/kick <player> [reason]` | - | `maxstaff.punish.kick` | Kicks a player. |
| `/warn <player> [reason]` | - | `maxstaff.punish.warn` | Adds a warning. |
| `/unban <player>` | - | `maxstaff.punish.unban` | Removes a ban. |
| `/unmute <player>` | - | `maxstaff.punish.unmute` | Removes a mute. |
| `/ban-ip <player/ip> [time] [reason]` | - | `maxstaff.punish.banip` | Bans an IP. |
| `/tempban-ip <player/ip> [time] [reason]` | - | `maxstaff.punish.banip` | Temporarily bans an IP. |
| `/unban-ip <ip>` | - | `maxstaff.punish.unbanip` | Removes an IP ban. |
| `/history <player>` | - | `maxstaff.history` | Opens punishment history. |
| `/silent <action> <player/ip> [time] [reason]` | - | `maxstaff.punish.silent` | Runs a punishment without broadcast/chat output. |

## Permissions

`maxstaff.admin` grants administrative access and includes most operational plugin permissions.

| Permission | Default | Purpose |
| --- | --- | --- |
| `maxstaff.admin` | `op` | Full administrative access. |
| `maxstaff.command.staff` | `op` | Allows `/staff`. |
| `maxstaff.mode` | `op` | Legacy alias that includes `maxstaff.command.staff`. |
| `maxstaff.vanish` | `op` | Allows `/vanish`. |
| `maxstaff.vanish.join` | `false` | Automatically enables vanish on join. |
| `maxstaff.see.vanish` | `op` | Allows seeing vanished players. |
| `maxstaff.staffchat` | `op` | Allows using and viewing staff chat. |
| `maxstaff.cmdspy` | `op` | Allows command spy. |
| `maxstaff.cmdspy.raw` | `false` | Shows sensitive command arguments without masking. |
| `maxstaff.cmdspy.raw.owners` | `op` | Group node that includes `maxstaff.cmdspy.raw`. |
| `maxstaff.chat.admin` | `op` | Allows `/chat clear` and `/chat mute`. |
| `maxstaff.gamemode` | `op` | Allows `/gm` and the game mode GUI. |
| `maxstaff.alts` | `op` | Allows `/alts`. |
| `maxstaff.punish` | `op` | Allows `/sanction <player>`. |
| `maxstaff.sanctions.list` | `op` | Allows `/sanction list`. |
| `maxstaff.freeze` | `op` | Allows `/freeze` and `/unfreeze`; also protects from freeze. |
| `maxstaff.report` | `true` | Allows `/report`. |
| `maxstaff.report.notify` | `op` | Receives live report notifications. |
| `maxstaff.report.bypass` | `op` | Bypasses report cooldowns. |
| `maxstaff.invsee` | `op` | Allows `/invsee`. |
| `maxstaff.revive` | `op` | Allows `/revive`. |
| `maxstaff.fly` | `op` | Allows `/fly`. |
| `maxstaff.history` | `op` | Allows `/history`. |
| `maxstaff.punish.ban` | `op` | Allows `/ban` and `/tempban`. |
| `maxstaff.punish.mute` | `op` | Allows `/mute` and `/tempmute`. |
| `maxstaff.punish.kick` | `op` | Allows `/kick`. |
| `maxstaff.punish.warn` | `op` | Allows `/warn`. |
| `maxstaff.punish.unban` | `op` | Allows `/unban`. |
| `maxstaff.punish.unmute` | `op` | Allows `/unmute`. |
| `maxstaff.punish.banip` | `op` | Allows `/ban-ip` and `/tempban-ip`. |
| `maxstaff.punish.unbanip` | `op` | Allows `/unban-ip`. |
| `maxstaff.punish.silent` | `op` | Allows `/silent`. |
| `maxstaff.punish.override` | `op` | Bypasses punishment protection. |
| `maxstaff.punish.protected` | `false` | Protects a player from punishment actions by staff without override. |
| `maxstaff.client.notify` | `op` | Receives client detection notifications. |
| `maxstaff.antixray.alert` | `op` | Receives Anti Xray alerts and allows `/xray`. |
| `maxstaff.antixray.bypass` | `false` | Prevents the player from generating Anti Xray alerts. |

Example dynamic group permissions for punishment limits:

| Permission | Purpose |
| --- | --- |
| `maxstaff.groups.helper` | Example helper group limit permission. |
| `maxstaff.groups.mod` | Example moderator group limit permission. |

These can be changed in `punishments.section-limits.groups`.

## PlaceholderAPI

If PlaceholderAPI is installed, MaxStaff registers the `maxstaff` expansion.

| Placeholder | Result |
| --- | --- |
| `%maxstaff_in_staff_mode%` | `status-true` if the player is in staff mode; otherwise `status-false`. |
| `%maxstaff_vanished%` | Player vanish state. |
| `%maxstaff_frozen%` | Player freeze state. |
| `%maxstaff_is_spy%` | Player command-spy state. |
| `%maxstaff_warn_count%` | Warning count from punishment history. |
| `%maxstaff_ban_count%` | Ban count from punishment history. |
| `%maxstaff_mute_count%` | Mute count from punishment history. |
| `%maxstaff_kick_count%` | Kick count from punishment history. |
| `%maxstaff_total_punishments%` | Total bans, mutes, and kicks. |
| `%maxstaff_playtime%` | Online player playtime in `h m` format. |

Boolean output text is configured in:

```yaml
placeholders:
  status-true: "..."
  status-false: "..."
```

## Internal Placeholders

These placeholders are used inside plugin messages, GUI text, Discord embeds, or configurable command templates.

### Reports

| Placeholder | Purpose |
| --- | --- |
| `{staff}` | Staff member receiving or running a clickable action. |
| `{reporter}` | Reporting player. |
| `{target}` | Reported player. |
| `{world}` | World where the report was created. |
| `{x}` / `{y}` / `{z}` | Report coordinates. |
| `{reason}` | Report reason. |
| `{id}` | Internal report ID. |
| `{sequence}` | Sequential report number. |
| `{seconds}` | Remaining cooldown. |
| `{min}` / `{max}` | Minimum or maximum reason length. |

### Punishments

| Placeholder | Purpose |
| --- | --- |
| `{target}` | Punished player. |
| `{staff}` | Staff member or console sender. |
| `{reason}` | Punishment reason. |
| `{duration}` | Punishment duration. |
| `{count}` | Accumulated warning count. |
| `{type}` | Punishment type. |
| `{max}` | Maximum allowed duration for the group. |
| `{command}` | Command label used in usage messages. |

### Freeze Display

| Placeholder | Purpose |
| --- | --- |
| `{name}` | Frozen player. |
| `{bans}` | Ban history count. |
| `{mutes}` | Mute history count. |
| `{kicks}` | Kick history count. |
| `{total}` | Total count used by the risk profile. |
| `{risk_label}` | Risk label. |
| `{risk_color}` | Risk color. |
| `{risk_bar}` | Visual risk bar. |

### Anti Xray

| Placeholder | Purpose |
| --- | --- |
| `{player}` | Suspected player. |
| `{mineral}` | Detected ore. |
| `{world}` | World. |
| `{x}` / `{y}` / `{z}` | Coordinates. |
| `{rate}` | Session total for that ore. |
| `{window_rate}` | Ore count inside the time window. |
| `{window_total}` | Total monitored ores inside the time window. |
| `{window_seconds}` | Time window length. |

### GUI And Menus

| Placeholder | Purpose |
| --- | --- |
| `{player}` | Player shown in the menu. |
| `{page}` / `{total}` | Pagination. |
| `{count}` | Record count. |
| `{death-cause}` / `{cause}` | Death cause for revive. |
| `{date}` | Offline snapshot date. |
| `{mode}` | Applied game mode. |
| `{level}` / `{speed}` | Flight level and speed. |
| `{permission}` | Permission shown in the permissions menu. |

### Discord

In addition to action-specific placeholders, `discord.yml` supports:

| Placeholder | Purpose |
| --- | --- |
| `{server}` | Name configured in `server-name`. |
| `{timestamp}` | Timestamp generated by MaxStaff. |
| `{face}` | Related player's face/avatar URL. |

## Database

MaxStaff can store punishment data in two modes:

| Mode | Setting | Recommended use |
| --- | --- | --- |
| Local YAML | `database.enabled: false` | Single backend/server setups. |
| MySQL | `database.enabled: true` | Networks or multiple servers that need synchronized punishments and vanish state. |

MySQL mode uses HikariCP and MySQL Connector/J. `database.server-id` is the stable ID for this server inside the network.

Note: the default `config.yml` warns that migrating from older local-storage versions to MySQL may leave sanction history blank.

## Recommended Permission Setups

### Basic Staff

```text
maxstaff.command.staff
maxstaff.vanish
maxstaff.see.vanish
maxstaff.staffchat
maxstaff.cmdspy
maxstaff.freeze
maxstaff.report.notify
maxstaff.client.notify
```

### Punishment Moderator

```text
maxstaff.punish
maxstaff.punish.ban
maxstaff.punish.mute
maxstaff.punish.kick
maxstaff.punish.warn
maxstaff.history
maxstaff.sanctions.list
```

### Regular Players

```text
maxstaff.report
```

`maxstaff.report` is already `default: true`.

## In-Server Testing Checklist

After installing or changing configuration, test:

1. `/maxstaff reload`
2. `/staff` to verify inventory save and restore.
3. `/vanish` with a player who does not have `maxstaff.see.vanish`.
4. `/sc` and `/sc message`.
5. `/cmdspy` with normal and sensitive commands.
6. `/report <player> <reason>` with cooldown behavior.
7. `/freeze <player>` and frozen-player disconnect behavior if ban-on-disconnect is enabled.
8. `/sanction <player>` and `/sanction list`.
9. `/ban`, `/mute`, `/warn`, `/history`, and `/silent`.
10. `/invsee <player>` online and offline.
11. `/revive` after a recent death.
12. `/xray` if `anti-xray` is enabled.
13. PlaceholderAPI placeholders through a compatible plugin.
