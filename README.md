# RedstoneUtils

RedstoneUtils is a Fabric mod for Minecraft 26.2 that can run on both client and server. It contains the full client UI from RedstoneUtilsClient plus server-side implementations for the features that can edit the world.

This project is the full mod for worlds and servers where RedstoneUtils can be installed server-side. The separate `RedstoneUtilsClient` project remains a client-only fallback for players on creative servers where they cannot upload server mods.

## Project Status

| Field | Value |
| --- | --- |
| Mod ID | `redstoneutils` |
| Mod Name | `RedstoneUtils` |
| Version | `1.0.0` |
| Minecraft | `26.2` |
| Fabric Loader | `>=0.19.3` |
| Fabric API | `0.154.2+26.2` |
| Java | `25` |
| Environment | Client + Server |
| License | All rights reserved |

## Server-Side Features

World-editing features are implemented with server APIs instead of internally sending vanilla `/give`, `/setblock`, or `/tp` commands.

### AutoWire

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

AutoWire state is stored per player on the server. The server detects block placements and places dust, repeaters, comparators, and elevated support blocks directly.

### Comparator Signal Tools

```mcfunction
/signal <0-15>
/signal <0-15> optimal
/signal <0-15> block <type>
/signal <0-15> <type>
/set-content <amount>
/set-signal <0-15>
```

`/signal` creates ItemStacks directly and inserts them into the player inventory. `/set-content` and `/set-signal` raycast from the player and fill the targeted container block entity directly.

### Teleport Debugging

```mcfunction
/redstone_utils tp
/redstone_utils tp <10-1000>
```

The server raycasts from the player and teleports them to the hit location, or forward to the maximum range if no block is hit.

## Client Features

RedstoneUtils also includes the client experience from RedstoneUtilsClient:

- Wire Preview Overlay
- Sculk Sensor Overlay
- Config screen
- Macro manager, command aliases, and keybind macros
- In-game calculator UI
- Teleport keybind
- Radial AutoWire menu
- Popup/HUD feedback

When connected to a server with RedstoneUtils installed, the radial AutoWire menu and teleport keybind use RedstoneUtils networking payloads and run through server-side code. If the server backend is unavailable, the copied client logic can still fall back to client-side behavior where applicable.

## Permissions

Server-side commands and network actions are available to gamemaster-level users or creative players. This keeps world-editing behavior scoped to creative/test workflows.

## Installation

For the full experience, install this mod on both the client and the server.

1. Install Fabric Loader for Minecraft 26.2.
2. Install Fabric API for Minecraft 26.2.
3. Place `RedstoneUtils-1.0.0.jar` in the client `mods` folder.
4. Place `RedstoneUtils-1.0.0.jar` in the server `mods` folder.
5. Start the server and client.

Use `RedstoneUtilsClient` instead when only the client can install mods.

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

After a successful build, the remapped mod JAR is typically located at:

```text
build/libs/RedstoneUtils-1.0.0.jar
```

## Project Structure

```text
src/main/java/org/main/redstoneutils/
  RedstoneUtils.java                 Shared mod initializer and Mod ID
  client/                            Client overlays, screens, keybinds, macros, calculator
  network/                           Client-to-server payload definitions
  server/                            Server commands, networking handlers, world-editing logic

src/main/resources/
  fabric.mod.json                    Fabric metadata
  assets/redstoneutils/              Client icon, language, and overlay textures
```

## License

Copyright (c) 2026.

All rights reserved.
