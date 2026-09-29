<#
Runs one lab end to end and prints the before/after table.

  labs\lab.ps1 <lab> <baseline|verify> [--runs N] [--gc serial|g1|zgc] [--seconds S]

  <lab>      lab number (1), slug (retention) or directory name (1-retention) under labs\
  --runs     Gatling runs against one application start (default 3)
  --gc       replace the lab's collector: serial, g1 or zgc
  --seconds  duration of each Gatling run (default 60)

Environment: LAB_EXTRA_JVM_OPTS is appended to the lab's JVM options.
Exit codes: 0 ok, 1 build/load-test/report failure, 2 port busy, 3 app not healthy, 64 usage.
Works on Windows PowerShell 5.1 and PowerShell 7 (Windows, macOS, Linux); same behaviour as labs/lab.sh.
If scripts are blocked: powershell -ExecutionPolicy Bypass -File labs\lab.ps1 1 baseline
#>

# Plain $args (no param block), so the bash-style --runs / --seconds / --gc options pass through untouched.
$ErrorActionPreference = 'Continue'
Set-StrictMode -Version 2.0

$Port = 8080
$HealthTimeout = 90
$ReportMain = 'net.safedata.performance.training.lab.harness.LabReportMain'
$Launcher = 'org.springframework.boot.loader.launch.PropertiesLauncher'
$Dash = [char]0x2014
$OnWindows = ([System.IO.Path]::DirectorySeparatorChar -eq '\')

function Show-Usage {
    [Console]::Error.WriteLine('Usage: labs\lab.ps1 <lab> <baseline|verify> [--runs N] [--gc serial|g1|zgc] [--seconds S]')
    [Console]::Error.WriteLine('  <lab> is a number (1), a slug (retention) or a directory name (1-retention) under labs\')
    exit 64
}

function Stop-Usage([string] $Message) {
    [Console]::Error.WriteLine($Message)
    Show-Usage
}

function Write-Err([string] $Message) {
    [Console]::Error.WriteLine($Message)
}

function Test-PositiveInt([string] $Value) {
    return ($Value -match '^[0-9]+$') -and ([long] $Value -gt 0)
}

# Quotes one argument for a Windows-style command line (also how .NET splits Arguments on macOS/Linux).
function ConvertTo-CommandLineArg([string] $Arg) {
    if ($Arg.Length -gt 0 -and $Arg -notmatch '[\s"]') {
        return $Arg
    }
    $escaped = [regex]::Replace($Arg, '(\\*)"', '$1$1\"')
    $escaped = [regex]::Replace($escaped, '(\\+)$', '$1$1')
    return '"' + $escaped + '"'
}

# --- arguments -------------------------------------------------------------------------------
$argv = @($args)
if ($argv.Count -ge 1 -and ($argv[0] -eq '-h' -or $argv[0] -eq '--help')) { Show-Usage }
if ($argv.Count -lt 2) { Show-Usage }

$LabArg = [string] $argv[0]
$Phase = [string] $argv[1]
$Runs = '3'
$Duration = '60'
$Gc = ''

if ($Phase -cne 'baseline' -and $Phase -cne 'verify') {
    Stop-Usage "Phase must be baseline or verify, got: $Phase"
}

$i = 2
while ($i -lt $argv.Count) {
    $opt = [string] $argv[$i]
    if ($opt -eq '-h' -or $opt -eq '--help') { Show-Usage }
    if (@('--runs', '--seconds', '--gc') -notcontains $opt) { Stop-Usage "Unknown option: $opt" }
    if ($i + 1 -ge $argv.Count) { Stop-Usage "$opt needs a value" }
    $value = [string] $argv[$i + 1]
    switch ($opt) {
        '--runs' { $Runs = $value }
        '--seconds' { $Duration = $value }
        '--gc' { $Gc = $value }
    }
    $i += 2
}

if (-not (Test-PositiveInt $Runs)) { Stop-Usage "--runs must be a positive whole number, got: $Runs" }
if (-not (Test-PositiveInt $Duration)) { Stop-Usage "--seconds must be a positive whole number, got: $Duration" }
if (@('', 'serial', 'g1', 'zgc') -cnotcontains $Gc) { Stop-Usage "--gc must be serial, g1 or zgc, got: $Gc" }
$Runs = [int] $Runs
$Duration = [int] $Duration

# --- 1. resolve the lab directory ------------------------------------------------------------
$Repo = Split-Path -Parent $PSScriptRoot
$Labs = Join-Path $Repo 'labs'

$candidates = @()
if ($LabArg -match '^[0-9]+$') {
    $number = [string] ([int] $LabArg)
    $candidates = @(Get-ChildItem -LiteralPath $Labs -Directory | Where-Object { $_.Name -like "$number-*" })
} elseif ($LabArg -match '^[0-9]+-[A-Za-z0-9._-]+$') {
    $candidates = @(Get-ChildItem -LiteralPath $Labs -Directory | Where-Object { $_.Name -eq $LabArg })
} elseif ($LabArg -match '^[A-Za-z0-9._-]+$') {
    $candidates = @(Get-ChildItem -LiteralPath $Labs -Directory | Where-Object { $_.Name -match ('^[0-9]+-' + [regex]::Escape($LabArg) + '$') })
}
$candidates = @($candidates | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'lab.properties') -PathType Leaf })
if ($candidates.Count -gt 1) { Stop-Usage "Lab '$LabArg' is ambiguous $Dash use the directory name" }
if ($candidates.Count -eq 0) { Stop-Usage "No lab '$LabArg' under $Labs (expected labs\<n>-<slug>\lab.properties)" }
$LabDir = $candidates[0].FullName
$PropsFile = Join-Path $LabDir 'lab.properties'

