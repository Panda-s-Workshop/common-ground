# Common Ground

[![Build](https://github.com/Panda-s-Workshop/common-ground/actions/workflows/build.yml/badge.svg)](https://github.com/Panda-s-Workshop/common-ground/actions/workflows/build.yml)

A Minecraft 26.3 mod for NeoForge (26.3.0.89) and Fabric (Fabric API 0.161.0), Java 25. Server side only.
The store page link will go here at release.

Biome mods' biomes count as the vanilla biomes they resemble, so every mob mod's mobs spawn there. Mob mods give
their mobs vanilla biomes, by id or by tag; a biome mod's forest is neither, so those mobs thin out or vanish there.
Common Ground maps each modded biome onto its vanilla twin for spawning, with presets for the popular biome mods and
one config line for anything else. Terrain, features and structures stay as the biome mod made them.

Work in progress: nothing is released yet.

## Build and run

JDK 25 is pinned in `gradle.properties` (`org.gradle.java.home`). CI overrides it.

```bash
./gradlew build
```

```bash
./gradlew :neoforge:runGameTestServer
```

- `build` writes `neoforge/build/libs/commonground-<version>-neoforge.jar` and
  `fabric/build/libs/commonground-<version>-fabric.jar`. The Fabric jar needs Fabric API.
- `./gradlew :neoforge:runGameTestServer -PgroundTests=mod_loaded,lang_*` runs only the GameTests named.
- The dev servers listen on port 25617.

License: All Rights Reserved.
