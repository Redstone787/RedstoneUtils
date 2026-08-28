# Third-party notices

Redstone Utils is built against the following projects. These dependencies are resolved by Gradle and are not bundled into the remapped mod JAR unless a future build explicitly says otherwise.

| Project | Purpose | License |
| --- | --- | --- |
| [Fabric Loader](https://github.com/FabricMC/fabric-loader) | Mod loader | Apache License 2.0 |
| [Fabric API](https://github.com/FabricMC/fabric) | Minecraft/Fabric APIs | Apache License 2.0 |
| [Fabric Loom](https://github.com/FabricMC/fabric-loom) | Gradle build plugin | MIT License |
| [Gradle](https://github.com/gradle/gradle) | Build system and committed wrapper JAR | Apache License 2.0 |
| [JUnit 5](https://github.com/junit-team/junit5) | Test framework | Eclipse Public License 2.0 |

The Gradle wrapper JAR contains its own license material. Transitive build and game dependencies remain subject to their respective licenses.

Minecraft is proprietary software owned by Microsoft/Mojang. Redstone Utils requires a legitimate Minecraft installation and does not grant any rights to Minecraft. No Minecraft game files or Mojang textures are intentionally included in this repository.

The project icon was generated specifically for Redstone Utils with OpenAI image generation without a supplied source image, then manually reviewed. The wire-menu icons are original project artwork generated from the source in `art/`. See [docs/ASSET_PROVENANCE.md](docs/ASSET_PROVENANCE.md) for the full provenance record.
