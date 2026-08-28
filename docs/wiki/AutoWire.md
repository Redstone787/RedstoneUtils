# AutoWire

AutoWire can be controlled with `/autowire` or the radial wire menu. The current mode is kept in the active client profile and synchronized with a Redstone Utils server when available.

## Modes

| Mode | Purpose |
| --- | --- |
| `none` | Disable automatic placement |
| `normal` | Basic Redstone-dust continuation |
| `auto` | Automatically choose supported continuation components |
| `fast_auto` | Faster booster-oriented automatic layouts |
| `only_repeaters` | Place repeater-oriented layouts |
| `only_comparators` | Place comparator-oriented layouts |
| `fast_comparators` | Comparator layouts with fast continuation |

`/autowire reset` clears the current placement sequence without changing the mode.

## Multiplayer behavior

With the mod installed on the server, the selected mode is stored per player and placement is executed server-side. Every placement rechecks permissions, occupancy, world height, required inventory, and placement validity. Survival players consume matching items; Creative players retain infinite-material behavior.

If the server denies a mode, client and server reset to a safe state. Failed placement feedback identifies common causes such as an occupied target, unsupported placement, missing item, build limit, or insufficient permission.

## Preview and controls

The wire overlay previews the calculated placement. Movement keys configured by the player remain usable while the radial menu is open. Overlay visibility, colors, opacity, distance, and through-wall behavior are configured under [Configuration](Configuration.md).
