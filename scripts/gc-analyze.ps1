#!/usr/bin/env pwsh
#
# gc-analyze.ps1 — summarize a JVM unified-logging (-Xlog:gc*) G1 GC log.
# PowerShell port of gc-analyze.sh (cross-platform: Windows PowerShell 5+ / pwsh 7+).
#
# For each GC it prints:
#   - heap total, before and after the collection           (Pause ... A->B(C))
#   - young and old generation occupancy after the collection (region lines)
#   - metaspace used, before and after the collection         (Metaspace line)
#
# Young/old/metaspace require detail lines that only appear at -Xlog:gc* level.
# A log captured at plain -Xlog:gc / -verbose:gc has only the heap totals; the
# script still reports those and tells you how to re-capture the rest.
#
# Usage:
#   ./scripts/gc-analyze.ps1 [-LogPath <path>]      (default: /tmp/gc.log)
#
# Re-capture a full log with, e.g.:
#   java -Xlog:gc*:file=/tmp/gc.log:tags,uptime,level ...
#
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$LogPath = "/tmp/gc.log"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $LogPath -PathType Leaf)) {
    [Console]::Error.WriteLine("Error: GC log not found: $LogPath")
    exit 1
}
$resolvedPath = (Resolve-Path -LiteralPath $LogPath).ProviderPath

# Convert a size token (e.g. 195M, 384K, 2G, 1056768K, 512B, raw bytes) to MB.
function Convert-ToMB {
    param([string]$v)
    if     ($v -match '^([0-9.]+)\s*[Gg]$') { return [double]$Matches[1] * 1024 }
    elseif ($v -match '^([0-9.]+)\s*[Mm]$') { return [double]$Matches[1] }
    elseif ($v -match '^([0-9.]+)\s*[Kk]$') { return [double]$Matches[1] / 1024 }
    elseif ($v -match '^([0-9.]+)\s*[Bb]$') { return [double]$Matches[1] / 1048576 }
    elseif ($v -match '^([0-9.]+)$')        { return [double]$Matches[1] / 1048576 }
    else { return 0.0 }
}

# Per-GC accumulator: id -> hashtable of fields.
$gcs = [ordered]@{}
function Get-Entry {
    param([int]$id)
    if (-not $gcs.Contains($id)) {
        $gcs[$id] = @{
            Type = ''; HaveHeap = $false; HB = 0.0; HA = 0.0; HT = 0.0
            HaveYoung = $false; Eden = 0; Surv = 0
            HaveOld = $false; Old = 0; Humo = 0
            HaveMeta = $false; MetaB = 0.0; MetaA = 0.0
        }
    }
    return $gcs[$id]
}

$regionMB = 0.0

foreach ($line in [System.IO.File]::ReadLines($resolvedPath)) {

    # Heap region size (needed to turn region counts into MB)
    if ($line -match 'Heap Region Size:\s*([0-9.]+[KMGBkmgb]?)') {
        $regionMB = Convert-ToMB $Matches[1]
    }

    # GC id present on most lines: "GC(7)" -> 7
    $gid = -1
    if ($line -match 'GC\((\d+)\)') { $gid = [int]$Matches[1] }
    if ($gid -lt 0) { continue }

    # Heap total: "... A->B(C) ..." on Pause lines
    if ($line -match 'Pause' -and
        $line -match '([0-9.]+[KMGBkmgb]?)->([0-9.]+[KMGBkmgb]?)\(([0-9.]+[KMGBkmgb]?)\)') {
        $e = Get-Entry $gid
        $e.HB = Convert-ToMB $Matches[1]
        $e.HA = Convert-ToMB $Matches[2]
        $e.HT = Convert-ToMB $Matches[3]
        $e.HaveHeap = $true
        # Type: text between "GC(n) " and the heap numbers (lazy match)
        if ($line -match 'GC\(\d+\)\s+(.+?)\s+[0-9.]+[KMGBkmgb]?->[0-9.]+[KMGBkmgb]?\(') {
            $e.Type = $Matches[1]
        }
    }

    # Young gen after = (Eden_after + Survivor_after) * region size
    if ($line -match 'Eden regions:\s*\d+->(\d+)') {
        $e = Get-Entry $gid; $e.Eden = [int]$Matches[1]; $e.HaveYoung = $true
    }
    if ($line -match 'Survivor regions:\s*\d+->(\d+)') {
        $e = Get-Entry $gid; $e.Surv = [int]$Matches[1]; $e.HaveYoung = $true
    }

    # Old gen after = (Old_after + Humongous_after) * region size
    if ($line -match 'Old regions:\s*\d+->(\d+)') {
        $e = Get-Entry $gid; $e.Old = [int]$Matches[1]; $e.HaveOld = $true
    }
    if ($line -match 'Humongous regions:\s*\d+->(\d+)') {
        $e = Get-Entry $gid; $e.Humo = [int]$Matches[1]; $e.HaveOld = $true
    }

    # Metaspace: handles "1234K(1408K)->1234K(1408K)" and "5051K->6020K(1056768K)"
    if ($line -match 'Metaspace:\s*(\S+)->(\S+)') {
        $b = $Matches[1] -replace '\(.*', ''
        $a = ($Matches[2] -replace '\(.*', '') -replace '[, ].*', ''
        $e = Get-Entry $gid
        $e.MetaB = Convert-ToMB $b
        $e.MetaA = Convert-ToMB $a
        $e.HaveMeta = $true
    }
}

