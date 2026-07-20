# Ticks

Ticks is a client-side Minecraft mod that renders the celestial clock at the frame rate instead of the 20 Hz game-tick rate. It targets Minecraft 1.21.1 and is built from the same source for Fabric and NeoForge.

The mod changes only the time value observed by the sky-rendering path. The authoritative world time, server synchronization, scheduled ticks, game rules, redstone, entities, and all other simulation logic remain untouched.

## Runtime behavior

At the start of every rendered frame, `TicksController` samples `ClientLevel#getDayTime()` and adds the current partial tick while `doDaylightCycle` is enabled. During that frame, calls to `DimensionType#timeOfDay(long)` for the active client level receive a celestial angle calculated from this fractional time. Outside the render call, the vanilla result is preserved.

Discontinuous updates are handled separately from normal clock progression. When `ClientLevel#setDayTime(long)` changes the clock by anything other than the expected single tick, Ticks:

1. preserves the currently rendered sky position;
2. computes the shortest signed offset on the 24,000-tick day cycle;
3. decays that offset exponentially with a time constant of 0.20 seconds.

The decay uses real frame time (`System.nanoTime()`), so transition duration does not depend on frame rate. A new level initializes directly from its current time, and fixed-time dimensions keep vanilla behavior.

## Implementation

The implementation is loader-neutral apart from the NeoForge bootstrap class and platform metadata. Three client mixins provide the render boundary and time interception:

| Mixin | Target | Responsibility |
| --- | --- | --- |
| `GameRendererMixin` | `GameRenderer#render` | Opens and closes the per-frame override scope. |
| `ClientLevelMixin` | `ClientLevel#setDayTime` | Detects discontinuous clock updates. |
| `DimensionTypeMixin` | `DimensionType#timeOfDay` | Supplies the fractional celestial angle only for the active rendered level. |

`SkyTimeMath` contains the loader-independent cyclic-difference and vanilla celestial-angle calculations. Its tests also verify midnight wrapping and frame-rate-independent decay.

## Supported platforms

| Component | Build/test version | Declared runtime range |
| --- | --- | --- |
| Minecraft | 1.21.1 | exactly 1.21.1 |
| Java | 21 | 21 or newer on Fabric; Java 21 is required by Minecraft 1.21.1 tooling |
| Fabric Loader | 0.19.2 | 0.16.10 or newer |
| NeoForge | 21.1.231 | 21.1.231 or newer, with Minecraft constrained to 1.21.1 |

Fabric API is not required. Ticks uses only Fabric Loader, SpongePowered Mixin, and vanilla client classes.

## Building and testing

The repository uses the Gradle wrapper and Stonecutter to generate two version projects from the shared sources. A local JDK is optional when Gradle can provision the Java 21 toolchain.

On Windows:

```powershell
.\gradlew.bat :1.21.1-fabric:build
.\gradlew.bat :1.21.1-neoforge:build
```

On Linux or macOS:

```bash
./gradlew :1.21.1-fabric:build
./gradlew :1.21.1-neoforge:build
```

Each `build` invocation compiles Java, processes the platform manifest, runs the JUnit suite, and produces both a distributable JAR and a sources JAR. Outputs are written to:

- `versions/1.21.1-fabric/build/libs/`
- `versions/1.21.1-neoforge/build/libs/`

Run a specific development client with:

```powershell
.\gradlew.bat :1.21.1-fabric:runClient
.\gradlew.bat :1.21.1-neoforge:runClient
```

`runActiveClient` is a convenience task that follows the target named in `.sc_active_version`:

```powershell
Set-Content -NoNewline .sc_active_version "1.21.1-fabric"
.\gradlew.bat runActiveClient
```

Use `1.21.1-neoforge` instead to make NeoForge the active target. The `.sc_active_version` file must exist before running any Gradle task because Stonecutter reads it during project configuration.

## Project layout

- `src/main/java/` — shared controller, math, mixins, and conditionally compiled NeoForge bootstrap.
- `src/main/resources/` — shared mixin configuration and icon.
- `src/fabric/resources/` — Fabric-only `fabric.mod.json`.
- `src/neoforge/resources/` — NeoForge-only `neoforge.mods.toml`.
- `src/test/java/` — loader-independent JUnit tests.
- `build.fabric.gradle.kts` and `build.neoforge.gradle.kts` — platform build definitions.
- `gradle.properties` — canonical mod metadata and supported dependency versions.
- `stonecutter.gradle.kts` — active-target and shared Stonecutter configuration.

## Compatibility notes

The mod affects vanilla rendering paths that obtain the celestial angle from `DimensionType#timeOfDay`. A shader pack or rendering mod that calculates sky time independently may bypass the override. Mods that invoke the same vanilla method during the scoped `GameRenderer#render` call will observe the smoothed angle; calls outside rendering receive the authoritative vanilla value.

Ticks has no configuration file, networking protocol, persistent state, or server component. Installing it on a dedicated server is unnecessary.

## Links and license

- Source: <https://github.com/vercim/ticks>
- Issues: <https://github.com/vercim/ticks/issues>

Ticks is distributed under the GNU Lesser General Public License v3.0 only (`LGPL-3.0-only`). See [LICENSE](LICENSE).
