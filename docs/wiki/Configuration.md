# Configuration

The in-game config screen is searchable and categorized. It provides toggles, choices, numeric inputs/sliders, per-setting reset buttons, exact color input, profile reset, and tooltips.

## Client files

```text
config/redstonelabworks.json
config/redstonelabworks_macros.json
config/redstonelabworks_macros_export.json
```

Available settings cover AutoWire and overlay visibility, BUD/sculk ranges, status HUD placement, feedback target, teleport fallback range, overlay colors/opacity/line width/distance, through-wall rendering, color-vision palettes, popup position/duration, and analysis rebuild intervals.

## Safe writes and recovery

Writes use a temporary file followed by atomic replacement where supported. The previous valid file is kept as `.bak`. If a JSON file cannot be parsed, the damaged input is preserved as `.corrupt-<timestamp>.bak`; the last valid backup or defaults are then loaded. An in-game message identifies the recovery path.

On the first run after the project rename, existing `redstoneutils.json`, `redstoneutils_macros.json`, and `redstoneutils-server.json` files (and valid `.bak` files) are copied to their new names. The legacy files are deliberately preserved and never overwritten by the migration.

Exit Minecraft before editing or deleting configuration files. A malformed value may be sanitized to the implementation's safe range during the next load.

## Status HUD

The HUD reports the active AutoWire mode, enabled overlays, server-backend availability, and frozen snapshots. It can be placed in any screen corner and hides while the F3 debug screen is open.
