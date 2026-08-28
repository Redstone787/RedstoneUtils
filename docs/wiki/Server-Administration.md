# Server Administration

The server creates `config/redstonelabworks-server.json` on first start. Restart the server after changing it.

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
  "maxHopperClockTicks": 2554,
  "historySize": 20
}
```

Permission values are `OP_OR_CREATIVE`, `OP_ONLY`, `CREATIVE_ONLY`, `EVERYONE`, or `DISABLED`. Limits are sanitized during loading and rechecked during network/command handling.

## Waterproof Redstone

Waterproof Redstone is controlled exclusively by the server gamerule:

```mcfunction
/gamerule redstonelabworks:waterproof_redstone true
```

It defaults to `false`. When enabled, water does not replace protected Redstone components, including dust, torches, repeaters, comparators, levers, tripwire/hooks, buttons, pressure plates, and relevant rails. Flowing water, player buckets, and dispenser buckets are covered.

Modpacks can extend the protected set through the `redstonelabworks:waterproof_redstone_components` block tag. The implementation does not otherwise change Redstone neighbor updates, scheduled ticks, quasi-connectivity, or BUD behavior.

## Shared history

Clock Builder, AutoWire, container signal/content changes, and targeted signal-block replacements share per-player undo/redo history for the current server session. Full block states and block-entity data are recorded. A new edit clears the redo stack.
