# Development, CI, and Release

How to build, run, test, and release Ticks. For per-Minecraft-version API notes and porting, see [VERSION_NOTES.md](VERSION_NOTES.md). User-facing release notes live in [../CHANGELOG.md](../CHANGELOG.md).

Ticks uses one [Stonecutter](https://stonecutter.kikugie.dev/) repository: a single shared source tree builds every Minecraft version and loader. Do not create long-lived per-version branches.

## Target matrix

| Target | Loader | Java |
|---|---|---:|
| `1.20.1-fabric` | Fabric | 17 |
| `1.20.1-forge` | Forge | 17 |
| `1.21.1-fabric`, `1.21.4-fabric`, `1.21.11-fabric` | Fabric | 21 |
| `1.21.1-neoforge`, `1.21.4-neoforge`, `1.21.11-neoforge` | NeoForge | 21 |
| `26.1-fabric`, `26.1.2-fabric`, `26.2-fabric`, `26.3-fabric` | Fabric | 25 |
| `26.1-neoforge`, `26.1.2-neoforge`, `26.2-neoforge`, `26.3-neoforge` | NeoForge | 25 |

The matrix is declared once in [`settings.gradle.kts`](../settings.gradle.kts); the Java version of each target is chosen in its loader build script. The active target for the convenience tasks is stored in `.sc_active_version`. Dependency versions per target: [VERSION_NOTES.md](VERSION_NOTES.md).

### Default target

The default Stonecutter target is always the **latest Minecraft version**, Fabric build: currently `26.3-fabric`. It lives in two places that must stay equal: `vcsVersion` in [`settings.gradle.kts`](../settings.gradle.kts) and the committed `.sc_active_version`.

- Switching `.sc_active_version` to another target for local work is fine, but restore the default before committing: `.sc_active_version` must not appear in the diff.
- When a new Minecraft version is added, move both to its Fabric target in the same change.

## Requirements

- JDK 25+ to launch Gradle (Fabric Loom 1.18 refuses to load on anything older). The Java 17, 21, and 25 toolchains that compile the targets are provisioned automatically (Foojay resolver); CI installs all three explicitly.
- Windows: PowerShell, use `.\gradlew.bat`. Always pass `-Path` and `-Value` by name to `Set-Content`: in Windows PowerShell 5.1 the positional form swaps them and creates a junk file named after the target.
- macOS / Linux: use `./gradlew`. If it is not executable:

  ```bash
  chmod +x gradlew
  ```

- To force a specific launcher JDK, set `JAVA_HOME` before running Gradle. macOS:

  ```bash
  export JAVA_HOME=$(/usr/libexec/java_home -v 25)
  ```

  Windows:

  ```powershell
  $env:JAVA_HOME = "C:\path\to\jdk-25"
  ```

## Commands

Each command is in its own block so it can be copied on its own.

### Windows (PowerShell)

Select the active target (any target name from the matrix instead of `26.3-fabric`, the [default target](#default-target)):

```powershell
Set-Content -NoNewline -Path .sc_active_version -Value "26.3-fabric"
```

```powershell
.\gradlew.bat runActiveClient
```

```powershell
.\gradlew.bat buildActive
```

```powershell
.\gradlew.bat build
```

```powershell
.\gradlew.bat buildAndCollect
```

```powershell
.\gradlew.bat :1.21.1-neoforge:build
```

Test one target:

```powershell
.\gradlew.bat :1.21.1-fabric:test
```

### macOS / Linux

Select the active target (any target name from the matrix instead of `26.3-fabric`, the [default target](#default-target)):

```bash
printf '26.3-fabric' > .sc_active_version
```

```bash
./gradlew runActiveClient
```

```bash
./gradlew buildActive
```

```bash
./gradlew build
```

```bash
./gradlew buildAndCollect
```

```bash
./gradlew :1.21.1-neoforge:build
```

Test one target:

```bash
./gradlew :1.21.1-fabric:test
```

### Tasks

| Task | What it does |
|---|---|
| `runActiveClient` | Runs the client of the active target (Fabric targets also load Mod Menu, development only) |
| `buildActive` | Builds the active target |
| `build` | Builds and tests every target (what CI runs) |
| `buildAndCollect` | Builds every target and collects release JARs in `build/libs/<mod_version>/` |
| `:<target>:build` / `:<target>:test` | Builds or tests one target explicitly (useful when investigating a failure) |
| `clean buildAndCollect` | Fresh full build; use for release verification only |
| `publishMods` | Publishes every target to Modrinth and CurseForge (run by the release workflow, not by hand) |

Unqualified task names are run by Gradle in every target project, so there is no target list to maintain: adding a target in `settings.gradle.kts` includes it everywhere.

Do not use `clean` routinely: it discards Gradle and Minecraft preparation work.

Outputs: collected release JARs are in `build/libs/<mod_version>/` (for example `ticks-1.0.0-beta+1.21.1-fabric.jar`); per-target output is in `versions/<target>/build/libs/` (`-sources` JARs are not published). The version written into the mod metadata (and shown in Mod Menu and the NeoForge/Forge mod lists) is the plain `mod_version`, for example `1.0.0-beta`; only the JAR file name carries the `+<Minecraft version>-<loader>` suffix.

## Conditional sources

Version- and loader-specific code lives in the shared tree inside condition blocks. The blocks are processed by [`gradle/preprocess-sources.gradle.kts`](../gradle/preprocess-sources.gradle.kts), which every loader build script applies: it writes the result for the target to `versions/<target>/build/generated/sources/ticks/main`, and that is the only tree that gets compiled. The files under `src/` are never compiled directly, so what they look like for a given target in an editor does not matter to the build.

```java
//? >=26 {
/*return level.getOverworldClockTime();
*///?}
//? <26 {
return level.getDayTime();
//?}
```

Rules enforced by the preprocessor:

- A block opens with `//? <condition> {` on its own line and closes with `//?}` or `*///?}`.
- A condition is one of `fabric`, `forge`, `neoforge` (the target's loader), `>=X.Y.Z` (target Minecraft version at least X.Y.Z), or `<X.Y.Z` (target Minecraft version below X.Y.Z). Nested blocks are combined with AND.
- There is no `else` and no `if`: write two sibling blocks with complementary conditions.
- Inactive code is kept commented as `/* … *///?}`. The preprocessor removes the leading `/*` from every kept line that starts with `/*` (but not `/**`), inside a block or not, so a block may be stored commented-out; never start an ordinary comment line with `/*`.
- If a whole class is loader- or version-specific, keep its `package` declaration outside the block so the other targets generate an empty source file (see `forge/TicksForge.java`, `mixin/SkyRendererMixin.java`).

Keep conditions as narrow as practical. Minecraft-version differences are expressed the same way, next to the affected code ([VERSION_NOTES.md](VERSION_NOTES.md)). Mixin classes that only exist for some versions are also listed through the `${sky_renderer_mixin}` / `${clock_manager_mixin}` placeholders in `ticks.mixins.json`, which each loader build script fills in; update both places when a version-specific mixin is added.

## CI

The build workflow (`.github/workflows/build.yml`) runs on pushes to `main` and pull requests only when build-relevant files change:

- `src/**`;
- `build.*.gradle.kts`, `settings.gradle.kts`, `stonecutter.gradle.kts`, `gradle.properties`, `gradle/**`, the wrapper scripts;
- `.github/workflows/build.yml`.

Documentation-only changes (including `docs/`, `README.md`, `CHANGELOG.md`, `CLAUDE.md`) do not start it. CI runs `./gradlew build` (compile + unit tests for every target) and then verifies exactly one release JAR per target. It sets up JDK 17, 21, and 25 (25 launches Gradle) and uses the Gradle action for caching.

A change is ready to merge when:

- every target compiles, passes its tests, and produces its release JAR;
- `.sc_active_version` and `vcsVersion` name the latest target (see [Default target](#default-target));
- each JAR contains and registers its mixin config (see below);
- loader-specific classes do not leak into the other loaders' JARs;
- behavior stays compatible with the previous release.

## Packaged-JAR validation

IDE runs are not enough for loader or Mixin changes. Before releasing (and after any change to mixins, metadata, or the build scripts):

- inspect each JAR you ship: the mixin config is present and registered (`ticks.mixins.json` in `fabric.mod.json` / `neoforge.mods.toml`; `ticks.forge.mixins.json`, `ticks.forge.refmap.json`, and the `MixinConfigs` manifest entry on Forge), and no class from another loader's package is inside;
- launch at least one JAR per loader and per Minecraft *generation* (`1.20.1`, `1.21.1`/`1.21.4`, `1.21.11`, `26.x`) in a clean client profile, since the hooks differ per generation;
- verify `config/ticks.json` is created with `transitionTimeMillis` and that changing it takes effect after a restart;
- watch the sun/moon at high FPS: motion must be smooth between ticks, `/time set` and `/time add` must blend instead of snapping or spinning the wrong way, and the sky must not drift while the game is paused;
- on every version with `/tick` (1.21.1 and newer) test `/tick freeze` and `/tick rate`: the sky must stop on freeze, speed up within a second at `/tick rate 100`, and at a rate the machine cannot sustain (for example `/tick rate 10000`) keep pace with the daytime the world really reaches (compare with `/time query daytime`) instead of racing ahead; a lagging server must not make the sky run ahead and jump back;
- confirm nothing changes in the Nether and the End (fixed-time dimensions) and that world time and server behavior are unaffected.

Use a clean client profile each time, and record the outcome in the target's `Verified` line in [VERSION_NOTES.md](VERSION_NOTES.md). Shaders are not supported.

## Changelog

[`CHANGELOG.md`](../CHANGELOG.md) is user-facing and is also published as the release notes, so it must be updated together with the version.

- One section per version, newest first: `## [<mod_version>] - YYYY-MM-DD` (the version is exactly `mod_version` from `gradle.properties`, including any `-alpha` / `-beta` suffix).
- Group entries under `###` headings already in use: `Overall`, `Effects`, `Config`, `Fixes`, `Architecture`, `Compatibility`, `Documentation`. Short bullets, one change each.
- Describe changes visible to players or integrators; skip internal-only edits (typos, CI path filters).
- Do not put other `## [` headings inside a version's section: the release workflow publishes everything between its heading and the next `## [`.
- Work not yet released is collected under `## [Unreleased]` at the top and renamed to the version when releasing.

## Release

1. Set `mod_version` in `gradle.properties` and rename `## [Unreleased]` in `CHANGELOG.md` to `## [<mod_version>] - YYYY-MM-DD`:
   - stable: `1.2.3`
   - beta: `1.2.3-beta` (or `1.2.3-beta.1`)
   - alpha: `1.2.3-alpha` (or `1.2.3-alpha.1`)
2. Run a fresh full build; the sixteen JARs are collected in `build/libs/<mod_version>/`. Windows:

   ```powershell
   .\gradlew.bat clean buildAndCollect
   ```

   macOS / Linux:

   ```bash
   ./gradlew clean buildAndCollect
   ```

3. Validate the packaged JARs (see above).
4. Commit and push the release to `main` (with `.sc_active_version` on the [default target](#default-target)), then create and push the matching `v<mod_version>` tag:

   ```bash
   git tag v<mod_version>
   ```

   ```bash
   git push origin v<mod_version>
   ```

   The tag must be exactly `v<mod_version>`, including any `-alpha` or `-beta` suffix.
5. Wait for **Actions > Release** to succeed, then verify the release on GitHub, CurseForge, and Modrinth.

The tag workflow (`.github/workflows/release.yml`):

- validates that the tag matches `mod_version` and that the changelog has a section for it;
- rejects pre-release suffixes other than `-alpha` / `-beta` (they become alpha / beta releases on the platforms);
- builds and tests every target with `clean buildAndCollect` and requires exactly one release JAR per target;
- verifies the publishing variables and secrets exist, and refuses to overwrite an existing GitHub release;
- publishes every target to Modrinth and CurseForge with the changelog section as the changelog, then creates the GitHub release with the same text as notes;
- posts the changelog to Discord through a webhook and pings the role from `DISCORD_ROLE_ID` (best effort: skipped when `DISCORD_WEBHOOK_URL` is unset, and a failure does not fail the run).

Platform display values omit the pre-release suffix: CurseForge file names and Modrinth version numbers use `<numeric mod version>+<Minecraft version>` (for example `1.0.0+1.21.1`), and Modrinth version titles use `Ticks <numeric mod version> <Loader>` (for example `Ticks 1.0.0 NeoForge`); the pre-release status is carried by the release type.

It needs the repository variables `MODRINTH_PROJECT_ID` and `CURSEFORGE_PROJECT_ID` and the secrets `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN`; the optional secret `DISCORD_WEBHOOK_URL` and optional variable `DISCORD_ROLE_ID` enable the Discord post. Never put tokens or webhook URLs in repository files.

### If publishing fails

- Nothing uploaded: fix the problem and rerun the failed workflow.
- Anything uploaded: increment `mod_version`, add its changelog section, and publish a new tag; do not reuse or move the old tag. A published version cannot be re-released.

## Troubleshooting

- **"Dependency requires at least JVM runtime version 25"** → Gradle was launched with an older JDK; launch it with JDK 25+ (`JAVA_HOME`, or `setup-java` in CI).
- **"No matching toolchain found"** → the Foojay resolver could not download the JDK (offline or blocked); install JDK 17, 21, and 25 locally.
- **Transparent world in dev** → Gradle is using the wrong JDK; use the Java toolchain of the target.
- **"Cannot find symbol" in a generated target** → look at the processed file in `versions/<target>/build/generated/sources/ticks/main` to see which conditional blocks were kept; remember that conditions have no `else`.
- **Editor shows errors on commented-out code** → expected; the editor sees the unprocessed tree. Build the target to check.
- **Wrong client runs** → update `.sc_active_version` and use `runActiveClient`.
- **Mod loads but nothing happens in the packaged JAR** → the mixin config is not registered for that loader; inspect the JAR metadata.
