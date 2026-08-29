# Sculk overlay event-driven caching

Date: 2026-08-28
Status: Unreleased

## Changed

- Added a client-side, per-chunk index of loaded Sculk sensor block entities.
- Added lifecycle handling for sensors loaded, placed, replaced, removed, and unloaded, including changes received from other players through normal multiplayer updates.
- Replaced periodic sensor discovery scans with one-time chunk reconciliation and incremental block-entity updates.
- Added per-sensor range-mesh caching and targeted invalidation when vibration-occluding blocks or relevant chunks change.
- Limited expensive mesh generation to one selected sensor per client tick while retaining the configured interval as a per-mesh rebuild debounce.
- Cached reusable render primitives and deduplicated surface lines instead of rebuilding all gizmo objects every frame.

## Compatibility

- The Sculk overlay remains client-side and does not require Redstone Utils on the multiplayer server.
- Existing Sculk visibility, search-distance, appearance, freeze, and update-interval settings remain compatible.
- Gradle dependency verification now trusts only Loom's locally generated Minecraft merged JAR, whose archive checksum changes between regenerations; downloaded dependencies remain verified normally.