$sep = '=' * 82
Write-Host $sep
Write-Host " GC log analysis: $LogPath"
Write-Host $sep

if ($gcs.Count -eq 0) {
    Write-Host " No GC events found in log."
    Write-Host $sep
    exit 0
}

$hdr = '{0,-4}  {1,-30} {2,9} {3,9} {4,9}  {5,10} {6,10}  {7,9} {8,9}'
Write-Host ($hdr -f 'GC#', 'Type', 'HeapBef', 'HeapAft', 'HeapTot', 'YoungAft', 'OldAft', 'MetaBef', 'MetaAft')
Write-Host ($hdr -f '----', ('-' * 30), '-------', '-------', '-------', '--------', '------', '-------', '-------')

$sawYoung = $false; $sawOld = $false; $sawMeta = $false
$peakBef = 0.0; $capacity = 0.0; $metaFirst = $null; $metaLast = $null
$ngc = 0

foreach ($id in ($gcs.Keys | Sort-Object { [int]$_ })) {
    $e = $gcs[$id]
    $ngc++

    if ($e.HaveHeap) {
        $hbS = '{0:0.0}M' -f $e.HB
        $haS = '{0:0.0}M' -f $e.HA
        $htS = '{0:0}M'   -f $e.HT
        if ($e.HB -gt $peakBef) { $peakBef = $e.HB }
        $capacity = $e.HT
    } else { $hbS = '-'; $haS = '-'; $htS = '-' }

    if ($e.HaveYoung) {
        $sawYoung = $true
        $yreg = $e.Eden + $e.Surv
        $yS = if ($regionMB -gt 0) { '{0:0.0}M' -f ($yreg * $regionMB) } else { "${yreg}reg" }
    } else { $yS = '-' }

    if ($e.HaveOld) {
        $sawOld = $true
        $oreg = $e.Old + $e.Humo
        $oS = if ($regionMB -gt 0) { '{0:0.0}M' -f ($oreg * $regionMB) } else { "${oreg}reg" }
    } else { $oS = '-' }

    if ($e.HaveMeta) {
        $sawMeta = $true
        $mbS = '{0:0.0}M' -f $e.MetaB
        $maS = '{0:0.0}M' -f $e.MetaA
        if ($null -eq $metaFirst) { $metaFirst = $e.MetaB }
        $metaLast = $e.MetaA
    } else { $mbS = '-'; $maS = '-' }

    $tt = $e.Type
    if ($tt.Length -gt 30) { $tt = $tt.Substring(0, 29) + '~' }

    Write-Host ($hdr -f $id, $tt, $hbS, $haS, $htS, $yS, $oS, $mbS, $maS)
}

Write-Host ""
Write-Host "Summary:"
Write-Host ("  GC events            : {0}" -f $ngc)
if ($regionMB -gt 0) { Write-Host ("  Heap region size     : {0:0}M" -f $regionMB) }
if ($peakBef -gt 0)  { Write-Host ("  Peak heap before GC  : {0:0.0}M (of {1:0}M capacity)" -f $peakBef, $capacity) }
if ($null -ne $metaFirst) {
    Write-Host ("  Metaspace            : {0:0.0}M -> {1:0.0}M (used, first to last GC)" -f $metaFirst, $metaLast)
}

if (-not $sawYoung -and -not $sawOld -and -not $sawMeta) {
    Write-Host ""
    Write-Host "  Note: this log has only heap totals (captured at -Xlog:gc level)."
    Write-Host "        Young/old gen and metaspace need detail lines. Re-capture with:"
    Write-Host "          -Xlog:gc*:file=/tmp/gc.log:tags,uptime,level"
} elseif (($sawYoung -or $sawOld) -and $regionMB -eq 0) {
    Write-Host ""
    Write-Host "  Note: heap region size not found in log; young/old shown as region counts."
    Write-Host "        Include gc,init lines (use -Xlog:gc*) so sizes can be converted to MB."
}

Write-Host $sep
