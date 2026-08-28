# Troubleshooting

## The server backend is unavailable

Confirm that the same compatible Redstone Labworks, Fabric Loader, and Fabric API versions are installed on both client and server. Client-only features still work, but world-editing/server commands will be unavailable or use limited fallbacks.

## A command conflicts with another mod/plugin

Use the canonical `/redstonelabworks ...` form. Short command roots exist for convenience and are more likely to collide.

## AutoWire placement was denied

Check server permission mode, target occupancy, build height, inventory items in Survival, protection/mod permissions, and whether the selected component supports the target placement.

## Configuration was reset

Look in the Minecraft `config` directory for `.bak` and `.corrupt-<timestamp>.bak` files. The mod preserves malformed input and attempts to recover the last valid backup. Exit Minecraft before manually restoring a file.

## An overlay appears stale

Unfreeze snapshots, confirm the overlay is enabled, move within its analysis distance, or lower the configured rebuild interval. Large BUD ranges deliberately refresh more slowly.

## Reporting a bug

Run `./gradlew build` for source issues or reproduce with unrelated mods removed where practical. Then use the repository bug-report form with exact versions and sanitized logs. Never post a vulnerability, access token, server address, or private world data publicly.
