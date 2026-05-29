#!/usr/bin/env bash
#
# gc-analyze.sh — summarize a JVM unified-logging (-Xlog:gc*) G1 GC log.
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
#   ./scripts/gc-analyze.sh [path-to-gc.log]      (default: /tmp/gc.log)
#
# Re-capture a full log with, e.g.:
#   java -Xlog:gc*:file=/tmp/gc.log:tags,uptime,level ...
#
set -uo pipefail

LOG="${1:-/tmp/gc.log}"

if [ ! -f "$LOG" ]; then
    echo "Error: GC log not found: $LOG" >&2
    echo "Usage: $0 [path-to-gc.log]" >&2
    exit 1
fi

echo "=================================================================================="
echo " GC log analysis: $LOG"
echo "=================================================================================="

awk '
# Convert a size token (e.g. 195M, 384K, 2G, 1056768K, 512B, raw bytes) to MB.
function tomb(v,   n) {
    n = v
    if      (v ~ /[Gg]$/) { sub(/[Gg]$/, "", n); return n * 1024 }
    else if (v ~ /[Mm]$/) { sub(/[Mm]$/, "", n); return n + 0 }
    else if (v ~ /[Kk]$/) { sub(/[Kk]$/, "", n); return n / 1024 }
    else if (v ~ /[Bb]$/) { sub(/[Bb]$/, "", n); return n / 1048576 }
    else                  { return (n + 0) / 1048576 }   # plain bytes
}

BEGIN { regionmb = 0; maxg = -1 }

# --- Heap region size (needed to turn region counts into MB) ----------------
/Heap Region Size:/ {
    if (match($0, /Heap Region Size: [0-9.]+[KMGBkmgb]?/)) {
        s = substr($0, RSTART, RLENGTH); sub(/Heap Region Size: /, "", s)
        regionmb = tomb(s)
    }
}

# --- Extract the GC id present on most lines: "GC(7)" -> 7 ------------------
{
    gid = -1
    if (match($0, /GC\([0-9]+\)/)) {
        g = substr($0, RSTART, RLENGTH); gsub(/[^0-9]/, "", g); gid = g + 0
    }
}

# --- Heap total: "... A->B(C) ..." on Pause lines --------------------------
/Pause/ && /->/ {
    if (gid >= 0 && match($0, /[0-9.]+[KMGBkmgb]?->[0-9.]+[KMGBkmgb]?\([0-9.]+[KMGBkmgb]?\)/)) {
        hstart = RSTART
        split(substr($0, RSTART, RLENGTH), h, /->|\(|\)/)
        hb[gid] = tomb(h[1]); ha[gid] = tomb(h[2]); ht[gid] = tomb(h[3])
        haveheap[gid] = 1
        if (match($0, /GC\([0-9]+\) /)) {
            ts = RSTART + RLENGTH
            ty = substr($0, ts, hstart - ts); sub(/ +$/, "", ty); type[gid] = ty
        }
        seen[gid] = 1; if (gid > maxg) maxg = gid
    }
}

# --- Young gen after = (Eden_after + Survivor_after) * region size ----------
/Eden regions:/ {
    if (gid >= 0 && match($0, /Eden regions: [0-9]+->[0-9]+/)) {
        split(substr($0, RSTART, RLENGTH), e, /->/); n = e[2]; gsub(/[^0-9]/, "", n)
        eden[gid] = n + 0; haveyoung[gid] = 1; seen[gid] = 1; if (gid > maxg) maxg = gid
    }
}
/Survivor regions:/ {
    if (gid >= 0 && match($0, /Survivor regions: [0-9]+->[0-9]+/)) {
        split(substr($0, RSTART, RLENGTH), e, /->/); n = e[2]; gsub(/[^0-9]/, "", n)
        surv[gid] = n + 0; haveyoung[gid] = 1; seen[gid] = 1; if (gid > maxg) maxg = gid
    }
}

# --- Old gen after = (Old_after + Humongous_after) * region size -----------
/Old regions:/ {
    if (gid >= 0 && match($0, /Old regions: [0-9]+->[0-9]+/)) {
        split(substr($0, RSTART, RLENGTH), e, /->/); n = e[2]; gsub(/[^0-9]/, "", n)
        old[gid] = n + 0; haveold[gid] = 1; seen[gid] = 1; if (gid > maxg) maxg = gid
    }
}
/Humongous regions:/ {
    if (gid >= 0 && match($0, /Humongous regions: [0-9]+->[0-9]+/)) {
        split(substr($0, RSTART, RLENGTH), e, /->/); n = e[2]; gsub(/[^0-9]/, "", n)
        humo[gid] = n + 0; haveold[gid] = 1; seen[gid] = 1; if (gid > maxg) maxg = gid
    }
}

