# Installation

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API 0.154.2+26.2
- Java 25

## Client and server

1. Install Fabric Loader for Minecraft 26.2.
2. Add the matching Fabric API JAR to the `mods` directory.
3. Add the Redstone Labworks JAR to the same directory.
4. For the complete multiplayer feature set, repeat steps 2 and 3 on the server.
5. Start the game/server and confirm that Fabric lists `redstonelabworks` without dependency errors.

Client-only usage is supported for local UI features and command fallbacks. Teleport, server AutoWire, signal/container editing, builders, shared history, permissions, and the Waterproof Redstone gamerule require the server component.

## Building from source

```shell
git clone https://github.com/Redstone787/RedstoneLabworks.git
cd RedstoneLabworks
./gradlew build
```

The remapped JAR is placed in `build/libs/`. Source builds require JDK 25 and internet access for the first dependency download.
