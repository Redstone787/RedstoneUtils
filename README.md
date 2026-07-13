# RedstoneUtils

RedstoneUtils is a client-side Fabric mod for Minecraft 26.2. It provides tools for creative Redstone development, including automatic wire placement, wire previews, Sculk Sensor range analysis, comparator signal block generation, macros, teleport debugging, and an in-game calculator.

The mod is intended for test worlds, creative builds, and rapid iteration. Several features generate or send Minecraft commands such as `/give`, `/setblock`, or `/tp`; on multiplayer servers, the required permissions must be available.

## Project Status

| Field | Value |
| --- | --- |
| Mod ID | `redstoneutils` |
| Mod Name | `RedstoneUtils` |
| Version | `1.0-SNAPSHOT` |
| Minecraft | `26.2` |
| Fabric Loader | `>=0.19.3` |
| Fabric API | `0.154.2+26.2` |
| Java | `25` |
| Environment | Client |
| License | All rights reserved |
| Repository | <https://github.com/johannes/RedstoneUtils> |

## Features

### AutoWire

AutoWire places Redstone components above newly placed support blocks. After you place a block, RedstoneUtils checks the active mode and automatically places Redstone dust, repeaters, comparators, or elevated support blocks above it.

| Mode | Description |
| --- | --- |
| None | Disables AutoWire. |
| Normal | Places Redstone dust on every newly placed support block. |
| Auto | Places dust and inserts repeaters when the signal can no longer safely continue or a dust line reaches 15 blocks. |
| Fast Auto | Builds booster steps with a block, repeater, output block, and Redstone dust when the signal is nearly depleted. |
| Only Repeaters | Places a repeater on every support block in the current travel direction. |
| Only Comparators | Places a comparator on every support block in the current travel direction. |
| Fast Comparators | Repeatedly builds block, comparator, block, and Redstone dust steps for fast comparator chains. |

Change the active mode in the config screen or with the radial Wire Menu. Hold the Wire Menu keybind, select a segment, and release the key to activate that mode. Keybinds can be configured under `Options -> Controls -> Key Binds -> Redstone Utils`.

### Wire Preview Overlay

The Wire Preview Overlay shows a translucent preview before placement. It renders both the block you are about to place and the AutoWire result above it, so you can see whether dust, a repeater, a comparator, or an elevated support block will be placed.

```mcfunction
/redstone_utils wire_overlay
/redstone_utils wire_overlay true
/redstone_utils wire_overlay false
```

### Sculk Sensor Overlay

The Sculk Overlay visualizes vibration detection ranges for nearby Sculk Sensors. It scans loaded chunks around the player and renders the audible area for every discovered sensor.

| Block | Radius |
| --- | --- |
| Sculk Sensor | 8 blocks |
| Calibrated Sculk Sensor | 16 blocks |

The overlay respects blocks tagged with `minecraft:occludes_vibration_signals`, so blocked vibration paths are excluded. Search distance and rebuild interval are configurable.

### Comparator Signal Tools

RedstoneUtils can create block items or targeted `setblock` commands for specific comparator signal strengths.

```mcfunction
/signal <0-15>
/signal <0-15> optimal
/signal <0-15> block <type>
/signal <0-15> <type>
/set-content <amount>
/set-signal <0-15>
```

`/signal <0-15> <type>` is shorthand for `/signal <0-15> block <type>`.

| Command | Purpose |
| --- | --- |
| `/signal <0-15>` | Gives a Barrel item that outputs the requested comparator strength. |
| `/signal <0-15> optimal` | Gives the most compact supported block item for the requested strength. |
| `/signal <0-15> block <type>` | Creates the specified signal block item, or sends a targeted `setblock` command for cauldron variants. |
| `/set-content <amount>` | Fills the targeted container using `setblock` with the specified amount of the item in your main hand. |
| `/set-signal <0-15>` | Fills the targeted container so that a comparator outputs the requested strength. |

Supported signal block types:

