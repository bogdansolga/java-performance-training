#!/usr/bin/env bash
#
# load-test.sh — simple curl-based load generator for ProfiledProductController.
#
# Hits the endpoints exposed by:
#   src/main/java/net/safedata/performance/training/controller/ProfiledProductController.java
#
#   GET /profiled/product/long/{productType}
#   GET /profiled/product/long/sync/{productType}
#   GET /profiled/product/deferred-result
#   GET /profiled/product/cf
#   GET /profiled/product/pool-size
#
# Simulates concurrent users (default 100, configurable), each firing a number
# of requests against a randomly-chosen (or pinned) endpoint, then prints
# aggregate latency / status-code statistics.
#
# Usage:
#   ./scripts/load-test.sh [options]
#
# Options:
#   -u USERS        Number of concurrent users        (default: 100)
#   -n REQUESTS     Requests per user                 (default: 10)
#   -H HOST         Base URL                          (default: http://localhost:8080)
#   -r RAMP_SECS    Ramp-up window in seconds; users  (default: 0 = all at once)
#                   are started spread across it
#   -t TIMEOUT      Per-request timeout in seconds    (default: 30)
#   -e ENDPOINT     Pin all traffic to one endpoint   (default: random mix)
#                   one of: long | sync | deferred | cf | pool
#   -p TYPE         productType path value for long/  (default: electronics)
#                   sync endpoints
#   -h              Show this help and exit
#
# Examples:
#   ./scripts/load-test.sh                       # 100 users x 10 reqs, random mix
#   ./scripts/load-test.sh -u 250 -n 20          # 250 users x 20 reqs
#   ./scripts/load-test.sh -u 50 -r 10           # 50 users ramped over 10s
#   ./scripts/load-test.sh -e long -p books      # only the long endpoint, type=books
#
set -uo pipefail

# ----------------------------------------------------------------------------
# Defaults (override via flags or environment)
# ----------------------------------------------------------------------------
USERS="${USERS:-100}"
REQUESTS="${REQUESTS:-10}"
HOST="${HOST:-http://localhost:8080}"
RAMP_SECS="${RAMP_SECS:-0}"
TIMEOUT="${TIMEOUT:-30}"
ENDPOINT="${ENDPOINT:-}"
PRODUCT_TYPE="${PRODUCT_TYPE:-electronics}"

usage() {
    sed -n '2,/^set -uo/p' "$0" | sed 's/^# \{0,1\}//; s/^#//; /^set -uo/d'
    exit "${1:-0}"
}

while getopts ":u:n:H:r:t:e:p:h" opt; do
    case "$opt" in
        u) USERS="$OPTARG" ;;
        n) REQUESTS="$OPTARG" ;;
        H) HOST="$OPTARG" ;;
        r) RAMP_SECS="$OPTARG" ;;
        t) TIMEOUT="$OPTARG" ;;
        e) ENDPOINT="$OPTARG" ;;
        p) PRODUCT_TYPE="$OPTARG" ;;
        h) usage 0 ;;
        :) echo "Error: -$OPTARG requires an argument" >&2; usage 1 ;;
        \?) echo "Error: unknown option -$OPTARG" >&2; usage 1 ;;
    esac
done

# ----------------------------------------------------------------------------
# Validation
# ----------------------------------------------------------------------------
command -v curl >/dev/null 2>&1 || { echo "Error: curl is not installed" >&2; exit 1; }

is_pos_int() { [[ "$1" =~ ^[0-9]+$ ]] && [ "$1" -gt 0 ]; }
is_nonneg_int() { [[ "$1" =~ ^[0-9]+$ ]]; }

is_pos_int "$USERS"      || { echo "Error: USERS must be a positive integer" >&2; exit 1; }
is_pos_int "$REQUESTS"   || { echo "Error: REQUESTS must be a positive integer" >&2; exit 1; }
is_nonneg_int "$RAMP_SECS" || { echo "Error: RAMP_SECS must be a non-negative integer" >&2; exit 1; }
is_pos_int "$TIMEOUT"    || { echo "Error: TIMEOUT must be a positive integer" >&2; exit 1; }

# Endpoint path builder. Echoes the path for a given key.
endpoint_path() {
    case "$1" in
        long)     echo "/profiled/product/long/${PRODUCT_TYPE}" ;;
        sync)     echo "/profiled/product/long/sync/${PRODUCT_TYPE}" ;;
        deferred) echo "/profiled/product/deferred-result" ;;
        cf)       echo "/profiled/product/cf" ;;
        pool)     echo "/profiled/product/pool-size" ;;
        *)        return 1 ;;
    esac
}

