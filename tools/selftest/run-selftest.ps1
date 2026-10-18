<#
.SYNOPSIS
  Runs the scripted time-jump test (DevSelfTest) on the dev client of each target and prints one verdict per target.

.DESCRIPTION
  For every target it: copies the harness from tools/selftest into src/ and hooks it into TicksController, prepares the
  target's dev profile (a copy of a test world, options.txt without the onboarding screen and the pause on lost focus),
  starts `runClient` with the test world as quick-play world, waits for the harness to finish (it ends the game itself),
  reads run/ticks-selftest.txt and run/logs/latest.log, and puts everything back. The harness never stays in src/ and
  never reaches a JAR.

  What it checks: the visual sky time advances every frame (not once per tick); the DimensionType hook answers with it
  (versions before 1.21.11); every /time set, /time add and the jump across midnight is blended along the shortest
  route, not snapped, and settles within 2.5 s; the log has no mixin or other errors.

  Close every running Minecraft dev client and Gradle run first: a running one holds the Gradle lock. The window of
  the game opens for about a minute per target; do not click into it.

.PARAMETER Targets
  Targets to test, comma-separated, for example 1.21.4-neoforge,1.21.9-fabric. Default: every folder in versions/.

.PARAMETER World
  A creative world with cheats to copy into the profile. Default: the first world found in versions/*/run/saves (the
  oldest target's, so that every version can load it; a newer world cannot be opened by an older game).

.PARAMETER TimeoutMinutes
  Per-target limit (a first start of a fresh profile and the world upgrade are slow); a client still alive after it is
  killed and the target is reported as TIMEOUT.

.PARAMETER KeepFiles
  Keep ticks-selftest.csv / .txt and the world copy in the profile after the run.

.PARAMETER Install
  Only copy the harness into src/ (for a run from the IDE) and exit.

.PARAMETER Uninstall
  Only remove the harness from src/ (also after a crashed run) and exit.
#>
param(
    [string[]]$Targets,
    [string]$World,
    [int]$TimeoutMinutes = 8,
    [switch]$KeepFiles,
    [switch]$Install,
    [switch]$Uninstall
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $root

$harnessSource = Join-Path $PSScriptRoot 'DevSelfTest.java'
$javaDir = Join-Path $root 'src\main\java\dev\vercim\ticks'
$harnessFile = Join-Path $javaDir 'DevSelfTest.java'
$controller = Join-Path $javaDir 'TicksController.java'
$initScript = Join-Path $PSScriptRoot 'selftest-init.gradle'
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Remove-Harness {
    Remove-Item $harnessFile -Force -ErrorAction SilentlyContinue
    $text = [System.IO.File]::ReadAllText($controller)
    $clean = [regex]::Replace($text, '(?m)^[ \t]*DevSelfTest\.onFrame\(level, partialTick\);\r?\n', '')
    if ($clean -ne $text) { [System.IO.File]::WriteAllText($controller, $clean, $utf8) }
}

function Install-Harness {
    Remove-Harness
    Copy-Item $harnessSource $harnessFile -Force
    $text = [System.IO.File]::ReadAllText($controller)
    $pattern = '(?m)^([ \t]*renderedSkyAngle = SkyTimeMath\.skyAngle\(INTERPOLATOR\.getRenderedTime\(\)\);)(\r?\n)'
    if (-not [regex]::IsMatch($text, $pattern)) { throw 'TicksController no longer has the line the harness hooks after.' }
    $patched = [regex]::Replace($text, $pattern, { param($m) $m.Groups[1].Value + $m.Groups[2].Value + "`t`tDevSelfTest.onFrame(level, partialTick);" + $m.Groups[2].Value })
    [System.IO.File]::WriteAllText($controller, $patched, $utf8)
}

if ($Uninstall) { Remove-Harness; Write-Host 'Harness removed from src/.'; return }
if ($Install) { Install-Harness; Write-Host 'Harness copied into src/ (remove it with -Uninstall).'; return }

if (-not $Targets) {
    $Targets = @(Get-ChildItem (Join-Path $root 'versions') -Directory | Where-Object { $_.Name -ne 'build' } | Select-Object -ExpandProperty Name)
}
$Targets = @($Targets | ForEach-Object { $_ -split ',' } | Where-Object { $_ })

if (-not $World) {
    $found = Get-ChildItem (Join-Path $root 'versions') -Directory | ForEach-Object {
        $saves = Join-Path $_.FullName 'run\saves'
        if (Test-Path $saves) { Get-ChildItem $saves -Directory | Where-Object { $_.Name -ne 'ticks-selftest' -and (Test-Path (Join-Path $_.FullName 'level.dat')) } }
    } | Select-Object -First 1
    if (-not $found) { throw 'No test world found. Create a creative world with cheats in a dev client, or pass -World <folder>.' }
    $World = $found.FullName
}
Write-Host "Test world: $World"

# Lines that appear in every healthy dev run (no network, normal shutdown) and are not a finding.
$benign = 'authlib|Realms|sessionserver|HttpURL|HttpsURL|UnknownHost|user properties|Ambiguity|ClosedChannel|Failed to fetch|profile properties|MinecraftClientException|SignedJWT|InvalidCredentials|Status: 401|^\s+at |^\s*\.\.\. \d+ more|^Caused by|lost connection|telemetry|random_sequences'
$problem = '(?i)(/ERROR\]|\[ERROR\]|Exception|InvalidInjection|Mixin apply|Critical injection|MixinApplyError)'

function Stop-Game($target) {
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
        Where-Object { $_.CommandLine -match 'quickPlaySingleplayer' -and $_.CommandLine -match [regex]::Escape("versions\$target") } |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
}

$results = @()
Install-Harness
try {
    foreach ($target in $Targets) {
        Write-Host "`n=== $target ==="
        $run = Join-Path $root "versions\$target\run"
        $options = Join-Path $run 'options.txt'
        $savedOptions = if (Test-Path $options) { [System.IO.File]::ReadAllText($options) } else { $null }
        $worldCopy = Join-Path $run 'saves\ticks-selftest'
        $verdictFile = Join-Path $run 'ticks-selftest.txt'
        $csvFile = Join-Path $run 'ticks-selftest.csv'
        $gradleLog = Join-Path $env:TEMP "ticks-selftest-$target.log"
        $reasons = @()
        try {
            New-Item -ItemType Directory -Force (Join-Path $run 'saves') | Out-Null
            Remove-Item $worldCopy, $verdictFile, $csvFile -Recurse -Force -ErrorAction SilentlyContinue
            Copy-Item $World $worldCopy -Recurse
            Remove-Item (Join-Path $worldCopy 'session.lock') -Force -ErrorAction SilentlyContinue
            # A fresh profile opens the accessibility onboarding screen instead of the quick-play world; a game that
            # loses focus pauses itself and the integrated server with it.
            $lines = @()
            if ($savedOptions) { $lines = @($savedOptions -split "\r?\n" | Where-Object { $_ -and $_ -notmatch '^(onboardAccessibility|pauseOnLostFocus):' }) }
            $lines += 'onboardAccessibility:false'
            $lines += 'pauseOnLostFocus:false'
            [System.IO.File]::WriteAllText($options, (($lines -join "`n") + "`n"), $utf8)

            $started = Get-Date
            $process = Start-Process -FilePath (Join-Path $root 'gradlew.bat') -PassThru -NoNewWindow `
                -ArgumentList @('-I', "`"$initScript`"", ":${target}:runClient", '--console=plain') `
                -RedirectStandardOutput $gradleLog -RedirectStandardError "$gradleLog.err"
            $null = $process.Handle   # without it ExitCode stays empty after a timed wait
            if (-not $process.WaitForExit($TimeoutMinutes * 60 * 1000)) {
                Stop-Game $target
                & taskkill /PID $process.Id /T /F | Out-Null
                $reasons += "TIMEOUT after $TimeoutMinutes min (is the game stuck on a screen? last log line: $((Get-Content (Join-Path $run 'logs\latest.log') -Tail 1 -ErrorAction SilentlyContinue)))"
            } else {
                $process.WaitForExit()
                if ($process.ExitCode -ne 0) {
                    $reasons += "Gradle exited with $($process.ExitCode): " + ((Get-Content $gradleLog, "$gradleLog.err" -Tail 15 -ErrorAction SilentlyContinue) -join ' | ')
                }
            }
            Stop-Game $target

            if (Test-Path $verdictFile) {
                $verdict = Get-Content $verdictFile
                $verdict | ForEach-Object { Write-Host "  $_" }
                if ($verdict -notcontains 'RESULT PASS') { $reasons += 'checks failed' }
            } else {
                $reasons += 'no ticks-selftest.txt (the harness never ran)'
            }

            $log = Join-Path $run 'logs\latest.log'
            if (Test-Path $log) {
                $findings = @(Get-Content $log | Where-Object { $_ -match $problem -and $_ -notmatch $benign })
                if ($findings.Count -gt 0) {
                    $reasons += "$($findings.Count) suspicious log line(s)"
                    $findings | Select-Object -First 8 | ForEach-Object { Write-Host "  LOG: $_" }
                } else {
                    Write-Host '  PASS log: no mixin or other errors'
                }
            }
        } finally {
            Stop-Game $target
            if (-not $KeepFiles) { Remove-Item $worldCopy, $verdictFile, $csvFile -Recurse -Force -ErrorAction SilentlyContinue }
            if ($null -ne $savedOptions) { [System.IO.File]::WriteAllText($options, $savedOptions, $utf8) } elseif (Test-Path $options) { Remove-Item $options -Force }
        }
        $status = if ($reasons.Count -eq 0) { 'PASS' } else { 'FAIL' }
        $results += [pscustomobject]@{ Target = $target; Result = $status; Notes = ($reasons -join '; ') }
        Write-Host "  => $status $($reasons -join '; ')"
    }
} finally {
    Remove-Harness
}

Write-Host "`n=== Summary ==="
$results | Format-Table -AutoSize -Wrap
if (@($results | Where-Object { $_.Result -ne 'PASS' }).Count -gt 0) { exit 1 }