| Type | Signal Strengths | Behavior |
| --- | --- | --- |
| `barrel` | `0-15` | Container item with matching contents. |
| `chest` | `0-15` | Container item with matching contents. |
| `trapped_chest` | `0-15` | Container item with matching contents. |
| `shulker_box` | `0-15` | Container item with matching contents. |
| `dispenser` | `0-15` | Container item with matching contents. |
| `dropper` | `0-15` | Container item with matching contents. |
| `hopper` | `0-15` | Container item with matching contents. |
| `furnace` | `0-15` | Container item with matching contents. |
| `blast_furnace` | `0-15` | Container item with matching contents. |
| `smoker` | `0-15` | Container item with matching contents. |
| `brewing_stand` | `0-15` | Container item with matching contents. |
| `lectern` | `0-15` | Lectern item containing a written book opened to the matching page. |
| `crafter` | `0-9` | Crafter item with matching inventory. |
| `composter` | `0-6, 8` | Composter item with the matching fill level. |
| `cake` | `2, 4, 6, 8, 10, 12, 14` | Cake item with the matching number of bites taken. |
| `beehive` | `0-5` | Beehive item with the matching honey level. |
| `bee_nest` | `0-5` | Bee Nest item with the matching honey level. |
| `respawn_anchor` | `0, 3, 7, 11, 15` | Respawn Anchor item with the matching charge count. |
| `cauldron` | `0-3` | Sets the targeted block as a Cauldron or Water Cauldron. |
| `water_cauldron` | `0-3` | Sets the targeted block as a Water Cauldron. |
| `powder_snow_cauldron` | `0-3` | Sets the targeted block as a Powder Snow Cauldron. |
| `lava_cauldron` | `0 or 3` | Sets the targeted block as a Cauldron or Lava Cauldron. |

The `optimal` option chooses compact block types:

| Signal Strength | Block |
| --- | --- |
| `0-6`, `8` | Composter |
| `10`, `12`, `14` | Cake |
| `7`, `11`, `15` | Respawn Anchor |
| All others | Lectern |

### Macros

The macro system supports two automation types:

| Type | Description |
| --- | --- |
| Keybind Macro | Executes a stored command when a user-defined key is pressed. |
| Command Alias | Replaces a short custom command with a longer command. |

Open the macro manager with:

```mcfunction
/redstone_utils macros
```

Macro UI features:

- Create keybind macros.
- Create command aliases.
- Edit existing macros by double-clicking.
- Delete macros.
- Save commands with or without a leading `/`.
- Detect keybind and alias conflicts.

Command aliases forward arguments automatically. For example, if `/wire` expands to `/redstone_utils wire_overlay`, then `/wire false` becomes `/redstone_utils wire_overlay false`. Alias expansion prevents direct recursion and aborts excessively deep alias chains.

The alias `/redstone_utils` is reserved and cannot be overridden.

### In-Game Calculator

Open the calculator with:

```mcfunction
/calc
```

Supported expressions:

| Feature | Syntax |
| --- | --- |
| Addition and subtraction | `1+2`, `5-3` |
| Multiplication and division | `4*8`, `12/3` |
| Modulo | `10%3` |
| Exponentiation | `2^8` |
| Parentheses | `(2+3)*4` |
| Square root | `sqrt(9)` or `sqrt 9` |
| Previous result | `ans` |
| Sign toggle | `+/-` button |

The calculator provides live result previews and stores the previous result for `ans`.

### Teleport Keybind

The `Teleport to targeted block` keybind teleports you to the block you are looking at. If no block is found within the configured range, it teleports you forward along your line of sight up to the maximum distance.

The teleport range is configurable and clamped to `10-1000` blocks. The configuration UI offers presets of `25`, `50`, `100`, `200`, and `500` blocks.

In singleplayer, teleportation is performed through the integrated server. On multiplayer servers, RedstoneUtils sends a `/tp @s ...` command, so the server must allow it.

### Feedback and HUD

RedstoneUtils includes a lightweight popup system displayed in the upper-left corner of the HUD. Feedback can also be sent to chat, the action bar, or a combination of outputs.

| Output | Description |
| --- | --- |
| Popup | Small RedstoneUtils popup. |
| Chat | Chat message. |
| Action Bar | Message above the hotbar. |
| Popup + Chat | Popup and chat message. |
| Popup + Action | Popup and action bar message. |

The HUD overlay controls popups and the Wire Menu. The config screen remains usable even when the HUD overlay is disabled.

## Commands

### Main Command

```mcfunction
/redstone_utils config
/redstone_utils macros
/redstone_utils wire_overlay [true|false]
/redstone_utils sculk_overlay [true|false]
/redstone_utils all_overlays [true|false]
```

| Command | Description |
| --- | --- |
| `/redstone_utils config` | Opens the RedstoneUtils config screen. |
| `/redstone_utils macros` | Opens the macro manager. |
| `/redstone_utils wire_overlay` | Toggles the Wire Preview Overlay. |
| `/redstone_utils wire_overlay true / false` | Explicitly enables or disables the Wire Preview Overlay. |
| `/redstone_utils sculk_overlay` | Toggles the Sculk Overlay. |
| `/redstone_utils sculk_overlay true / false` | Explicitly enables or disables the Sculk Overlay. |
| `/redstone_utils all_overlays` | Toggles the HUD, Wire Preview, and Sculk Overlay together. |
| `/redstone_utils all_overlays true / false` | Explicitly enables or disables all overlays. |