ENDPOINT_KEYS=(long sync deferred cf pool)

if [ -n "$ENDPOINT" ]; then
    endpoint_path "$ENDPOINT" >/dev/null || {
        echo "Error: unknown endpoint '$ENDPOINT' (expected: ${ENDPOINT_KEYS[*]})" >&2
        exit 1
    }
fi

# ----------------------------------------------------------------------------
# Run directory for per-request results
# ----------------------------------------------------------------------------
RESULTS_DIR="$(mktemp -d "${TMPDIR:-/tmp}/loadtest.XXXXXX")"
cleanup() { rm -rf "$RESULTS_DIR"; }
trap cleanup EXIT

# ----------------------------------------------------------------------------
# Worker: one simulated user. Fires REQUESTS requests, appends a line per
# request to its own result file: "<http_code> <time_total_seconds>".
# Picks a random endpoint per request unless one is pinned.
# ----------------------------------------------------------------------------
run_user() {
    local user_id="$1"
    local out_file="${RESULTS_DIR}/user_${user_id}.txt"
    local i key path code_time

    for ((i = 0; i < REQUESTS; i++)); do
        if [ -n "$ENDPOINT" ]; then
            key="$ENDPOINT"
        else
            key="${ENDPOINT_KEYS[RANDOM % ${#ENDPOINT_KEYS[@]}]}"
        fi
        path="$(endpoint_path "$key")"

        # -s silent, -o discard body, -w writes "code time" to stdout.
        # --max-time bounds each request; on timeout/failure code is 000.
        # Each result line is "<endpoint_key> <http_code> <time_total>".
        code_time="$(curl -s -o /dev/null \
                          --max-time "$TIMEOUT" \
                          -w '%{http_code} %{time_total}' \
                          "${HOST}${path}" 2>/dev/null)" \
            || code_time="000 0"
        [ -n "$code_time" ] || code_time="000 0"
        echo "${key} ${code_time}" >> "$out_file"
    done
}

# ----------------------------------------------------------------------------
# Launch
# ----------------------------------------------------------------------------
echo "================================================================"
echo " Load test: ProfiledProductController"
echo "----------------------------------------------------------------"
echo " Host           : ${HOST}"
echo " Users          : ${USERS}"
echo " Requests/user  : ${REQUESTS}"
echo " Total requests : $((USERS * REQUESTS))"
echo " Endpoint       : ${ENDPOINT:-random mix (${ENDPOINT_KEYS[*]})}"
echo " productType    : ${PRODUCT_TYPE}"
echo " Ramp-up        : ${RAMP_SECS}s"
echo " Timeout        : ${TIMEOUT}s/request"
echo "================================================================"

# Per-user ramp delay (integer-friendly via awk for fractional sleeps).
ramp_delay="0"
if [ "$RAMP_SECS" -gt 0 ] && [ "$USERS" -gt 1 ]; then
    ramp_delay="$(awk -v r="$RAMP_SECS" -v u="$USERS" 'BEGIN { printf "%.4f", r / u }')"
fi

start_ts="$(date +%s)"

for ((u = 1; u <= USERS; u++)); do
    run_user "$u" &
    if [ "$ramp_delay" != "0" ]; then
        sleep "$ramp_delay"
    fi
done

echo "Started ${USERS} users; waiting for completion..."
wait

end_ts="$(date +%s)"
elapsed="$((end_ts - start_ts))"
[ "$elapsed" -lt 1 ] && elapsed=1

# ----------------------------------------------------------------------------
# Aggregate results
# ----------------------------------------------------------------------------
echo
echo "================================================================"
echo " Results"
echo "================================================================"

cat "$RESULTS_DIR"/user_*.txt 2>/dev/null | awk -v elapsed="$elapsed" '
{
    ep   = $1
    code = $2
    t    = $3 + 0.0

    total++
    codes[code]++
    ep_total[ep]++
    ep_codes[ep, code]++
    seen_ep[ep] = 1

    if (code ~ /^2/) {
        ok++
        sum += t
        times[ok] = t
        if (ok == 1 || t < min) min = t
        if (ok == 1 || t > max) max = t

        ep_ok[ep]++
        ep_sum[ep] += t
        ep_times[ep, ep_ok[ep]] = t
        if (ep_ok[ep] == 1 || t < ep_min[ep]) ep_min[ep] = t
        if (ep_ok[ep] == 1 || t > ep_max[ep]) ep_max[ep] = t
    } else {
        fail++
        ep_fail[ep]++
    }
}

