# Overlays

Redstone Utils includes wire, BUD, and sculk overlays. They can be toggled independently with `/overlay`, through the toolbox, or in configuration.

## Wire preview

Shows the blocks AutoWire plans to place before the placement is committed.

## BUD/quasi-connectivity overlay

The BUD overlay searches a configurable spherical range for armed quasi-connectivity states involving pistons, dispensers, and droppers. A block is highlighted as a BUD risk when its current state disagrees with quasi-power received through the block space above it and normal adjacent power does not explain the state.

This includes piston arrangements where a power source, such as a Redstone block, is two blocks above the piston: the space directly above the piston receives power and can quasi-power the piston. The overlay also recognizes stale powered or unpowered states. A retracted quasi-powered piston can remain an armed BUD even when currently obstructed, because changing the obstruction causes the update that lets it react.

Risk blocks are red by default and active quasi-power sources are yellow. A BUD waiting to deactivate may have no yellow source because that source is already gone.

The overlay is diagnostic only. It does not place updates, repair BUDs, suppress quasi-connectivity, or change Minecraft's Redstone update logic.

## Sculk overlay

Shows nearby sculk sensor analysis. Rendering is capped at the four nearest sensors by default.

The client maintains an event-driven index of sculk sensor block entities in loaded chunks. A chunk is inspected when it arrives, then normal block-entity load and unload events keep the index synchronized when sensors are placed, removed, replaced, or changed by another player. Multiplayer detection uses the vanilla chunk and block updates that the server already sends; the Redstone Utils server backend is not required for this overlay.

Calculated range meshes are cached. A mesh is invalidated only when its sensor changes, a relevant chunk loads or unloads, or a block inside the sensor range changes whether it occludes vibration signals. At most one invalidated sensor mesh is rebuilt per client tick, and the configured Sculk update interval is the minimum delay before the same cached mesh may be rebuilt again. Cached render primitives are reused between frames.

## Appearance and snapshots

Opacity, line width, through-wall rendering, analysis distance, individual colors, and accessibility palettes are configurable. Freezing an overlay captures its current geometry—even an empty result—so it remains at the same world position while the player moves.
