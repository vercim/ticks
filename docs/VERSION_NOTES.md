# Version Notes

Per-Minecraft-version notes: exact dependency versions, API differences, and quirks. Read this before porting to a new version or changing a version-specific condition. Build and release steps are in [WORKFLOW.md](WORKFLOW.md); player-facing changes go in [../CHANGELOG.md](../CHANGELOG.md).

## How to use and fill this file

**When to write here**

- You add a target to `settings.gradle.kts` → add a row to the tables below and an entry for the version.
- You bump a dependency of an existing target → update the dependency table.
- You hit an API difference, a quirk, or a bug specific to one version → record it in that version's entry.
- You finish testing a target's packaged JAR → update its `Verified` line.

**What to put in an entry**

- *Dependencies:* Java and loader notes that differ from the previous entry; the exact values go into the [Dependencies per target](#dependencies-per-target) table, copied from `gradle.properties` or the loader build script.
- *API differences:* only what differs from the previous entry (the [Baseline](#baseline) table lists it per column). Give the Minecraft signature, the file that adapts to it, and the Stonecutter condition used (keep conditions narrow).
- *Quirks:* anything that cost you time: crashes, mixin targets that moved, loader behavior.
- *Verified:* date, mod version, and which checklist items you ran on the packaged JAR.
**What not to put here**

- General rules and architecture → `CLAUDE.md` / `AGENTS.md`.
- Anything players should read → `CHANGELOG.md`.
- Step-by-step commands → `WORKFLOW.md`.

**Conventions**

- Newest Minecraft version first in the entries; loaders as sub-notes.
- Never delete an entry when a target is dropped: move it to [Archived ports](#archived-ports).
- Keep entries factual and short; link to commits or tags instead of pasting logs.

### Template

```md
### MC X.Y.Z — Fabric / NeoForge

Targets: `X.Y.Z-fabric`, `X.Y.Z-neoforge` · first mod version: A.B.C · date: YYYY-MM-DD

#### Dependencies
- …

#### API differences (vs. previous entry)
- …

#### Quirks
- …

#### Verified
- YYYY-MM-DD, mod A.B.C: Fabric ☐ NeoForge ☐ (checklist in WORKFLOW.md → Packaged-JAR validation)
```
## Dependencies per target

Values come from `gradle.properties` and the loader build scripts; copy, do not paraphrase.

| Minecraft | Java | Fabric Loader (dev / min) | NeoForge / Forge (dev) |
|---|---:|---|---|
| 1.20.1 | 17 | `0.15.11` / `0.14.21` (pinned in `build.fabric.gradle.kts`) | Forge `1.20.1-47.4.26` (`forge_loader_version_range=[47,)`) |
| 1.21 | 21 | `fabric_loader_version` / `fabric_loader_min_version` | `neoforge_1_21_version=21.0.167` |
| 1.21.1 | 21 | same | `neoforge_version=21.1.259` |
| 1.21.3 | 21 | same | `neoforge_1_21_3_version=21.3.100` |
| 1.21.4 | 21 | same | `neoforge_1_21_4_version=21.4.158` |
| 1.21.5 | 21 | same | `neoforge_1_21_5_version=21.5.101` |
| 1.21.6 | 21 | same | `neoforge_1_21_6_version=21.6.20-beta` |
| 1.21.8 | 21 | same | `neoforge_1_21_8_version=21.8.57` |
| 1.21.10 | 21 | same | `neoforge_1_21_10_version=21.10.64` |
| 1.21.11 | 21 | same | `neoforge_1_21_11_version=21.11.45` |
| 26.1 | 25 | same | `neoforge_26_1_version=26.1.0.19-beta` |
| 26.1.2 | 25 | same | `neoforge_26_1_2_version=26.1.2.116` |
| 26.2 | 25 | `fabric_loader_version` / `fabric_loader_26_2_min_version=0.19.0` | `neoforge_26_2_version=26.2.0.90` |
| 26.3 | 25 | same as 26.2 (`fabric_loader_26_2_min_version`) | `neoforge_26_3_version=26.3.0.76` |

Current common values: `fabric_loader_version=0.19.5`, `fabric_loader_min_version=0.16.10`, `neoforge_loader_version_range=[4,)`. Ticks has no mod dependencies beyond the loader.

Fabric dev clients additionally load Fabric API and Mod Menu through `modLocalRuntime` (`build.fabric.gradle.kts`), so the mod list can be checked without installing anything; neither is on the compile classpath or in the JAR. Versions are in `gradle.properties` as `modmenu_version_<minecraft version with _>` and `fabric_api_version_<minecraft version with _>`; a new Minecraft version needs both entries, or the Fabric build fails with a missing property. Mod Menu versions: `7.2.2` (1.20.1), `11.0.3` (1.21), `11.0.5` (1.21.1), `12.0.1` (1.21.3), `13.0.4` (1.21.4), `14.0.0` (1.21.5), `15.0.0` (1.21.6, 1.21.8), `16.0.0` (1.21.10), `17.0.1` (1.21.11), `18.0.2` (26.1, 26.1.2), `20.0.3` (26.2), `21.0.0` (26.3).

Fabric API is needed because Mod Menu requires some of its modules and Loom removes the ones nested in the Mod Menu JAR; the POMs of Mod Menu 11.0.3, 11.0.5, 13.0.4, and 17.0.1 do not declare them either, so Gradle would not pull them in (the client then stops with "Incompatible mods found"). NeoForge and Forge have a built-in mod list and need nothing.

Build tooling versions live in `settings.gradle.kts` (Stonecutter, Loom Back Compat, NeoForge ModDevGradle, Mod Publish Plugin, Foojay) and `gradle/wrapper/gradle-wrapper.properties`. Keep `loomx.loom_version` in `gradle.properties` equal to the `fabric-loom-remap` version in `stonecutter.gradle.kts`. Before bumping any of them, check that Stonecutter, Loom Back Compat, Fabric Loom, and NeoForge ModDev still work together.

Metadata ranges:

- Fabric: `minecraft` is an **exact** match of the target's Minecraft version, `java` is `>=` the target's Java version.
- NeoForge: Minecraft `[X, X')` where `X'` is the target version with its last component incremented (`26.1.2` → `26.1.3`; `1.21` counts as `1.21.0` → `1.21.1`); NeoForge itself `[neoforge_version,)`.
- Forge (1.20.1): Minecraft `[1.20.1,1.20.2)`, Forge `[47,)`.

## Baseline

Conditions are written relative to the Minecraft version of the target (`>=1.21.4`, `<26`). The behavior of the oldest hook set is the **MC 1.21 / 1.21.1** column below; each later column lists only what changed.

| Area | 1.20.1 | 1.21 / 1.21.1 (baseline) | 1.21.3 – 1.21.10 | 1.21.11 | 26.x |
|---|---|---|---|---|---|
| Frame hook | `GameRenderer.render(float partialTick, long nanoTime, boolean tick)` HEAD / RETURN (`GameRendererMixin`, `<1.21`) | `GameRenderer.render(DeltaTracker, boolean)`; partial tick from `deltaTracker.getGameTimeDeltaPartialTick(false)` (`>=1.21 <1.21.11`) | same | `SkyRenderer.extractRenderState(ClientLevel, float, Camera, SkyRenderState)` HEAD / RETURN (`SkyRendererMixin`) | same, with `SkyRenderState` in `net.minecraft.client.renderer.state.level` (`SkyRenderer26Mixin`) |
| Sky angle | `DimensionType.timeOfDay(long)` overridden while rendering (`DimensionTypeMixin`, `<1.21.11`) | same | same | `renderState.sunAngle` / `starAngle` / `moonAngle` written at RETURN (angle × 2π; moon + π) | same |
| Server time update | `ClientLevel.setDayTime(long)` HEAD, skipped while the client's own `tickTime()` runs; a negative day time means the day does not advance; game time is already set | same | `ClientLevel.setTimeFromServer(long gameTime, long dayTime, boolean tickDayTime)` HEAD; `dayTimeRate` = 1 or 0 from `tickDayTime` | same | `ClientClockManager.handleUpdates(long, Map<Holder<WorldClock>, ClockNetworkState>)` HEAD: the overworld clock state when present, otherwise a game-time-only sync (`ClientClockManagerMixin`) |
| Current sky time | `level.getDayTime()` | same | same | same | `level.getOverworldClockTime()` |
| Day time rate | `GameRules.RULE_DAYLIGHT` (`<1.21.2`), read every frame | same | `dayTimeRate` (`>=1.21.2`) | same | clock `rate` of the last overworld clock update |
| Target time per frame | `ServerTimeEstimator.estimate`; before the first packet of a level `getDayTime()` + partial tick if the daylight rule is on | same | same, with `partialTick * dayTimeRate` before the first packet | same | `ServerTimeEstimator.estimate`; before the first packet `getOverworldClockTime()` |
| Mixin config template | `ticks.forge.mixins.json` (Forge) / `ticks.mixins.json` | `ticks.mixins.json` | same | adds `SkyRendererMixin` | adds `SkyRenderer26Mixin` and `ClientClockManagerMixin` |

Mixin class source for each column is in `src/main/java/dev/vercim/ticks/mixin/`; the `${sky_renderer_mixin}` / `${clock_manager_mixin}` placeholders are filled in `build.fabric.gradle.kts` and `build.neoforge.gradle.kts` (the Forge target has none and uses its own `ticks.forge.mixins.json`).

## Porting checklist

For each new version, after adding the targets:

- Add the targets to `settings.gradle.kts`; add the NeoForge (and Forge) version property and the `when` branches in the loader build scripts (`neoForgeVersion`, `javaVersion`, the mixin placeholder expansion).
- Extend the explicit version lists, which fall back to the old behavior for an unknown version: `fabricLoaderMinVersion` in `build.fabric.gradle.kts` and `modIcon` in `build.neoforge.gradle.kts` (both name `26.2` / `26.3` literally).
- Move the default target (`vcsVersion` in `settings.gradle.kts` and `.sc_active_version`) to the new version's Fabric target, run `Set active project to <target>` so `src/` follows, and move the **default target** mark below to its entry.
- Frame hook: is `GameRenderer.render` still the right place, or has sky extraction moved (as in 1.21.11)?
- Sky angle: `DimensionType.timeOfDay`, `SkyRenderState` fields and their package, `SkyRenderer.extractRenderState` signature.
- Time update hook: `ClientLevel.setDayTime` / `setTimeFromServer` / the clock manager; confirm jumps (`/time set`) are detected.
- Time advancement: daylight rule, `tickDayTime`, tick rate / freeze handling.
- Express differences with narrow conditions in the shared tree, not copied files; update `ticks.mixins.json` placeholders if a mixin class is added.
- Mixin config is registered in each packaged JAR; Forge refmap present.
- Run `:<target>:test` and the packaged-JAR validation in [WORKFLOW.md](WORKFLOW.md) for each loader.
- Fill this file's entry for the version; add a `CHANGELOG.md` line under `Compatibility`.

Likely trouble spots, in order: the frame/sky hook, the time-update hook, then metadata and dependency version ranges.

## Target entries

### MC 26.x — Fabric / NeoForge

Targets: `26.1`, `26.1.2`, `26.2`, `26.3` (each `-fabric` and `-neoforge`) · first mod version: 1.0.0-beta (`26.3`: 1.0.1) · date: 2026-07-29 (`26.3`: 2026-10-12) · **default target (`vcsVersion`): `26.3-fabric`**

#### Dependencies
- Java 25; unobfuscated Minecraft (no remapping, `isUnobfuscated` in `build.fabric.gradle.kts`).
- 26.2 and 26.3 need Fabric Loader `>=0.19.0` (`fabric_loader_26_2_min_version`, shared by both in `build.fabric.gradle.kts`).
- `26.1` uses a NeoForge beta (`26.1.0.19-beta`); revisit when a stable build appears. NeoForge `26.3.0.76` is a stable build.

#### API differences (vs. 1.21.11)
- World time comes from the overworld clock, not `getDayTime()`. The server sends the clock state only when it changes (`/time set`, rate, pause, join); the periodic sync every 20 ticks has an empty clock map and carries the game time only, so the day time between clock updates is `clock time + (gameTime - clock gameTime) * rate`. `ServerTimeEstimator` derives it that way and measures the real tick rate from those periodic game times.
- 26.3 needed no source changes: the 26.2 hooks (`SkyRenderer.extractRenderState`, `ClientClockManager.handleUpdates`, `SkyRenderState` angles) have identical signatures, and the Fabric dev client runs with all five mixins applied.
- NeoForge 26.2.0.90 and newer read `iconFile` / `iconBlur` (square icons) and `bannerFile` in `neoforge.mods.toml`. Older NeoForge (21.x, 26.1.x, 26.2 betas) knows only `logoFile`, so `build.neoforge.gradle.kts` fills `${mod_icon}` per target.

#### Quirks
- NeoForge 26.2.0.90 and newer deprecate `logoFile` / `logoBlur` and show a warning screen at launch when they are used.

#### Verified
- In-game validation not recorded (see WORKFLOW.md → Packaged-JAR validation).

### MC 1.21.11 — Fabric / NeoForge

Targets: `1.21.11-fabric`, `1.21.11-neoforge` · first mod version: 1.0.0-beta · date: 2026-07-29

#### API differences (vs. 1.21.4)
- Sky rendering is extracted to `SkyRenderState`; `DimensionType.timeOfDay` is no longer the hook (`DimensionTypeMixin` is empty here). `SkyRendererMixin` is the only hook and also begins/ends the frame.
- Time updates still arrive through `ClientLevel.setTimeFromServer`.

#### Verified
- In-game validation not recorded.

### MC 1.21.10 — Fabric / NeoForge

Targets: `1.21.10-fabric`, `1.21.10-neoforge` · first mod version: 1.1.0 · date: 2026-10-18

#### Dependencies
- NeoForge `21.10.64`; Fabric API `0.138.4+1.21.10` and Mod Menu `16.0.0` (declares `minecraft >=1.21.9- <1.21.11`) for the dev client.

#### API differences (vs. 1.21.8)
- None for the hooks. `SkyRenderer.extractRenderState(ClientLevel, float, Vec3, SkyRenderState)` and `SkyRenderState` already exist (the render state arrives with 1.21.9), but `DimensionType.timeOfDay` is still the sky-angle source: `SkyRenderer.extractRenderState` reads it through `ClientLevel.getSunAngle` / `getTimeOfDay`, and the whole chain runs inside `GameRenderer.render` (`render` → `renderLevel` → `LevelRenderer.renderLevel` → `SkyRenderer.extractRenderState`), so `GameRendererMixin` + `DimensionTypeMixin` keep working. The 1.21.11 signature (`Camera` instead of `Vec3`) is what `SkyRendererMixin` targets, so it must not be enabled below 1.21.11 (checked with `javap`).
- NeoForge `21.9` (1.21.9) exists only as a beta, so 1.21.9 is not ported.

#### Verified
- In-game validation not recorded.

### MC 1.21.8 — Fabric / NeoForge

Targets: `1.21.8-fabric`, `1.21.8-neoforge` · first mod version: 1.1.0 · date: 2026-10-18

#### Dependencies
- NeoForge `21.8.57`; Fabric API `0.136.1+1.21.8` and Mod Menu `15.0.0` (declares `minecraft ~1.21.6`) for the dev client.

#### API differences (vs. 1.21.5)
- None. The 1.21.6 rendering rework (`SkyRenderer` split out of `LevelRenderer`, GPU buffers) did not touch the hooks: `GameRenderer.render(DeltaTracker, boolean)`, `ClientLevel.setTimeFromServer(long, long, boolean)`, `DimensionType.timeOfDay(long)` (still reached through `LevelTimeAccess.getTimeOfDay`, which `LevelRenderer` calls for the sun angle), `TickRateManager`, and `Minecraft.isPaused()` are as on 1.21.5 (checked with `javap`).
- NeoForge `21.6` and `21.7` (1.21.6, 1.21.7) exist only as betas; see the 1.21.6 entry.

#### Verified
- In-game validation not recorded.

### MC 1.21.6 — Fabric / NeoForge

Targets: `1.21.6-fabric`, `1.21.6-neoforge` · first mod version: 1.1.0 · date: 2026-10-18

#### Dependencies
- NeoForge `21.6.20-beta`: no stable NeoForge build exists for 1.21.6 (the same holds for 1.21.7, 1.21.9, and 1.21.2), so the target uses the last beta, as 26.1 does. Fabric API `0.128.2+1.21.6` and Mod Menu `15.0.0` (declares `minecraft ~1.21.6`) for the dev client.

#### API differences (vs. 1.21.5)
- None for the hooks. The 1.21.6 rendering rework (`SkyRenderer` split out of `LevelRenderer`, GPU buffers) left `GameRenderer.render(DeltaTracker, boolean)`, `ClientLevel.setTimeFromServer(long, long, boolean)`, `DimensionType.timeOfDay(long)` (reached through `LevelTimeAccess.getTimeOfDay`, which `LevelRenderer` calls for the sun angle), `TickRateManager`, and `Minecraft.isPaused()` unchanged (checked with `javap`).

#### Verified
- In-game validation not recorded.

### MC 1.21.5 — Fabric / NeoForge

Targets: `1.21.5-fabric`, `1.21.5-neoforge` · first mod version: 1.1.0 · date: 2026-10-18

#### Dependencies
- NeoForge `21.5.101`; Fabric API `0.128.2+1.21.5` and Mod Menu `14.0.0` (declares `minecraft >=1.21.5 <1.21.6`) for the dev client.

#### API differences (vs. 1.21.4)
- None. Checked with `javap` on the Mojang-mapped 1.21.5 jar: `GameRenderer.render(DeltaTracker, boolean)`, `ClientLevel.setTimeFromServer(long, long, boolean)`, `DimensionType.timeOfDay(long)` (still called through `LevelTimeAccess.getTimeOfDay`, which `Level.getSunAngle` and the sky renderer use), `TickRateManager`, `Minecraft.isPaused()`.

#### Verified
- In-game validation not recorded.

### MC 1.21.4 — Fabric / NeoForge

Targets: `1.21.4-fabric`, `1.21.4-neoforge` · first mod version: 0.1.2-alpha · date: 2026-07-27

#### API differences (vs. 1.21.3)
- None.

#### Verified
- In-game validation not recorded.

### MC 1.21.3 — Fabric / NeoForge

Targets: `1.21.3-fabric`, `1.21.3-neoforge` · first mod version: 1.1.0 · date: 2026-10-18

#### Dependencies
- NeoForge `21.3.100`; Fabric API `0.114.1+1.21.3` and Mod Menu `12.0.1` for the dev client (Mod Menu 12.x declares `minecraft >=1.21.2-`).

#### API differences (vs. 1.21.1)
- `ClientLevel.setDayTime` is replaced by `setTimeFromServer(gameTime, dayTime, tickDayTime)`; the daylight gamerule no longer drives the time, `tickDayTime` does (`dayTimeRate`). This arrives with 1.21.2, not 1.21.4: `ClientLevel.getGameRules()` is gone and the first 1.21.3 build failed on it, so the conditions say `>=1.21.2` / `<1.21.2` (`TicksController`, `ClientLevelMixin`).
- The frame hook, `DimensionType.timeOfDay`, and `TickRateManager` are unchanged (checked with `javap` on the Mojang-mapped 1.21.3 jar).

#### Verified
- In-game validation not recorded.

### MC 1.21.1 — Fabric / NeoForge

Targets: `1.21.1-fabric`, `1.21.1-neoforge` · first mod version: 0.1.2-alpha · date: 2026-07-27

#### API differences (baseline)
- `GameRenderer.render(DeltaTracker, boolean)`; the partial tick must be read from the delta tracker with `getGameTimeDeltaPartialTick(false)`.

#### Quirks
- The client advances game and day time itself every tick (`ClientLevel.tickTime`) through the same `setGameTime` / `setDayTime` the server packet uses, so `ClientLevelMixin` marks `tickTime` to tell the packet apart. NeoForge's custom time payload calls those setters directly and never reaches `ClientPacketListener.handleSetTime`: hook the `ClientLevel` setters, not the packet handler.
- The vanilla packet sends a negative day time when the daylight rule is off; NeoForge's payload does not and updates the rule after `setDayTime`, which is why the rule is read every frame and not when the packet arrives.

#### Verified
- In-game validation not recorded.

### MC 1.21 — Fabric / NeoForge

Targets: `1.21-fabric`, `1.21-neoforge` · first mod version: 1.1.0 · date: 2026-10-18

#### Dependencies
- NeoForge `21.0.167` (the last 21.0 build); Fabric API `0.102.0+1.21` and Mod Menu `11.0.3` for the dev client.

#### API differences (vs. 1.21.1)
- None. Every hook has the same signature as on 1.21.1; the version conditions that said `1.21.1` (`DeltaTracker` frame hook, `tickRateManager()`) now say `1.21`. Checked with `javap` on the Mojang-mapped 1.21 jar: `GameRenderer.render(DeltaTracker, boolean)`, `ClientLevel.setDayTime(long)` / `tickTime()`, `DimensionType.timeOfDay(long)`, and `TickRateManager.runsNormally()` / `tickrate()`.

#### Quirks
- `build.neoforge.gradle.kts` treats `1.21` as `1.21.0` for the Minecraft upper bound (`[1.21,1.21.1)`); incrementing the last component of `1.21` literally would give `1.22`.

#### Verified
- In-game validation not recorded.

### MC 1.20.1 — Fabric / Forge

Targets: `1.20.1-fabric`, `1.20.1-forge` · first mod version: 0.1.2-alpha · date: 2026-07-27

#### Dependencies
- Java 17; Fabric Loader is pinned to `0.15.11` (min `0.14.21`) in `build.fabric.gradle.kts`.
- Forge uses ModDevGradle legacy (`net.neoforged.moddev.legacyforge`) with Mixin 0.8.5.

#### API differences (vs. 1.21.1)
- `GameRenderer.render(float, long, boolean)` is the frame hook (`GameRendererMixin`, `<1.21`).
- There is no tick rate manager (`/tick` arrived with 1.20.3): the nominal rate is 20 and time always runs normally. Time packets are handled as on 1.21.1.

#### Quirks
- Forge: the annotation processor writes `ticks.forge.refmap.json`, `ticks.forge.mixins.json` is registered through `mods.toml` and the `MixinConfigs` manifest attribute, and `pack.mcmeta` (`pack_format` 15) is required.

#### Verified
- In-game validation not recorded.
## Archived ports

None yet. When a target is dropped, move its entry here unchanged and note the last mod version that shipped it.
