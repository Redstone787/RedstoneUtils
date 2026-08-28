<p align="center">
  <img src="src/main/resources/assets/redstone_utils/icon.png" alt="Redstone Utils logo" width="220">
</p>

# Redstone Utils

Redstone Utils is a Fabric mod for Minecraft 26.2 with client-side tools for designing, inspecting, and debugging Redstone systems and optional server-side implementations for world-editing features.

> **Pre-release:** public-release preparation is still in progress. No public build is available yet.

## Highlights

- AutoWire placement modes with a radial menu and world preview;
- BUD/quasi-connectivity and sculk sensor overlays;
- comparator and Ethonian hopper clock builders with shared undo/redo;
- comparator signal and container-content tools;
- profiles, macros, calculator, color helper, toolbox, and configurable status HUD;
- server-owned permissions, limits, networking validation, and a Waterproof Redstone gamerule.

## Compatibility

| Component | Version |
| --- | --- |
| Mod ID | `redstone_utils` |
| Development version | `2.0.0-alpha.1` |
| Minecraft | `26.2` |
| Fabric Loader | `>=0.19.3` |
| Fabric API | `0.154.2+26.2` |
| Java | `25` |
| Environment | Client and server |

Install the mod on both client and server for all features. Client-only installation still provides local menus, overlays, profiles, macros, the calculator, and command fallbacks where Minecraft permits them.

## Quick start

```mcfunction
/redstone_utils
/redstone_utils config
/autowire normal
/overlay bud true
/clock 2t
/signal 15
/redstone undo
```

Waterproof Redstone is intentionally available only as a server gamerule and defaults to vanilla behavior:

```mcfunction
/gamerule redstone_utils:waterproof_redstone true
```

## Documentation

The full installation, command, AutoWire, clock, overlay, macro, profile, configuration, server, privacy, and troubleshooting documentation lives in the versioned [Wiki source](docs/wiki/Home.md). It can be synchronized to GitHub Wiki with `scripts/publish-wiki.sh` once GitHub enables Wiki for this private repository or the repository is deliberately made public.

## Building

Install JDK 25, then run:

```shell
./gradlew build
```

The remapped mod JAR is written to `build/libs/`. The Gradle wrapper distribution is protected by a committed SHA-256 checksum, and archives are configured for reproducible ordering and timestamps.

## Contributing and support

Read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting changes. Contributions use MPL-2.0 and require a [Developer Certificate of Origin 1.1](DCO) sign-off. Use the issue forms for ordinary bugs and features, and [private vulnerability reporting](SECURITY.md) for sensitive reports.

Publisher and maintainer: **Redstone787**.

## License and notices

Source code is available under the [Mozilla Public License 2.0](LICENSE). See [NOTICE.md](NOTICE.md), [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), [PRIVACY.md](PRIVACY.md), and the [asset provenance record](docs/ASSET_PROVENANCE.md) for additional information. MPL-2.0 permits use, modification, and distribution while requiring covered source changes and notices to remain available; it does not grant rights to contributor names, project names, or logos.

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
