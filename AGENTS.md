# Repository Guidelines

## Project Structure & Module Organization

Ticks is a client-only, multi-version Minecraft mod sharing one codebase across Fabric, Forge, and NeoForge. Core logic and mixins live under `src/main/java/dev/vercim/ticks/`; loader-neutral tests are in the matching `src/test/java/` package. Shared assets and Mixin configuration belong in `src/main/resources/`, while loader metadata stays in `src/fabric/resources/`, `src/forge/resources/`, or `src/neoforge/resources/`.

Stonecutter generates loader-specific projects under `versions/`. Treat that directory, `build/`, and `run/` as generated output; edit the root sources and Gradle scripts instead. Dependency versions and mod metadata are centralized in `gradle.properties` and `gradle/libs.versions.toml`.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper. On Windows:

- `.\gradlew.bat clean build` — clean, test, and package every Stonecutter target.
- `.\gradlew.bat :1.21.1-fabric:test` — run only one target's JUnit suite when narrowing a failure.
- `.\gradlew.bat runActiveClient` — launch the target selected by `.sc_active_version`.
- `.\gradlew.bat :1.21.1-fabric:runClient` — launch a specific development client.

Unqualified task names such as `clean`, `build`, and `publishMods` are selected across every Gradle subproject, so do not maintain explicit target lists. Use `./gradlew` in Unix-like shells. Built artifacts appear in `versions/<target>/build/libs/`.

## Coding Style & Naming Conventions

Follow the existing Java style: tabs for indentation, braces on the same line, grouped imports, and `final` utility classes with private constructors. Use `UpperCamelCase` for types, `lowerCamelCase` for methods and variables, and `UPPER_SNAKE_CASE` for constants. Keep platform-independent behavior in shared classes; isolate loader-specific bootstrapping and metadata. Add concise Javadocs where timing or cyclic math is not self-evident. No formatter or linter is configured, so match neighboring code.

## Testing Guidelines

Tests use JUnit Jupiter 5. Name files `*Test.java` and methods after observable behavior, such as `midnightUsesTheShortForwardRoute`. Add deterministic unit tests for interpolation, wrapping, and discontinuities. There is no enforced coverage threshold; protect edge cases and run tests for both loaders before submitting.

## Commit & Pull Request Guidelines

History is currently minimal; follow its imperative, title-case style (for example, `Add Ticks for Fabric and NeoForge`). Keep commits focused. Pull requests should explain behavior changes, list tested loader commands, link relevant issues, and include screenshots or short captures for visible rendering changes. Call out compatibility implications for Mixins, shader packs, or Minecraft versions.

## Release Publishing

Pushing a tag matching `v*` starts [`.github/workflows/release.yml`](.github/workflows/release.yml). `mod_version` in `gradle.properties` is the release source of truth: the tag must be exactly `v<mod_version>`. Use `-alpha` or `-beta` in `mod_version` for prereleases; the workflow assigns the same Alpha or Beta channel to GitHub Releases, CurseForge, and Modrinth. A version without either suffix is a stable release.

The workflow runs the root `clean build` and `publishMods` task selectors. Gradle applies them to all supported Stonecutter subprojects, so adding a target in `settings.gradle.kts` automatically includes it in CI builds and publishing.

Repository configuration is required before publishing:

- GitHub Action variables: `CURSEFORGE_PROJECT_ID` and `MODRINTH_PROJECT_ID` (the actual Modrinth project ID or slug).
- GitHub Action secrets: `CURSEFORGE_TOKEN` and `MODRINTH_TOKEN`.

Never add upload tokens to the repository, `gradle.properties`, tags, or logs. The platform display values intentionally omit prerelease suffixes: CurseForge file names and Modrinth Version numbers use `<numeric mod version>+<Minecraft version>` (for example, `0.1.2+1.21.1`), while Modrinth Version subtitles use `Ticks <numeric mod version> <loader>` (for example, `Ticks 0.1.2 NeoForge`).

Before creating a release tag, run `.\gradlew.bat clean build`. If a release has uploaded to any platform and then fails, publish a new version and matching tag rather than replacing the existing release.
