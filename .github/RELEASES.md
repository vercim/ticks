# Deploying a new version

Pushing a release tag starts the `Release` GitHub Actions workflow. It builds all six supported targets, uploads the JARs to GitHub Releases, CurseForge, and Modrinth, and generates GitHub release notes from the commits since the previous release.

## One-time repository setup

Create the CurseForge and Modrinth projects before publishing. Then add these repository settings in **GitHub > Settings > Secrets and variables > Actions**:

| Kind | Name | Value |
| --- | --- | --- |
| Variable | `CURSEFORGE_PROJECT_ID` | Numeric ID of the CurseForge project. It is shown in the project URL or on its Overview page. |
| Secret | `CURSEFORGE_TOKEN` | CurseForge API upload token. |
| Variable | `MODRINTH_PROJECT_ID` | Project slug or the 8-character project ID from the Modrinth project page. For example, `my-mod` from `modrinth.com/mod/my-mod`. |
| Secret | `MODRINTH_TOKEN` | Modrinth token with permission to upload versions to that project. |

Do not put either token in `gradle.properties`, a commit, or a tag message. `MODRINTH_PROJECT_ID` must be the actual Modrinth project identifier; the mod ID `ticks` is not automatically a Modrinth project ID.

## Choose the version and channel

`mod_version` in `gradle.properties` is the source of truth. The Git tag must exactly equal `v<mod_version>`.

| `mod_version` | Tag | Channel |
| --- | --- | --- |
| `0.1.2` | `v0.1.2` | Release |
| `0.1.2-beta.1` | `v0.1.2-beta.1` | Beta / GitHub prerelease |
| `0.1.2-alpha.1` | `v0.1.2-alpha.1` | Alpha / GitHub prerelease |

The same channel is applied to CurseForge and Modrinth. JAR names include `mod_version`, so a beta such as `0.1.2-beta.1` cannot be mistaken for the final `0.1.2` release.

CurseForge displays each uploaded file as the numeric part of `<mod_version>+<Minecraft version>` — for example, both `0.1.2` and `0.1.2-alpha.1` are displayed as `0.1.2+1.21.1`. Modrinth uses the same value as its Version number and the version subtitle `Ticks <numeric mod version> <loader>`, for example `Ticks 0.1.2 NeoForge`. The Alpha/Beta/Release type is shown by both platforms separately.

## Publish the release

1. Update `mod_version` in `gradle.properties` to the exact version being released.
2. Run the tests for every target:

   ```powershell
   .\gradlew.bat :1.20.1-fabric:test :1.20.1-forge:test :1.21.1-fabric:test :1.21.1-neoforge:test :1.21.4-fabric:test :1.21.4-neoforge:test
   ```

3. Commit and push the version change and all intended release changes:

   ```powershell
   git add gradle.properties
   git commit -m "Release 0.1.2"
   git push origin main
   ```

4. Create an annotated tag that exactly matches `mod_version`, then push it:

   ```powershell
   git tag -a v0.1.2 -m "Release 0.1.2"
   git push origin v0.1.2
   ```

5. Open **GitHub > Actions > Release** and wait for the workflow to finish. It must complete successfully before the version is considered published.
6. Check the generated GitHub Release and the new files on CurseForge and Modrinth. Confirm the JAR filenames, Minecraft versions, loaders, and release channel.

For a beta, use the same sequence with the beta value everywhere, for example `mod_version=0.1.3-beta.1` and tag `v0.1.3-beta.1`.

## Recovery

- If the workflow fails before uploading to any platform, fix the issue and re-run the failed GitHub Actions job.
- If an external platform already accepted one or more files, do not overwrite that version. Make a new version (for example, `0.1.3-beta.2`) and publish a new matching tag.
- The workflow rejects a tag that does not match `mod_version`; update the version commit and create the correct new tag instead of moving an existing release tag.
