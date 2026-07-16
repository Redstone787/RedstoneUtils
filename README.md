# Redstone Utils

Redstone Utils is a Fabric mod for Minecraft 26.2 with client-side workflow tools and server-side implementations for world-editing features. Install it on both client and server for the complete multiplayer experience; client-only usage still provides the local overlays, menus, macros, calculator, and fallback commands where Minecraft allows them.

[Releases / Downloads](https://github.com/Redstone787/RedstoneUtils/releases)

## Project Status

| Field | Value |
| --- | --- |
| Mod ID | `redstoneutils` |
| Display Name | Redstone Utils |
| Version | `1.1.0` |
| Minecraft | `26.2` |
| Fabric Loader | `>=0.19.3` |
| Fabric API | `0.154.2+26.2` |
| Java | `25` |
| Environment | Client + Server |
| License | All rights reserved |

## Features

### AutoWire

AutoWire can be controlled through commands or the radial wire menu. On servers with Redstone Utils installed, the selected mode is stored per player and block placements are handled directly on the server.

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

### Teleport Debugging

```mcfunction
/redstone_utils tp
/redstone_utils tp <10-1000>
```

The server raycasts from the player and teleports them to the hit location, or forward to the maximum range if no block is hit.

### Client Tools

- Wire preview overlay
- Sculk sensor overlay
- Config screen
- Macro manager, command aliases, and keybind macros
- In-game calculator through `/calc`
- Teleport keybind
- Radial AutoWire menu
- Popup, chat, and action-bar feedback options

Overlay visibility is controlled through its own command:

```mcfunction
/overlay
/overlay wire [true|false]
/overlay sculk [true|false]
/overlay all [true|false]
```

`/overlay` without a subcommand toggles all overlays. The radial AutoWire menu keeps the configured forward, backward, left, and right movement keys active while it is open.

## Client/Server Behavior

When the client connects to a server with Redstone Utils installed, AutoWire mode changes and teleport actions use custom networking payloads and execute through server-side code. If the server backend is unavailable, client-side behavior is used where possible.

Server-side commands and network actions are available to gamemaster-level users or creative players, keeping world-editing tools scoped to creative and test workflows.

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
  server/                            Server commands, networking handlers, world-editing logic

src/main/resources/
  fabric.mod.json                    Fabric metadata
  assets/redstoneutils/              Client icon, language file, and overlay textures
```

## License

Copyright (c) 2026.

All rights reserved.
