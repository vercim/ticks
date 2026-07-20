# Smooth Time

Client-side Fabric and NeoForge mod for Minecraft 1.21.1 that evaluates the sky's celestial
time every render frame instead of only at whole game ticks. It does not change
the server clock or any game logic.

Large time corrections such as `/time set`, sleeping, and server time updates
keep the current visual sky position, then take the shortest route to the new
time. The remaining difference decays with a 0.20-second time constant.

## Development

```powershell
.\gradlew.bat :1.21.1-fabric:build
.\gradlew.bat :1.21.1-neoforge:build
.\gradlew.bat runActiveClient
```

The built jars are written to `versions/1.21.1-fabric/build/libs/` and
`versions/1.21.1-neoforge/build/libs/`. `runActiveClient` follows the target in
`.sc_active_version`; change it to `1.21.1-neoforge` to run NeoForge.

## Compatibility

Smooth Time is client-only and requires Fabric Loader or NeoForge plus Minecraft 1.21.1.
It affects vanilla render paths that use `DimensionType#timeOfDay`. Shader
packs that calculate their own sky time may not use the smoothed value.
