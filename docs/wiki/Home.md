# Redstone Labworks Wiki

Redstone Labworks is a Fabric mod for Minecraft 26.2 focused on Redstone construction, inspection, and debugging. Install it on both client and server for the complete feature set. A client-only installation keeps local overlays, menus, macros, profiles, the calculator, and available command fallbacks.

## Start here

- [Installation](Installation.md)
- [Commands](Commands.md)
- [AutoWire](AutoWire.md)
- [Clock Builder](Clock-Builder.md)
- [Overlays](Overlays.md)
- [Macros and Profiles](Macros-and-Profiles.md)
- [Configuration](Configuration.md)
- [Server Administration](Server-Administration.md)
- [Troubleshooting](Troubleshooting.md)

## Important behavior

The `redstonelabworks:waterproof_redstone` setting is only a gamerule. It is disabled by default, so installing the mod does not alter vanilla water behavior unless a server operator explicitly enables it.

Redstone Labworks inspects BUD/quasi-connectivity states but does not repair them or replace Minecraft's Redstone update logic. The Waterproof Redstone mixins only intercept water-replacement checks while the gamerule is enabled.

## Project policies

See [Privacy and Data](Privacy-and-Data.md), [Contributing](Contributing.md), the repository [security policy](https://github.com/Redstone787/RedstoneLabworks/blob/main/SECURITY.md), and [MPL-2.0 license](https://github.com/Redstone787/RedstoneLabworks/blob/main/LICENSE).
