> Client-side Minecraft mod that smooths the celestial clock from the 20 Hz game-tick rate to the frame rate. 

It affects sky rendering only; world time, server synchronization, and game logic are unchanged. Dedicated-server installation is unnecessary.

[<img alt="modrinth" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg">](https://modrinth.com/mod/ticks/)
[<img alt="github" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg">](https://github.com/vercim/ticks)
<img alt="fabric" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/fabric_vector.svg">
<img alt="neoforge" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/neoforge_vector.svg">

## Structure

```text
dev/vercim/ticks/
|- TicksController        - Owns the client-only visual time for each frame
|- SkyTimeMath            - Celestial-angle and day-cycle calculations
└─ mixin/
   |- GameRendererMixin   - Starts and ends the frame scope
   |- ClientLevelMixin    - Detects world-time jumps
   └─ DimensionTypeMixin  - Supplies the smoothed sky angle
```

`TicksController` combines world time with the current partial tick. The Mixins restrict the override to sky rendering, so the authoritative world time and game simulation remain unchanged.

## File name format

Released JARs follow this pattern:

```text
ticks-0.1.0+1.21.1-fabric.jar
      |     |      |
      |     |      └─ Mod loader
      |     └─ Target Minecraft version
      └─ Mod version
```

## Multi-loader and multi-version setup

The project uses [Stonecutter](https://stonecutter.kikugie.dev/) to create four Gradle targets from one source tree: Fabric and Forge for 1.20.1, plus Fabric and NeoForge for 1.21.1. Shared Java sources and Mixin configuration live in `src/main`; loader metadata is stored in loader-specific resource directories.

Each target selects its build script: Fabric uses Fabric Loom (with Loom Back Compat for 1.20.1), Forge uses NeoForge ModDev LegacyForge, and NeoForge uses NeoForge ModDev. Stonecutter conditional compilation in the shared sources handles API differences between Minecraft versions, such as the `GameRenderer#render` signature.

## Requirements

No additional mod dependencies; Fabric API is not required

## Configuration

Ticks works automatically and has no configuration screen or config file. It is client-side only and does not need to be installed on a server.

## Build

Use the Gradle wrapper for the target you need:

```powershell
.\gradlew.bat :1.20.1-fabric:build
.\gradlew.bat :1.20.1-forge:build
.\gradlew.bat :1.21.1-fabric:build
.\gradlew.bat :1.21.1-neoforge:build
```

Artifacts are written to `versions/<target>/build/libs/`. On Linux or macOS, replace `./gradlew.bat` with `./gradlew`.

---

> If you've found a bug or a version incompatibility, or if you have a suggestion, please [post it here](https://github.com/vercim/ticks/issues). Here is a [simple guide](https://youtu.be/CVqOHDpVwDc) on how to do that.