# lab.properties: first '=' separates key and value; blank lines and #/! comments skipped.
$Props = [ordered] @{}
foreach ($line in [System.IO.File]::ReadAllLines($PropsFile)) {
    $trimmed = $line.Trim()
    if ($trimmed.Length -eq 0 -or $trimmed.StartsWith('#') -or $trimmed.StartsWith('!')) { continue }
    $eq = $line.IndexOf('=')
    if ($eq -lt 0) { continue }
    $key = $line.Substring(0, $eq).Trim()
    if (-not $Props.Contains($key)) { $Props[$key] = $line.Substring($eq + 1).Trim() }
}
function Get-Prop([string] $Key) {
    if ($Props.Contains($Key)) { return [string] $Props[$Key] }
    return ''
}

$LabId = Get-Prop 'lab'
if (-not $LabId) { $LabId = Split-Path -Leaf $LabDir }
$SpringProfile = Get-Prop 'profile'
$JvmOptions = Get-Prop 'jvm.options'
$Simulation = Get-Prop 'simulation'
$StatsPath = Get-Prop 'stats.path'
if (-not $SpringProfile) { Write-Err "$PropsFile has no profile= entry"; exit 64 }
if (-not $Simulation) { Write-Err "$PropsFile has no simulation= entry"; exit 64 }

# --- 2. the port must be free ----------------------------------------------------------------
function Test-PortBusy([int] $PortNumber) {
    foreach ($address in @([System.Net.IPAddress]::Loopback, [System.Net.IPAddress]::IPv6Loopback)) {
        $client = $null
        try {
            $client = New-Object System.Net.Sockets.TcpClient($address.AddressFamily)
            $pending = $client.BeginConnect($address, $PortNumber, $null, $null)
            if ($pending.AsyncWaitHandle.WaitOne(1000) -and $client.Connected) {
                return $true
            }
        } catch {
            # refused or IPv6 unavailable: nothing listening on this address
        } finally {
            if ($client) { $client.Close() }
        }
    }
    return $false
}
if (Test-PortBusy $Port) {
    Write-Err "Port $Port is already in use $Dash stop the other process first"
    exit 2
}

