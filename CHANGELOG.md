# Changelog

Notable changes to Redstone Utils are documented in this file.

## [2.0.0-alpha.1] - 2026-08-28

First public alpha for Minecraft 26.2.

### Added

- AutoWire placement modes with a radial menu, previews, and undo/redo support.
- Read-only wire, BUD/quasi-connectivity, and sculk sensor overlays.
- Comparator and Ethonian hopper clock builders, with documented examples for the first four possible comparator-clock periods.
- Comparator signal, container-content, calculator, color, toolbox, macro, and profile tools.
- Server-side permissions, configurable limits, and validation for world-changing actions.
- A server-owned `redstone_utils:waterproof_redstone` gamerule that leaves vanilla behavior unchanged by default.
- Versioned Wiki documentation, contribution guidance, security policy, privacy information, and third-party notices.

### Changed

- Unified commands and persistent identifiers under the `redstone_utils` namespace while retaining the public project name **Redstone Utils**.
- Updated the build to Gradle 9.7.1 and the test suite to JUnit 6.1.3.

### Notes

- This is an alpha release. Configuration, commands, and behavior may change before the first stable release.
- Install the mod on both client and server for all features. A client-only installation provides only the locally available tools and fallbacks.
- The BUD overlay only visualizes relevant piston/quasi-connectivity states; it does not alter Minecraft's update logic or repair BUD contraptions.

[2.0.0-alpha.1]: https://github.com/Redstone787/RedstoneUtils/releases/tag/v2.0.0-alpha.1-mc26.2