# --- Metaspace: "Metaspace: <usedBefore>(<commit>)->...->... " ------------
# Handles new "1234K(1408K)->1234K(1408K)" and old "5051K->5051K(1056768K)".
/Metaspace:/ {
    if (gid >= 0) {
        ms = $0; sub(/.*Metaspace: /, "", ms)
        split(ms, p, /->/)
        b = p[1]; a = (length(p[2]) > 0) ? p[2] : p[1]
        sub(/\(.*/, "", b)
        sub(/\(.*/, "", a); sub(/[ ,].*/, "", a)
        metab[gid] = tomb(b); metaa[gid] = tomb(a)
        havemeta[gid] = 1; seen[gid] = 1; if (gid > maxg) maxg = gid
    }
}

END {
    if (maxg < 0) { print " No GC events found in log."; exit }

    young_unit = (regionmb > 0) ? "" : " (regions; size unknown)"

    printf "%-4s  %-30s %9s %9s %9s  %10s %10s  %9s %9s\n", \
           "GC#", "Type", "HeapBef", "HeapAft", "HeapTot", "YoungAft", "OldAft", "MetaBef", "MetaAft"
    printf "%-4s  %-30s %9s %9s %9s  %10s %10s  %9s %9s\n", \
           "----", "------------------------------", "-------", "-------", "-------", "--------", "------", "-------", "-------"

    sawyoung = 0; sawmeta = 0; sawold = 0
    peakbef = 0; capacity = 0; metafirst = -1; metalast = -1
    ngc = 0

    for (i = 0; i <= maxg; i++) {
        if (!(i in seen)) continue
        ngc++

        hbS = (i in haveheap) ? sprintf("%.1fM", hb[i]) : "-"
        haS = (i in haveheap) ? sprintf("%.1fM", ha[i]) : "-"
        htS = (i in haveheap) ? sprintf("%.0fM", ht[i]) : "-"
        if (i in haveheap) {
            if (hb[i] > peakbef) peakbef = hb[i]
            capacity = ht[i]
        }

        if (i in haveyoung) {
            sawyoung = 1
            yreg = eden[i] + surv[i]
            yS = (regionmb > 0) ? sprintf("%.1fM", yreg * regionmb) : sprintf("%dreg", yreg)
        } else { yS = "-" }

        if (i in haveold) {
            sawold = 1
            oreg = old[i] + humo[i]
            oS = (regionmb > 0) ? sprintf("%.1fM", oreg * regionmb) : sprintf("%dreg", oreg)
        } else { oS = "-" }

        if (i in havemeta) {
            sawmeta = 1
            mbS = sprintf("%.1fM", metab[i]); maS = sprintf("%.1fM", metaa[i])
            if (metafirst < 0) metafirst = metab[i]
            metalast = metaa[i]
        } else { mbS = "-"; maS = "-" }

        tt = type[i]; if (length(tt) > 30) tt = substr(tt, 1, 29) "~"
        printf "%-4d  %-30s %9s %9s %9s  %10s %10s  %9s %9s\n", \
               i, tt, hbS, haS, htS, yS, oS, mbS, maS
    }

    print ""
    print "Summary:"
    printf "  GC events            : %d\n", ngc
    if (regionmb > 0) printf "  Heap region size     : %.0fM\n", regionmb
    if (peakbef > 0)  printf "  Peak heap before GC  : %.1fM (of %.0fM capacity)\n", peakbef, capacity
    if (metafirst >= 0) printf "  Metaspace            : %.1fM -> %.1fM (used, first to last GC)\n", metafirst, metalast

    if (!sawyoung && !sawold && !sawmeta) {
        print ""
        print "  Note: this log has only heap totals (captured at -Xlog:gc level)."
        print "        Young/old gen and metaspace need detail lines. Re-capture with:"
        print "          -Xlog:gc*:file=/tmp/gc.log:tags,uptime,level"
    } else if ((sawyoung || sawold) && regionmb == 0) {
        print ""
        print "  Note: heap region size not found in log; young/old shown as region counts."
        print "        Include gc,init lines (use -Xlog:gc*) so sizes can be converted to MB."
    }
}
' "$LOG"

echo "=================================================================================="