# --- 3. build when the jar is missing or older than the sources ------------------------------
$JavaExe = if ($OnWindows) { 'java.exe' } else { 'java' }
$Java = $null
if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path (Join-Path $env:JAVA_HOME 'bin') $JavaExe) -PathType Leaf)) {
    $Java = Join-Path (Join-Path $env:JAVA_HOME 'bin') $JavaExe
} else {
    $cmd = Get-Command java -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($cmd) { $Java = $cmd.Source }
}
if (-not $Java) { Write-Err 'No java found: set JAVA_HOME or put java on the PATH'; exit 1 }

$Mvnw = if ($OnWindows) { Join-Path $Repo 'mvnw.cmd' } else { Join-Path $Repo 'mvnw' }
$Target = Join-Path $Repo 'target'

function Find-Jar {
    if (-not (Test-Path -LiteralPath $Target -PathType Container)) { return $null }
    $jar = Get-ChildItem -LiteralPath $Target -Filter '*.jar' -File | Select-Object -First 1
    if ($jar) { return $jar.FullName }
    return $null
}

$Jar = Find-Jar
$SimClassFile = Join-Path (Join-Path $Target 'test-classes') (($Simulation -replace '\.', '/') + '.class')
$needsBuild = (-not $Jar) -or (-not (Test-Path -LiteralPath $SimClassFile -PathType Leaf))
if (-not $needsBuild) {
    $jarTime = (Get-Item -LiteralPath $Jar).LastWriteTimeUtc
    $newer = Get-ChildItem -LiteralPath (Join-Path $Repo 'src') -Recurse -File |
            Where-Object { $_.LastWriteTimeUtc -gt $jarTime } | Select-Object -First 1
    if ($newer -or (Get-Item -LiteralPath (Join-Path $Repo 'pom.xml')).LastWriteTimeUtc -gt $jarTime) { $needsBuild = $true }
}
if ($needsBuild) {
    Write-Host 'Building the application (mvnw -q -DskipTests package)...'
    Push-Location -LiteralPath $Repo
    try { & $Mvnw -q '-DskipTests' package; $buildExit = $LASTEXITCODE } finally { Pop-Location }
    if ($buildExit -ne 0) { Write-Err 'Build failed'; exit 1 }
    $Jar = Find-Jar
    if (-not $Jar) { Write-Err 'Build produced no jar under target\'; exit 1 }
}

# --- 4. JVM options, with the optional collector override ------------------------------------
# 'java -version' writes to stderr: read it through a Process so PowerShell 5.1 does not treat it as an error.
function Get-JavaMajor {
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $Java
    $psi.Arguments = '-XshowSettings:properties -version'
    $psi.UseShellExecute = $false
    $psi.RedirectStandardError = $true
    $psi.RedirectStandardOutput = $true
    $psi.CreateNoWindow = $true
    $process = [System.Diagnostics.Process]::Start($psi)
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    $text = $stderr + "`n" + $stdout.Result
    $match = [regex]::Match($text, '(?m)^\s*java\.specification\.version = (\S+)')
    if (-not $match.Success) { return -1 }
    $version = $match.Groups[1].Value -replace '^1\.', ''
    return [int] $version
}
$JavaMajor = Get-JavaMajor
if ($JavaMajor -lt 0) { Write-Err "Cannot read the Java version of $Java"; exit 1 }

$JvmOpts = @($JvmOptions -split '\s+' | Where-Object { $_ })
if ($Gc) {
    $JvmOpts = @($JvmOpts | Where-Object { $_ -notmatch '^-XX:[+-]Use[A-Za-z0-9]*GC$' -and $_ -notmatch '^-XX:[+-]ZGenerational$' })
    switch ($Gc) {
        'serial' { $JvmOpts += '-XX:+UseSerialGC' }
        'g1' { $JvmOpts += '-XX:+UseG1GC' }
        'zgc' {
            $JvmOpts += '-XX:+UseZGC'
            # generational ZGC: opt-in on 21-22 (JEP 439), the default from 23 (JEP 474)
            if ($JavaMajor -eq 21 -or $JavaMajor -eq 22) {
                $JvmOpts += '-XX:+ZGenerational'
            } elseif ($JavaMajor -lt 21) {
                Write-Host "ZGC on $JavaMajor is non-generational"
            }
        }
    }
}
if ($env:LAB_EXTRA_JVM_OPTS) {
    $JvmOpts += @($env:LAB_EXTRA_JVM_OPTS -split '\s+' | Where-Object { $_ })
}

# --- 5. the run directory: results/<safe-branch>/<lab>/<yyyyMMdd-HHmmss>-<phase> --------------
$Branch = ''
try {
    $Branch = [string] (& git -C $Repo rev-parse --abbrev-ref HEAD 2> $null)
    if ($LASTEXITCODE -ne 0) { $Branch = '' }
} catch {
    $Branch = ''
}
$Branch = $Branch.Trim()
if (-not $Branch) { $Branch = 'local' }
$SafeBranch = $Branch -replace '[^A-Za-z0-9._-]', '_'
# Relative to the repository: Maven resolves it against the project folder, so mvnw.cmd never
# receives an absolute path (a space in it would break the -D argument on Windows).
$RunDirRel = Join-Path (Join-Path (Join-Path 'results' $SafeBranch) $LabId) ((Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + $Phase)
$RunDir = Join-Path $Repo $RunDirRel
New-Item -ItemType Directory -Force -Path $RunDir | Out-Null
$AppLog = Join-Path $RunDir 'app.log'
# Windows PowerShell 5.1 cannot send stdout and stderr to one file, so the JVM's stderr gets its own.
$AppErrLog = Join-Path $RunDir 'app-stderr.log'

function Show-AppLogTail {
    foreach ($file in @($AppLog, $AppErrLog)) {
        if ((Test-Path -LiteralPath $file -PathType Leaf) -and (Get-Item -LiteralPath $file).Length -gt 0) {
            Write-Err "--- last lines of $file"
            Get-Content -LiteralPath $file -Tail 40 | ForEach-Object { Write-Err $_ }
        }
    }
}

# --- 6. start the application; the finally block stops it on any exit ------------------------
$App = $null
function Stop-App {
    if ($null -eq $script:App) { return }
    try {
        if (-not $script:App.HasExited) {
            Stop-Process -Id $script:App.Id -Force -ErrorAction SilentlyContinue
            [void] $script:App.WaitForExit(15000)
        }
    } catch {
        # already gone
    }
    $script:App = $null
}

$exitCode = 0
try {
    Write-Host "Lab $LabId ($Phase) on branch $Branch, Java $JavaMajor, JVM options: $($JvmOpts -join ' ')"
    Write-Host "Results: $RunDir"

    # Started inside the run directory, so gc.log is a relative path (no drive-letter colon for -Xlog).
    $appArgs = @($JvmOpts) + @('-Xlog:gc*:file=gc.log:uptime,level,tags', '-jar', $Jar,
            "--spring.profiles.active=$SpringProfile", "--server.port=$Port")
    $argLine = ($appArgs | ForEach-Object { ConvertTo-CommandLineArg $_ }) -join ' '
    $script:App = Start-Process -FilePath $Java -ArgumentList $argLine -WorkingDirectory $RunDir `
            -RedirectStandardOutput $AppLog -RedirectStandardError $AppErrLog -NoNewWindow -PassThru

    # --- 7. wait for /actuator/health to report UP -------------------------------------------
    Write-Host "Waiting for the application on port $Port (up to ${HealthTimeout}s)..."
    $healthy = $false
    for ($t = 0; $t -lt $HealthTimeout; $t++) {
        if ($script:App.HasExited) { break }
        try {
            $health = Invoke-RestMethod -Uri "http://localhost:$Port/actuator/health" -TimeoutSec 2 -UseBasicParsing
            if ($health.status -eq 'UP') { $healthy = $true; break }
        } catch {
            # not listening yet, or DOWN (503)
        }
        Start-Sleep -Seconds 1
    }
    if (-not $healthy) {
        if ($script:App.HasExited) {
            Write-Err 'The application stopped during startup.'
        } else {
            Write-Err "The application did not become healthy within ${HealthTimeout}s."
        }
        Stop-App
        Show-AppLogTail
        exit 3    # 'exit' still runs the finally blocks
    }

    # --- 8. the Gatling runs -----------------------------------------------------------------
    # The simulations judge themselves; a failed assertion is the expected baseline result, so it
    # must not stop the run (failOnError=false). The before/after table below is the verdict here.
    $savedBaseUrl = $env:LAB_BASE_URL
    $savedDuration = $env:LAB_DURATION_SECONDS
    $env:LAB_BASE_URL = "http://localhost:$Port"
    $env:LAB_DURATION_SECONDS = [string] $Duration
    try {
        for ($run = 1; $run -le $Runs; $run++) {
            Write-Host "Gatling run $run of $Runs (${Duration}s, $Simulation)..."
            $simArg = "-Dgatling.simulationClass=$Simulation"
            $resultsArg = '-Dgatling.resultsFolder=' + (Join-Path $RunDirRel "gatling-$run")
            Push-Location -LiteralPath $Repo
            try { & $Mvnw -q 'gatling:test' $simArg '-Dgatling.failOnError=false' $resultsArg; $gatlingExit = $LASTEXITCODE } finally { Pop-Location }
            # failOnError=false also hides a crash, so check that the run left a report behind
            $report = Get-ChildItem -Path (Join-Path $RunDir "gatling-$run") -Filter 'stats.js' -Recurse -File -ErrorAction SilentlyContinue |
                    Where-Object { $_.Directory.Name -eq 'js' } | Select-Object -First 1
            if (-not $report) {
                Write-Err "Gatling run $run failed to produce a report"
                exit 1
            }
            if ($gatlingExit -ne 0) {
                Write-Err "Gatling run $run failed"
                exit 1
            }
        }
    } finally {
        $env:LAB_BASE_URL = $savedBaseUrl
        $env:LAB_DURATION_SECONDS = $savedDuration
    }

    # --- 9. lab statistics while the app still runs; log counters once it has stopped ---------
    if ($StatsPath) {
        try {
            Invoke-WebRequest -Uri "http://localhost:$Port$StatsPath" -TimeoutSec 10 -UseBasicParsing `
                    -OutFile (Join-Path $RunDir 'stats.json')
        } catch {
            Write-Err "Warning: could not read $StatsPath $Dash continuing without lab statistics"
        }
    }

    # --- 10. stop the app, record the run and print the comparison ---------------------------
    Stop-App

    $counterArgs = @()
    foreach ($key in @($Props.Keys)) {
        if (-not $key.StartsWith('log.counter.')) { continue }
        $name = $key.Substring('log.counter.'.Length)
        $spec = [string] $Props[$key]
        $colon = $spec.IndexOf(':')
        $file = if ($colon -ge 0) { $spec.Substring(0, $colon) } else { '' }
        if ($file -cne 'app.log' -and $file -cne 'gc.log') {
            Write-Err "Warning: log.counter.$name must be app.log:<text> or gc.log:<text> $Dash skipped"
            continue
        }
        $text = $spec.Substring($colon + 1)
        # app.log counts the JVM's stderr too, as lab.sh does with one combined log
        $files = if ($file -ceq 'app.log') { @($AppLog, $AppErrLog) } else { @(Join-Path $RunDir 'gc.log') }
        $count = 0
        foreach ($f in $files) {
            if (Test-Path -LiteralPath $f -PathType Leaf) {
                $count += @(Select-String -LiteralPath $f -Pattern $text -SimpleMatch -CaseSensitive).Count
            }
        }
        $counterArgs += @('--counter', "$name=$count")
    }

    & $Java -cp $Jar "-Dloader.main=$ReportMain" $Launcher `
            --lab $LabId --phase $Phase --branch $Branch --run-dir $RunDir --props $PropsFile @counterArgs
    if ($LASTEXITCODE -ne 0) {
        Write-Err 'Recording the run failed'
        $exitCode = 1
    }
} finally {
    Stop-App
}
exit $exitCode
