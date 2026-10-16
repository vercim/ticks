# CLAUDE.md

Ticks — client-side Minecraft mod that renders the sky (sun, moon, stars) at the frame rate instead of the game tick rate. Fabric + Forge + NeoForge, one shared source tree built with Stonecutter for 16 targets (Minecraft 1.20.1 – 26.3), Mojang mappings.

`AGENTS.md` is a symlink to this file.

## Docs map

| Document | Read it when | Update it when |
|---|---|---|
| [docs/WORKFLOW.md](docs/WORKFLOW.md) | building, running, switching targets (commands for Windows and macOS), conditional sources, CI, packaged-JAR validation, releasing, build troubleshooting | commands, tasks, CI, or the release flow change |
| [docs/VERSION_NOTES.md](docs/VERSION_NOTES.md) | porting to a new Minecraft version, or touching version-specific code and dependency versions | a target is added, a dependency is bumped, or you hit a version-specific API difference or quirk (the file explains how to fill it) |
| [CHANGELOG.md](CHANGELOG.md) | preparing a release | any player-visible change (see [Changelog](#changelog)) |

## Current state

- Version `1.0.2` (earlier: `1.0.1`, `1.0.0-beta`, `0.1.2-alpha`).
- Targets (declared once in `settings.gradle.kts`): `1.20.1-fabric`, `1.20.1-forge`, and `-fabric` / `-neoforge` for `1.21.1`, `1.21.4`, `1.21.11`, `26.1`, `26.1.2`, `26.2`, `26.3`. Per-target details: [docs/VERSION_NOTES.md](docs/VERSION_NOTES.md). Never add long-lived version branches.
- README texts and images are shared with the Modrinth and CurseForge pages; keep README edits minimal and user-facing.

## Rules that must not be forgotten

- Ticks only changes what the sky *looks like* during rendering. World time, server synchronization, and game logic must stay untouched: the override is visible only while a frame is being rendered (`TicksController.rendering`).
- IDE-only runs prove nothing about loader integration; validate the packaged JAR from `build/libs/<mod_version>/` in a clean client profile (steps in [docs/WORKFLOW.md](docs/WORKFLOW.md)).
- Every JAR must register its mixin config (`ticks.mixins.json` for Fabric and NeoForge, `ticks.forge.mixins.json` + refmap for Forge), and loader-specific classes must be absent from the other loaders' JARs. A missing registration makes the mod load while every injection silently does nothing.
- Build commands differ per OS (`.\gradlew.bat` on Windows, `./gradlew` on macOS/Linux); take them from [docs/WORKFLOW.md](docs/WORKFLOW.md) rather than guessing.
- The default Stonecutter target (`vcsVersion` in `settings.gradle.kts` and the committed `.sc_active_version`) is always the latest Minecraft version, Fabric build: currently `26.3-fabric`. The committed `src/` is in that target's state. Switch targets only with `Set active project to <target>` (it rewrites `src/`) and restore the default before committing (`Reset active project`); move both when a new version is ported ([docs/WORKFLOW.md](docs/WORKFLOW.md#default-target)).
- Conditional sources are plain Stonecutter comments (`//? if … {`), and the active target compiles straight from `src/`: after editing `.sc_active_version` by hand, or when a conditional block looks wrong, the sources are stale until `Set active project to <target>` or `Refresh active project` runs (see [Conditional sources](#conditional-sources)).

## Structure

```
src/main/java/dev/vercim/ticks/
  TicksController.java        — client-only visual time: per-frame target time, server time packet hooks,
                                sky-angle override
  ServerTimeEstimator.java    — server day time between time packets: last packet + measured tick rate (no MC classes)
  TickRateEstimator.java      — ticks per second the server really runs, from its time packets (no MC classes)
  SkyTimeInterpolator.java    — exponential blend of the rendered time toward the target (no MC classes)
  SkyTimeMath.java            — pure math: shortest day-wrap difference, vanilla sky-angle curve
  TicksConfig.java            — load/save of config/ticks.json (transitionTimeMillis)
  mixin/
    GameRendererMixin.java    — frame begin/end hook via GameRenderer.render (<1.21.11)
    ClientLevelMixin.java     — server time packets (setDayTime ignoring the client's own tick on <1.21.4,
                                setTimeFromServer on 1.21.4–1.21.11; <26)
    DimensionTypeMixin.java   — overrides DimensionType.timeOfDay during rendering (<1.21.11)
    SkyRendererMixin.java     — 1.21.11 only: frame hook + sun/moon/star angles in SkyRenderState
    SkyRenderer26Mixin.java   — 26.x: same on the 26.x SkyRenderState + overworld clock
    ClientClockManagerMixin.java — 26.x: world-clock updates → onClockUpdate, game-time-only syncs → onGameTimeUpdate
  fabric/TicksFabric.java, forge/TicksForge.java, neoforge/TicksNeoForge.java — loader entry points (config init),
                                each wrapped in its loader condition
src/fabric/resources/fabric.mod.json
src/forge/resources/   — META-INF/mods.toml, pack.mcmeta, ticks.forge.mixins.json
src/neoforge/resources/META-INF/neoforge.mods.toml
src/main/resources/    — ticks.mixins.json (template), assets/ticks/icon.png
src/test/java/dev/vercim/ticks/ — JUnit 5 tests for SkyTimeMath, SkyTimeInterpolator, ServerTimeEstimator, TickRateEstimator, TicksConfig
build.fabric.gradle.kts / build.forge.gradle.kts / build.neoforge.gradle.kts — per-loader build + publishing
stonecutter.gradle.kts — loader constants for conditions, `runActiveClient`, `buildActive`, `clean`
```

Metadata files (`fabric.mod.json`, `mods.toml`, `neoforge.mods.toml`, `ticks.mixins.json`) are templates: Gradle `expand`s `${...}` values from `gradle.properties` and the build scripts, so edit values there, not in the generated JAR.

## Architecture

Per rendered frame `TicksController.beginFrame(partialTick)` computes the *target* time and feeds it to `SkyTimeInterpolator`. The interpolator keeps an offset between rendered and target time and decays it exponentially over `transitionTimeMillis`; when a time jump is flagged (`markTimeJump`) it re-bases the offset along the shortest route around the 24000-tick day. The vanilla sky-angle curve is then applied to the fractional rendered time (`SkyTimeMath.skyAngle`) and used only until `endFrame()`.

The target is the server's day time as `ServerTimeEstimator` extrapolates it: the last server time packet, advanced at the **measured** speed of the server's game clock (`TickRateEstimator`, from the game times of the packets). Neither the nominal tick rate (`/tick rate`) nor the client's own ticking can be used for this: a server often runs slower than its nominal rate (`/tick rate 10000`, or just lag), and the client ticks at the nominal rate regardless. Every packet re-anchors the estimate, so a wrong speed cannot accumulate into drift. Until the first packet of a level arrives the client's own time is used.

- **<1.21.11:** `GameRendererMixin` brackets `GameRenderer.render` with `beginFrame` / `endFrame`; `DimensionTypeMixin` replaces `DimensionType.timeOfDay(long)` while `rendering` is set and the requested day time matches the tracked level.
- **1.21.11 and 26.x:** the sky is extracted to a `SkyRenderState`; `SkyRendererMixin` / `SkyRenderer26Mixin` call `beginFrame` at the head of `extractRenderState` and write the sun, star, and moon angles at its return, then `endFrame`. `DimensionTypeMixin` has no effect there.
- **Time packets:** `ClientLevelMixin` (`setDayTime` on <1.21.4, where the client's own per-tick calls are skipped through `tickTime`; `setTimeFromServer` on 1.21.4–1.21.11) forwards each server packet's game time and day time. On 26.x `ClientClockManagerMixin` forwards overworld clock updates, and the periodic game-time-only syncs (empty clock map) that carry the speed measurement. Do not hook `ClientPacketListener.handleSetTime`: NeoForge's custom time payload bypasses it.
- **Time jumps:** a packet whose day time differs from the previous one by more than the game ticks between them allow (`/time set`, sleeping) is flagged as a jump (`markTimeJump`); large corrections after lag are flagged too, so they blend instead of snapping.
- **Pause and freeze:** while `Minecraft.isPaused()` or the tick rate manager does not run normally (`/tick freeze`) the estimate holds still, and the interpolator advances no time, so the blend does not drain during a pause menu.
- **Fixed-time dimensions** (`hasFixedTime`) and non-tracked levels are never overridden.

Keep `SkyTimeMath` and `SkyTimeInterpolator` free of Minecraft classes so they stay unit-testable.

## Conditional sources

Version- and loader-specific code uses Stonecutter conditions, `//? if <condition> {` … `//?}`; the code of an inactive block is commented out as `/* … *///?}`. A condition is a loader constant (`fabric`, `forge`, `neoforge`, defined in `stonecutter.gradle.kts`) or a Minecraft version predicate (`>=1.21.4`, `<26`), and may use `else`, `else if`, and `&&`. The active target (`.sc_active_version`) compiles straight from `src/`, every other target from a generated copy under `versions/<target>/build/generated/stonecutter`, so the committed tree always shows the default target's state. Details and examples: [docs/WORKFLOW.md](docs/WORKFLOW.md#conditional-sources). Express Minecraft-version differences with narrow local conditions, never by copying files.

## Config

`config/ticks.json` holds one key, `transitionTimeMillis` (default 200, range 0–2000, clamped on load and on set). The file is created on first start; unreadable or malformed files fall back to the default with a log warning. There is no config screen and no versioning: keep the file name and key stable, and add new keys with defaults so older files keep loading. The mod has no user-visible text, so no language files exist.

## Coding style

Tabs for indentation, braces on the same line, grouped imports, `final` utility classes with private constructors, `UpperCamelCase` types, `lowerCamelCase` members, `UPPER_SNAKE_CASE` constants. No formatter or linter is configured; match neighboring code. Add short Javadoc where timing or cyclic math is not obvious.

## Testing

JUnit Jupiter 5 runs per target as part of `build` (CI runs all 16). Name test files `*Test.java` and methods after observable behavior (for example `midnightUsesTheShortForwardRoute`). Add deterministic unit tests for interpolation, wrapping, and discontinuities; run one target with `:1.21.1-fabric:test` while narrowing a failure. Mixins and rendering cannot be unit-tested: use the packaged-JAR checklist in [docs/WORKFLOW.md](docs/WORKFLOW.md#packaged-jar-validation).

## Changelog

`CHANGELOG.md` is user-facing and is published verbatim as the release notes on Modrinth, CurseForge, and GitHub. Keep it current as part of the change, not at release time.

- Add an entry for every player-visible change: new or changed behavior, config options and defaults, fixes, compatibility (new versions/loaders, dependency requirements), and notable architecture or documentation changes. Skip internal-only edits.
- Format: `## [<mod_version>] - YYYY-MM-DD`, newest first; group bullets under `###` headings already in use (`Overall`, `Effects`, `Config`, `Fixes`, `Architecture`, `Compatibility`, `Documentation`); one short bullet per change.
- The version must equal `mod_version` in `gradle.properties` (including `-alpha` / `-beta`). The release workflow fails if the section is missing and publishes everything up to the next `## [` heading, so never nest another `## [` heading inside a section.
- Unreleased work sits under `## [Unreleased]` at the top; rename it when releasing.
- Bumping `mod_version` and adding its changelog section happen together. Full rules and release steps: [docs/WORKFLOW.md](docs/WORKFLOW.md#changelog).

## Commits and pull requests

Imperative, title-case commit subjects (for example `Add Minecraft 26.2 support`); keep commits focused. Pull requests explain behavior changes, list the targets and commands tested, link issues, and call out compatibility implications for mixins, shader packs, or Minecraft versions.

## Porting to a new Minecraft version

Add the two targets in `settings.gradle.kts` (loader versions in `gradle.properties` and the per-loader build script) and fix API differences with narrow conditions — never a new long-lived branch. The checklist, the baseline API table, and the notes from every earlier port are in [docs/VERSION_NOTES.md](docs/VERSION_NOTES.md); fill its entry for the new version as you go. Move the default target to the new version's Fabric target. Start with the frame hook and sky-render mixins, then the time-update hook.

## Common issues

Build and run problems (wrong JDK, missing symbols in a generated target, wrong client): [docs/WORKFLOW.md](docs/WORKFLOW.md#troubleshooting). Code-level pitfalls:

- **Sky does not move smoothly / mixin has no effect** → check the mixin config is registered in the packaged JAR and that the version-specific mixin is listed (`${sky_renderer_mixin}` / `${clock_manager_mixin}` in `ticks.mixins.json` are filled by each loader build script).
- **`required: true` mixin failure at startup** → a mixin target changed in that Minecraft version; `injectors.defaultRequire` is `1`, so a broken injection crashes loudly instead of silently doing nothing; keep it.
- **Sky jumps after `/time set` or a server time sync** → the jump was not flagged; check the time-update hook for that version in `ClientLevelMixin` / `ClientClockManagerMixin`.
- **Sky runs faster or slower than the real day/night (high `/tick rate`, lagging server)** → the speed estimate is wrong; check that every periodic time packet (on 26.x including the one with an empty clock map) reaches `TicksController`, and that `ServerTimeEstimator.getTickRate()` matches the ticks per second the server really runs.