### Additional Commands

```mcfunction
/calc
/signal <0-15>
/signal <0-15> optimal
/signal <0-15> block <type>
/signal <0-15> <type>
/set-content <amount>
/set-signal <0-15>
```

## Keybinds

All built-in keybinds can be configured under `Options -> Controls -> Key Binds -> Redstone Utils`.

| Keybind | Function |
| --- | --- |
| Teleport to targeted block | Teleports to the targeted block or up to the configured maximum range. |
| Open wire menu | Opens the radial Wire Menu while held and activates the selected AutoWire mode on release. |

Macro keybinds are created and managed through `/redstone_utils macros`.

## Configuration

The main configuration file is stored in the Fabric config directory:

```text
config/redstoneutils.json
```

Macros are stored separately:

```text
config/redstoneutils_macros.json
```

Configuration is loaded, sanitized, and saved automatically during startup. Invalid or incomplete values are reset to safe defaults.

| Option | Default | Range / Values | Description |
| --- | --- | --- | --- |
| HUD Overlay | `true` | `true` / `false` | Enables popups and the Wire Menu. |
| Wire Preview Overlay | `true` | `true` / `false` | Displays the AutoWire preview. |
| Sculk Overlay | `false` | `true` / `false` | Displays Sculk Sensor ranges. |
| Active Wire Mode | `None` | All AutoWire modes | Current AutoWire mode. |
| Feedback Output | `Popup` | Popup, Chat, Action Bar, combinations | Controls where feedback messages appear. |
| Teleport Max Range | `100.0` | `10.0-1000.0` | Maximum teleport raycast distance. |
| Sculk Sensor Search Distance | `96` | `16-256` | Search radius for nearby Sculk Sensors. |
| Sculk Rebuild Interval | `5` ticks | `1-100` ticks | Number of ticks between Sculk Overlay rebuilds. |

The configuration UI provides presets for numeric options, including `32`, `64`, `96`, `128`, and `192` blocks for Sculk search distance.

## Installation

1. Install Fabric Loader for Minecraft 26.2.
2. Install Fabric API for Minecraft 26.2.
3. Place the compiled RedstoneUtils JAR in your `mods` folder.
4. Launch Minecraft with the Fabric profile.

RedstoneUtils is client-side and only needs to be installed on the client. On multiplayer servers, individual features depend on whether the server allows the commands they use.

## Building

Requirements:

- JDK 25
- Git
- Internet access for the first Gradle/Fabric dependency download

Common Gradle commands:

```shell
./gradlew build
./gradlew runClient
./gradlew clean
```

After a successful build, the remapped mod JAR is typically located at:

```text
build/libs/RedstoneUtils-1.0-SNAPSHOT.jar
```

## Project Structure

```text
src/main/java/org/main/redstoneutils/
  RedstoneUtils.java                 Shared mod initializer and Mod ID
  client/
    RedstoneUtilsClient.java         Client initialization
    RedstoneUtilsCommand.java        /redstone_utils commands
    Keybindings.java                 Built-in RedstoneUtils keybinds
    autowire/                        AutoWire logic, placement, preview
    calculator/                      /calc command, parser, and UI
    config/                          Configuration file and config screen
    macro/                           Macros, aliases, macro UI
    sculk/                           Sculk Sensor Overlay
    signal/                          Comparator signal tools
    teleport/                        Teleport keybind
    ui/                              HUD, popups, wheel menu, shared UI helpers

src/main/resources/
  fabric.mod.json                    Fabric metadata
  assets/redstoneutils/lang/en_us.json
  assets/redstoneutils/textures/gui/wire/
```

## Notes and Limitations

- Many features send commands from the client. Without sufficient permissions on a server, commands such as `/give`, `/setblock`, or `/tp` may fail.
- On multiplayer servers, AutoWire requires the needed items to be available in the main hand, offhand, or hotbar because placement uses normal item interactions.
- In singleplayer, AutoWire uses the integrated server for direct block placement and updates neighboring Redstone dust automatically.
- The Sculk Overlay only scans loaded chunks and may become more expensive with large search distances.
- Macro execution is disabled while no player is present, no server connection exists, or another GUI screen is open.
- Command aliases must be a single word and may only contain `a-z`, `0-9`, `_`, or `-`.

## License

Copyright (c) 2026.

All rights reserved.
