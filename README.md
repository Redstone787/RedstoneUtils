# Redstone Utils

Redstone Utils is a Fabric mod for Minecraft 26.2 with client-side workflow tools and server-side implementations for world-editing features. Install it on both client and server for the complete multiplayer experience; client-only usage still provides the local overlays, menus, macros, calculator, and fallback commands where Minecraft allows them.

[Releases / Downloads](https://github.com/Redstone787/RedstoneUtils/releases)

## Project Status

| Field | Value |
| --- | --- |
| Mod ID | `redstoneutils` |
| Display Name | Redstone Utils |
| Version | `1.1.1` |
| Minecraft | `26.2` |
| Fabric Loader | `>=0.19.3` |
| Fabric API | `0.154.2+26.2` |
| Java | `25` |
| Environment | Client + Server |
| License | All rights reserved |

## Features

### AutoWire

AutoWire can be controlled through commands or the radial wire menu. On servers with Redstone Utils installed, the selected mode is stored per player and block placements are handled directly on the server.
The active client mode is sent automatically whenever a play connection becomes ready, so the HUD, radial menu, and server backend cannot silently start with different modes. Failed placements report the concrete reason as a popup (occupied target, unsupported placement, build limit, missing item, or missing permission).

```mcfunction
/redstone_utils autowire
/redstone_utils autowire none
/redstone_utils autowire normal
/redstone_utils autowire auto
/redstone_utils autowire fast_auto
/redstone_utils autowire only_repeaters
/redstone_utils autowire only_comparators
/redstone_utils autowire fast_comparators
/redstone_utils reset_autowire
```

Supported placement modes cover redstone dust, repeaters, comparators, elevated support blocks, and fast booster layouts.

### Comparator Signal Tools

```mcfunction
/signal <0-15>
/signal <0-15> optimal
/signal <0-15> block <type>
/signal <0-15> <type>
/set-content <amount>
/set-signal <0-15>
```

`/signal` gives comparator-output items directly when the server module is available. `/set-content` and `/set-signal` raycast from the player and edit the targeted container block entity directly.

### Clock Builder

```mcfunction
/clock <interval>
/clock comparator <interval>
/clock hopper <interval>
/clock undo
```

The clock builder supports comparator and Ethonian hopper clocks. `comparator` is optional and remains the default clock type. Intervals can be written as a plain redstone-tick count (`2`), an explicit redstone-tick count (`2t`), or seconds (`1s`). Decimal seconds such as `0.2s` are supported. One second equals 10 redstone ticks.

The interval is the complete period from one rising pulse to the next, including the on and off phases. Comparator clocks support even intervals from 2 to 600 redstone ticks. In seconds, that is `0.2s` to `60s` in `0.2s` steps. Unsupported values return an error instead of being rounded.

The feedback loop uses the smallest possible two-row layout. A two-tick clock contains only the comparator and redstone dust:

```text
#x
xx
```

A four-tick clock adds one repeater:

```text
#xx
x<-x
```

Longer intervals use the fewest possible repeaters, with each repeater configured for up to four ticks. Whenever the loop contains repeaters, its final repeater is placed directly before the comparator's subtraction input. It regenerates the feedback signal to strength 15 so the comparator can switch fully off. Additional repeaters fill the outgoing row first and then the remaining return-row positions. `#` is the subtract-mode comparator, arrows are repeaters, and `x` is redstone dust. A redstone block directly behind the comparator supplies the input signal.

Hopper clocks use the classic flat 2-by-6 Ethonian layout:

```text
B <C H> <H C> B
x P> R  .  <P x
```

`H` marks the two hoppers facing into each other, `C` the outward-facing comparators, `P` the sticky pistons, `R` the initial redstone-block position, `.` its second position, `x` redstone dust, and `B` solid blocks. Counter items are added to the right hopper automatically. The moving redstone block locks one hopper at a time and also provides the alternating clock output.

Hopper intervals are nominal periods from 8 to 2,560 redstone ticks (`0.8s` to `256s`) in 8-tick (`0.8s`) steps. Each step adds one stick to the hoppers, up to their combined capacity of 320 items. The classic Ethonian clock transfers its first item immediately, so its measured period is half a redstone tick (`0.05s`) shorter than the nominal command value: one item measures about `0.75s`, and every additional item adds `0.8s`.

The clock is built at the player's feet and points in the horizontal direction the player is looking. If the player holds a solid full redstone-conducting block that does not emit a signal in the main hand, that block is used for the support platform and the hopper clock's two outer blocks; otherwise the builder uses white wool. Configured repeaters in comparator feedback loops and counter items in hopper clocks provide the requested delay.

`/clock undo` restores complete block states, inventories, and other block-entity data through the shared history described below.

### Shared Undo and Redo

All server-backed world edits use one per-player history. It includes Clock Builder operations, AutoWire components, `/set-content`, `/set-signal`, and targeted signal-block replacements. Block states and full block-entity data are captured, including container inventories.

```mcfunction
/redstone undo
/redstone redo
```

`/clock undo` remains as a compatibility alias for undoing the latest shared change. A new edit clears the redo stack. History is kept for the current server session, works across dimensions, and its maximum size is configurable on the server.

### Teleport Debugging

```mcfunction
/redstone_utils tp
/redstone_utils tp <10-1000>
```

The server raycasts from the player and teleports them to the hit location, or forward to the maximum range if no block is hit.

### Client Tools

- Central toolbox, available from a keybind, the pause-menu button, or `/redstone_utils toolbox`
- Compact status HUD for AutoWire mode, enabled overlays, frozen snapshots, and server-backend availability
- Wire preview overlay
- BUD switch test overlay
- Sculk sensor overlay
- Searchable, categorized config screen with per-setting reset, tooltips, exact number input, and sliders
- Macro manager with search, categories, sorting, duplication, enable/disable, import/export, and delete confirmation
- Key and mouse macro combinations with modifiers and pressed, released, or held triggers
- In-game calculator through `/calc`
- Teleport keybind
- Radial AutoWire menu
- Popup, chat, and action-bar feedback options
- English and German translations

Overlay visibility is controlled through its own command:

```mcfunction
/overlay
/overlay wire [true|false]
/overlay bud [true|false]
/overlay sculk [true|false]
/overlay all [true|false]
```

`/overlay` without a subcommand toggles all overlays. The radial AutoWire menu keeps the configured forward, backward, left, and right movement keys active while it is open.

The BUD switch overlay continuously searches a configurable spherical range around the player for quasi-connectivity risks. Pistons, dispensers, and droppers that can be powered through the block space above them are highlighted red. The current or still-unpowered blocks capable of supplying that quasi-power are highlighted yellow. This includes solid conductors with incoming redstone or another switchable signal source, so a potential BUD is visible before it activates. The test range can be changed in `/redstone_utils config`.

### Toolbox and Status HUD

Bind **Open Redstone Utils toolbox** in Minecraft's Controls screen or use the **Redstone Utils** button in the pause menu. The toolbox provides direct access to AutoWire, overlays, Clock Builder, signal tools, calculator, macros, settings, and overlay snapshots. Clock and signal buttons open chat with the corresponding command prefilled so parameters can be entered without memorizing the command name.

The status HUD can be disabled or placed in any screen corner. It displays:

- the active AutoWire mode;
- currently enabled wire, BUD, and sculk overlays;
- whether the server backend is available;
- whether an overlay snapshot is frozen.

### Profiles

Client settings and macros support a global default profile plus automatically selected profiles for each multiplayer server and singleplayer world. A profile contains its AutoWire mode, overlay states, and macro collection. The first time a server or world is opened, its settings and macros are copied from the global defaults; later changes remain specific to that profile. The active profile key is shown in the config, macro, and toolbox screens.

### Macro Manager

Keybind macros support normal keys, mouse buttons, and `Ctrl`, `Shift`, `Alt`, or `Super` modifiers. Each binding can trigger when pressed, when released, or repeatedly while held. Command aliases remain available and can be enabled or disabled just like keybind macros.

The macro list can be searched and sorted by category, name, or type. Import and export use this editable JSON file by default:

```text
config/redstoneutils_macros_export.json
```

Conflicting aliases or key combinations are skipped during import. Imported macros receive new IDs.

### Overlay Appearance, Accessibility, and Snapshots

The config screen provides shared overlay controls for opacity, line width, visibility through walls, and maximum analysis distance. The wire preview, BUD risk, BUD source, and sculk range colors can each be entered as a six-digit RGB hex value. Resetting an individual color returns it to the selected palette.

Available palettes are Default, Deuteranopia, Protanopia, Tritanopia, and High Contrast. Popup position and duration are configurable independently.

Freezing overlays captures the most recently available wire preview, BUD analysis, and sculk analysis. The geometry remains at its world position while the player moves, making it possible to inspect an analysis result from another angle. Toggle the snapshot from the toolbox or the config screen.

## Client/Server Behavior

When the client connects to a server with Redstone Utils installed, AutoWire mode changes and teleport actions use custom networking payloads and execute through server-side code. If the server backend is unavailable, client-side behavior is used where possible.

The server controls Teleport, AutoWire, Signal Tools, Builder, and History permissions independently. Each permission defaults to `OP_OR_CREATIVE` and can instead be set to `OP_ONLY`, `CREATIVE_ONLY`, `EVERYONE`, or `DISABLED`.

## Server Configuration

The server creates `config/redstoneutils-server.json` on first start. Changes take effect after a server restart.

```json
{
  "teleportPermission": "OP_OR_CREATIVE",
  "autoWirePermission": "OP_OR_CREATIVE",
  "signalToolsPermission": "OP_OR_CREATIVE",
  "builderPermission": "OP_OR_CREATIVE",
  "historyPermission": "OP_OR_CREATIVE",
  "maxTeleportRange": 1000.0,
  "maxTargetRange": 128.0,
  "maxContainerItems": 100000,
  "maxComparatorClockTicks": 600,
  "maxHopperClockTicks": 2560,
  "historySize": 20
}
```

Limits are sanitized to safe implementation bounds when loaded. Network teleport requests are clamped again on the server, and command arguments use the configured maximum values.

## Configuration Safety

Client settings and macros are stored in:

```text
config/redstoneutils.json
config/redstoneutils_macros.json
```

Writes use a temporary file followed by an atomic replacement where the platform supports it. If either JSON file cannot be parsed, the original is copied to the corresponding `.bak` file before defaults are written. An in-game popup shows the full backup path. The server config applies the same backup behavior and records the problem in the server log.

## Installation

For the full experience, install the mod on both the client and the server.

1. Install Fabric Loader for Minecraft 26.2.
2. Install Fabric API for Minecraft 26.2.
3. Build or download the matching mod JAR.
4. Place the mod JAR in the client `mods` folder.
5. Place the same mod JAR in the server `mods` folder.
6. Start the server and client.

## Building

Requirements:

- JDK 25
- Internet access for the first Gradle/Fabric dependency download

Common Gradle commands:

```shell
./gradlew build
./gradlew runClient
./gradlew runServer
./gradlew clean
```

After a successful build, the remapped mod JAR is written to `build/libs/`.

## Project Structure

```text
src/main/java/org/main/redstoneutils/
  Shared initializer and mod ID helpers
  client/                            Client overlays, screens, keybinds, macros, calculator, fallback commands
  network/                           Client-to-server and server-to-client payload definitions
  server/                            Server commands, networking, config, shared history, and world-editing logic

src/main/resources/
  fabric.mod.json                    Fabric metadata
  assets/redstoneutils/              Client icon, English/German language files, and overlay textures
```

## License

Copyright (c) 2026.

All rights reserved.
