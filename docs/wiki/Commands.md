# Commands

`/redstonelabworks` is the collision-safe root command and opens the toolbox without arguments. Short convenience roots remain available. If a short name conflicts with another mod or plugin, use its `/redstonelabworks ...` form.

## UI and utility commands

```mcfunction
/redstonelabworks
/redstonelabworks toolbox
/redstonelabworks config
/redstonelabworks teleport [10-1000]
/macro
/calc <expression>
/color <hex-color>
/sculkinfo
```

The teleport command raycasts from the player. The server backend decides whether the request is permitted and clamps its range to server limits.

## Overlays

```mcfunction
/overlay
/overlay wire [true|false]
/overlay bud [true|false]
/overlay sculk [true|false]
/overlay all [true|false]
```

`/overlay` without a subcommand toggles all overlays.

## AutoWire

```mcfunction
/autowire
/autowire none
/autowire normal
/autowire auto
/autowire fast_auto
/autowire only_repeaters
/autowire only_comparators
/autowire fast_comparators
/autowire reset
```

## Signal tools

```mcfunction
/signal <0-15>
/signal optimal <0-15>
/signal block <type> <0-15>
/signal container content <x> <y> <z> <amount> <item> [name]
/signal container strength <x> <y> <z> <0-15> <item> [name]
```

The container forms accept vanilla coordinates such as `~ ~-1 ~`. `/set-content` and `/set-signal` are compatibility aliases.

## Builders and history

```mcfunction
/clock <interval>
/clock comparator <interval>
/clock hopper <interval>
/clock undo
/redstone undo
/redstone redo
```

See [Clock Builder](Clock-Builder.md) for exact supported periods.

## Gamerule

Waterproof Redstone is not a regular command or config toggle:

```mcfunction
/gamerule redstonelabworks:waterproof_redstone true
/gamerule redstonelabworks:waterproof_redstone false
```