# percentile from a 1-based, ascending-sorted array slice held in arr[prefix,1..n]
function pct(arr, prefix, n, p,   idx) {
    idx = int((n - 1) * p) + 1
    return arr[prefix, idx]
}

# in-place insertion sort of arr[prefix,1..n] (n is small per endpoint / per run)
function isort(arr, prefix, n,   i, j, key) {
    for (i = 2; i <= n; i++) {
        key = arr[prefix, i]
        j = i - 1
        while (j >= 1 && arr[prefix, j] > key) {
            arr[prefix, j + 1] = arr[prefix, j]
            j--
        }
        arr[prefix, j + 1] = key
    }
}

END {
    if (total == 0) { print " No results collected."; exit }

    printf " Total requests   : %d\n", total
    printf " Successful (2xx) : %d\n", ok
    printf " Failed           : %d\n", fail
    printf " Throughput       : %.2f req/s (wall: %ds)\n", total / elapsed, elapsed
    print  " ----------------------------------------------------------------"
    print  " Status codes (all endpoints):"
    for (c in codes) printf "   %-5s : %d\n", (c == "000" ? "ERR" : c), codes[c]

    # --- Per-endpoint breakdown ---------------------------------------------
    print  " ----------------------------------------------------------------"
    print  " Per-endpoint breakdown:"
    printf "   %-10s %7s %7s %7s %7s  %8s %8s %8s %8s\n", \
           "endpoint", "total", "ok", "fail", "ok%", "avg(s)", "p50", "p95", "max"
    printf "   %-10s %7s %7s %7s %7s  %8s %8s %8s %8s\n", \
           "--------", "-----", "--", "----", "---", "------", "---", "---", "---"
    for (ep in seen_ep) {
        eok = ep_ok[ep] + 0
        et  = ep_total[ep] + 0
        ef  = ep_fail[ep] + 0
        okpct = (et > 0) ? (100.0 * eok / et) : 0
        if (eok > 0) {
            isort(ep_times, ep, eok)
            eavg = ep_sum[ep] / eok
            ep50 = pct(ep_times, ep, eok, 0.50)
            ep95 = pct(ep_times, ep, eok, 0.95)
            emax = ep_max[ep]
            printf "   %-10s %7d %7d %7d %6.1f%%  %8.3f %8.3f %8.3f %8.3f\n", \
                   ep, et, eok, ef, okpct, eavg, ep50, ep95, emax
        } else {
            printf "   %-10s %7d %7d %7d %6.1f%%  %8s %8s %8s %8s\n", \
                   ep, et, eok, ef, okpct, "-", "-", "-", "-"
        }
    }

    # Per-endpoint status-code detail (helps tell timeouts apart from 5xx)
    print  " ----------------------------------------------------------------"
    print  " Status codes per endpoint:"
    for (ep in seen_ep) {
        line = ""
        for (c in codes) {
            key = ep SUBSEP c
            if (key in ep_codes) {
                lbl = (c == "000" ? "ERR" : c)
                line = line sprintf("%s=%d  ", lbl, ep_codes[key])
            }
        }
        printf "   %-10s : %s\n", ep, line
    }

    # --- Overall latency -----------------------------------------------------
    if (ok > 0) {
        n = ok
        # times[1..n] holds successful latencies; sort ascending in place
        for (i = 2; i <= n; i++) {
            key = times[i]; j = i - 1
            while (j >= 1 && times[j] > key) { times[j+1] = times[j]; j-- }
            times[j+1] = key
        }
        p50 = times[int((n - 1) * 0.50) + 1]
        p90 = times[int((n - 1) * 0.90) + 1]
        p95 = times[int((n - 1) * 0.95) + 1]
        p99 = times[int((n - 1) * 0.99) + 1]
        print  " ----------------------------------------------------------------"
        print  " Latency (all successful requests, seconds):"
        printf "   min : %.3f   avg : %.3f   max : %.3f\n", min, sum / ok, max
        printf "   p50 : %.3f   p90 : %.3f   p95 : %.3f   p99 : %.3f\n", p50, p90, p95, p99
    }
}'

echo "================================================================"
