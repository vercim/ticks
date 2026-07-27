# Automated releases

Pushing a tag named `v<mod_version>` builds all four Stonecutter targets, creates (or updates) the matching GitHub Release, and publishes each loader-specific JAR to CurseForge and Modrinth. The release channel is derived from `mod_version`:

| `mod_version` example | Channel |
| --- | --- |
| `0.1.1` | Release |
| `0.1.1-beta.1` | Beta |
| `0.1.1-alpha.1` | Alpha |

Beta and alpha releases are marked as prereleases in GitHub and use the matching channel in CurseForge and Modrinth.
Their JAR names also include the same `mod_version`, which prevents an alpha or beta artifact from being confused with the eventual stable release.

Before the first release, create these repository settings in GitHub:

| Kind | Name | Value |
| --- | --- | --- |
| Variable | `CURSEFORGE_PROJECT_ID` | Numeric project ID from the CurseForge project page. |
| Secret | `CURSEFORGE_TOKEN` | CurseForge API upload token. |
| Secret | `MODRINTH_TOKEN` | Modrinth token with permission to upload versions to the `ticks` project. |

The Modrinth project is configured as `ticks`, matching the project URL in the README. The workflow deliberately checks all settings before it creates a GitHub Release, so a missing credential cannot leave a partial release behind.

To publish version `0.1.1`:

```sh
git tag v0.1.1
git push origin v0.1.1
```

For a beta, set `mod_version=0.1.2-beta.1` and push the matching `v0.1.2-beta.1` tag. The same convention applies to alpha releases.

Re-running the workflow for the tag updates the release assets; publishing endpoints may reject duplicate files, so use a new version tag for an actual new release.
