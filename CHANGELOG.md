## [1.1.0] - 2026-10-18

### Overall
- New mod icon.

### Compatibility
- Added Minecraft 1.21, 1.21.3, and 1.21.5 support for Fabric and NeoForge.

## [1.0.2] - 2026-10-16

### Overall
- Mod Menu and the NeoForge and Forge mod lists now show the plain mod version (for example `1.0.1`) instead of one with the Minecraft version and loader appended (`1.0.1+26.3-fabric`). JAR file names are unchanged.
- Mod Menu (Fabric) now shows a Discord link on the mod's page.

## [1.0.1] - 2026-10-12

### Fixes
- The sky now follows the speed at which the server really runs. A tick rate the server cannot reach (for example `/tick rate 10000`) no longer makes the sun and moon race far ahead of the actual day and night on 26.x, or pulse and snap back on older versions.

### Compatibility
- Added Minecraft 26.3 support for Fabric and NeoForge.
- Updated development and minimum dependencies: Fabric Loader 0.19.5, Forge 47.4.26 (1.20.1), and NeoForge 21.1.259 (1.21.1), 21.4.158 (1.21.4), 21.11.45 (1.21.11), 26.1.2.116 (26.1.2), 26.2.0.90 (26.2), 26.3.0.76 (26.3).

### Architecture
- The visual time is now estimated on every version from the server's time packets and the measured tick rate, instead of the nominal tick rate (26.x) or the client's own tick counter (older versions).
- Updated build tooling: Gradle 9.8.1, Stonecutter 0.9.8, Loom Back Compat 0.4.3, Fabric Loom 1.18.4, NeoForge ModDevGradle 2.0.148, Mod Publish Plugin 2.2.1, and the Foojay toolchain resolver.

- CI now sets up JDK 17, 21, and 25 with the Gradle action, and uses current GitHub Actions versions.
- The Forge JAR no longer contains the unused shared `ticks.mixins.json`, and the source preprocessor no longer uses a Gradle API that Gradle 10 removes.

## [1.0.0-beta] - 2026-07-29

### Overall
- First beta: Fabric and NeoForge builds for Minecraft 1.21.1, 1.21.4, 1.21.11, 26.1, 26.1.2, and 26.2, plus Fabric and Forge builds for 1.20.1.

### Effects
- Reworked the visual time interpolation: sudden time changes such as `/time set` are blended over the shortest route, including across midnight.
- The blend between the server time and the smoothed time is frozen while the game is paused.

### Compatibility
- Added Minecraft 1.21.11, 26.1, 26.1.2, and 26.2 support, including the new world clock synchronization in 26.x.

### Architecture
- Switched to a single standard Gradle build for every Stonecutter target, with `buildAndCollect` for collecting release JARs and per-target unit tests in CI.

## [0.1.2-alpha] - 2026-07-27

### Overall
- Alpha release for Minecraft 1.20.1 (Fabric, Forge), 1.21.1, and 1.21.4 (Fabric, NeoForge).

### Config
- Settings are read directly from `config/ticks.json`; edit `transitionTimeMillis` while the game is closed.
