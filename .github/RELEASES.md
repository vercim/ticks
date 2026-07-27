# Release checklist

## One-time GitHub setup

Add these under **Settings > Secrets and variables > Actions**:

| Type | Name |
| --- | --- |
| Variable | `CURSEFORGE_PROJECT_ID` |
| Variable | `MODRINTH_PROJECT_ID` |
| Secret | `CURSEFORGE_TOKEN` |
| Secret | `MODRINTH_TOKEN` |

Never commit tokens.

## Publish

1. Set `mod_version` in `gradle.properties`:
   - stable: `0.1.2`
   - beta: `0.1.2-beta.1`
   - alpha: `0.1.2-alpha.1`
2. Build and test every target:

   ```powershell
   .\gradlew.bat clean build
   ```

3. Commit and push the release:

   ```powershell
   git add gradle.properties
   git commit -m "Release 0.1.2"
   git push origin main
   ```

4. Create and push the matching tag:

   ```powershell
   git tag -a v0.1.2 -m "Release 0.1.2"
   git push origin v0.1.2
   ```

   The tag must be exactly `v<mod_version>`, including any `-alpha` or `-beta` suffix.

5. Wait for **Actions > Release** to succeed.
6. Verify the release on GitHub, CurseForge, and Modrinth.

## If publishing fails

- Nothing uploaded: fix the problem and rerun the failed workflow.
- Anything uploaded: increment `mod_version` and publish a new tag; do not reuse or move the old tag.
