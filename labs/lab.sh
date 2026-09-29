#!/usr/bin/env bash
# Runs one lab end to end and prints the before/after table.
#
#   labs/lab.sh <lab> <baseline|verify> [--runs N] [--gc serial|g1|zgc] [--seconds S]
#
#   <lab>      lab number (1), slug (retention) or directory name (1-retention) under labs/
#   --runs     Gatling runs against one application start (default 3)
#   --gc       replace the lab's collector: serial, g1 or zgc
#   --seconds  duration of each Gatling run (default 60)
#
# Environment: LAB_EXTRA_JVM_OPTS is appended to the lab's JVM options.
# Exit codes: 0 ok, 1 build/load-test/report failure, 2 port busy, 3 app not healthy, 64 usage.
# Windows: use labs\lab.ps1, which behaves the same.

set -euo pipefail

PORT=8080
HEALTH_TIMEOUT=90
REPORT_MAIN=net.safedata.performance.training.lab.harness.LabReportMain
LAUNCHER=org.springframework.boot.loader.launch.PropertiesLauncher

usage() {
    echo "Usage: labs/lab.sh <lab> <baseline|verify> [--runs N] [--gc serial|g1|zgc] [--seconds S]" >&2
    echo "  <lab> is a number (1), a slug (retention) or a directory name (1-retention) under labs/" >&2
    exit 64
}

die_usage() {
    echo "$1" >&2
    usage
}

is_positive_int() {
    [[ "$1" =~ ^[0-9]+$ ]] && (( 10#$1 > 0 ))
}

# --- arguments -------------------------------------------------------------------------------
[[ $# -ge 1 && ( "$1" == "-h" || "$1" == "--help" ) ]] && usage
[[ $# -lt 2 ]] && usage

LAB_ARG=$1
PHASE=$2
shift 2
RUNS=3
DURATION=60
GC=""

case "$PHASE" in
    baseline|verify) ;;
    *) die_usage "Phase must be baseline or verify, got: $PHASE" ;;
esac

while [[ $# -gt 0 ]]; do
    case "$1" in
        --runs)    [[ $# -ge 2 ]] || die_usage "--runs needs a value"; RUNS=$2; shift 2 ;;
        --seconds) [[ $# -ge 2 ]] || die_usage "--seconds needs a value"; DURATION=$2; shift 2 ;;
        --gc)      [[ $# -ge 2 ]] || die_usage "--gc needs a value"; GC=$2; shift 2 ;;
        -h|--help) usage ;;
        *) die_usage "Unknown option: $1" ;;
    esac
done

is_positive_int "$RUNS" || die_usage "--runs must be a positive whole number, got: $RUNS"
is_positive_int "$DURATION" || die_usage "--seconds must be a positive whole number, got: $DURATION"
case "$GC" in
    ""|serial|g1|zgc) ;;
    *) die_usage "--gc must be serial, g1 or zgc, got: $GC" ;;
esac

# --- 1. resolve the lab directory ------------------------------------------------------------
REPO=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
LABS="$REPO/labs"

shopt -s nullglob
if [[ "$LAB_ARG" =~ ^[0-9]+$ ]]; then
    candidates=("$LABS/$((10#$LAB_ARG))"-*/)
elif [[ "$LAB_ARG" =~ ^[0-9]+-[A-Za-z0-9._-]+$ ]]; then
    candidates=("$LABS/$LAB_ARG"/)
elif [[ "$LAB_ARG" =~ ^[A-Za-z0-9._-]+$ ]]; then
    candidates=("$LABS"/[0-9]*-"$LAB_ARG"/)
else
    candidates=()
fi
shopt -u nullglob

LAB_DIR=""
for dir in "${candidates[@]}"; do
    if [[ -f "$dir/lab.properties" ]]; then
        [[ -n "$LAB_DIR" ]] && die_usage "Lab '$LAB_ARG' is ambiguous — use the directory name"
        LAB_DIR=${dir%/}
    fi
done
[[ -n "$LAB_DIR" ]] || die_usage "No lab '$LAB_ARG' under $LABS (expected labs/<n>-<slug>/lab.properties)"
PROPS="$LAB_DIR/lab.properties"

# Reads one key from lab.properties (first '=' separates key and value; CRLF tolerated).
prop() {
    local key=$1 line k v
    while IFS= read -r line || [[ -n "$line" ]]; do
        line=${line%$'\r'}
        [[ "$line" =~ ^[[:space:]]*(#|!|$) ]] && continue
        [[ "$line" == *=* ]] || continue
        k=${line%%=*}; v=${line#*=}
        k=${k#"${k%%[![:space:]]*}"}; k=${k%"${k##*[![:space:]]}"}
        v=${v#"${v%%[![:space:]]*}"}; v=${v%"${v##*[![:space:]]}"}
        if [[ "$k" == "$key" ]]; then
            printf '%s' "$v"
            return 0
        fi
    done < "$PROPS"
    return 0
}

# Lists "name<TAB>file:text" for every log.counter.<name> key.
log_counters() {
    local line k v
    while IFS= read -r line || [[ -n "$line" ]]; do
        line=${line%$'\r'}
        [[ "$line" =~ ^[[:space:]]*log\.counter\. ]] || continue
        [[ "$line" == *=* ]] || continue
        k=${line%%=*}; v=${line#*=}
        k=${k#"${k%%[![:space:]]*}"}; k=${k%"${k##*[![:space:]]}"}
        v=${v#"${v%%[![:space:]]*}"}
        printf '%s\t%s\n' "${k#log.counter.}" "$v"
    done < "$PROPS"
}

LAB_ID=$(prop lab)
[[ -n "$LAB_ID" ]] || LAB_ID=$(basename "$LAB_DIR")
PROFILE=$(prop profile)
JVM_OPTIONS=$(prop jvm.options)
SIMULATION=$(prop simulation)
STATS_PATH=$(prop stats.path)
[[ -n "$PROFILE" ]] || { echo "$PROPS has no profile= entry" >&2; exit 64; }
[[ -n "$SIMULATION" ]] || { echo "$PROPS has no simulation= entry" >&2; exit 64; }

# --- 2. the port must be free ----------------------------------------------------------------
# curl exit 7 = nothing listening; anything else means something answered on the port.
port_busy() {
    local host rc
    for host in 127.0.0.1 "[::1]"; do
        rc=0
        curl -g -s -o /dev/null --max-time 3 "http://$host:$PORT/" || rc=$?
        [[ $rc -ne 7 ]] && return 0
    done
    return 1
}
if port_busy; then
    echo "Port $PORT is already in use — stop the other process first" >&2
    exit 2
fi

# --- 3. build when the jar is missing or older than the sources ------------------------------
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA=java
fi
command -v "$JAVA" > /dev/null || { echo "No java found: set JAVA_HOME or put java on the PATH" >&2; exit 1; }

find_jar() {
    local jar
    for jar in "$REPO"/target/*.jar; do
        [[ -f "$jar" ]] && { printf '%s' "$jar"; return 0; }
    done
    return 0
}

JAR=$(find_jar)
SIM_CLASS_FILE="$REPO/target/test-classes/${SIMULATION//.//}.class"
if [[ -z "$JAR" || ! -f "$SIM_CLASS_FILE" ]] \
        || [[ -n "$(find "$REPO/src" "$REPO/pom.xml" -type f -newer "$JAR" -print -quit)" ]]; then
    echo "Building the application (./mvnw -q -DskipTests package)..."
    (cd "$REPO" && ./mvnw -q -DskipTests package) || { echo "Build failed" >&2; exit 1; }
    JAR=$(find_jar)
    [[ -n "$JAR" ]] || { echo "Build produced no jar under target/" >&2; exit 1; }
fi

# --- 4. JVM options, with the optional collector override ------------------------------------
JAVA_MAJOR=$("$JAVA" -XshowSettings:properties -version 2>&1 \
    | sed -n 's/^[[:space:]]*java\.specification\.version = //p' | tr -d '\r' | head -1)
JAVA_MAJOR=${JAVA_MAJOR#1.}
[[ "$JAVA_MAJOR" =~ ^[0-9]+$ ]] || { echo "Cannot read the Java version of $JAVA" >&2; exit 1; }

read -r -a JVM_OPTS <<< "$JVM_OPTIONS"
if [[ -n "$GC" ]]; then
    kept=()
    for opt in ${JVM_OPTS[@]+"${JVM_OPTS[@]}"}; do
        [[ "$opt" =~ ^-XX:[+-]Use[A-Za-z0-9]*GC$ || "$opt" =~ ^-XX:[+-]ZGenerational$ ]] || kept+=("$opt")
    done
    JVM_OPTS=(${kept[@]+"${kept[@]}"})
    case "$GC" in
        serial) JVM_OPTS+=(-XX:+UseSerialGC) ;;
        g1)     JVM_OPTS+=(-XX:+UseG1GC) ;;
        zgc)
            JVM_OPTS+=(-XX:+UseZGC)
            # generational ZGC: opt-in on 21-22 (JEP 439), the default from 23 (JEP 474)
            if (( JAVA_MAJOR == 21 || JAVA_MAJOR == 22 )); then
                JVM_OPTS+=(-XX:+ZGenerational)
            elif (( JAVA_MAJOR < 21 )); then
                echo "ZGC on $JAVA_MAJOR is non-generational"
            fi
            ;;
    esac
fi
if [[ -n "${LAB_EXTRA_JVM_OPTS:-}" ]]; then
    read -r -a extra <<< "$LAB_EXTRA_JVM_OPTS"
    JVM_OPTS+=(${extra[@]+"${extra[@]}"})
fi

# --- 5. the run directory: results/<safe-branch>/<lab>/<yyyyMMdd-HHmmss>-<phase> --------------
BRANCH=$(git -C "$REPO" rev-parse --abbrev-ref HEAD 2> /dev/null || true)
[[ -n "$BRANCH" ]] || BRANCH=local
SAFE_BRANCH=$(printf '%s' "$BRANCH" | sed 's/[^A-Za-z0-9._-]/_/g')
# Relative to the repository: Maven resolves it against the project folder (no absolute path in -D)
RUN_DIR_REL="results/$SAFE_BRANCH/$LAB_ID/$(date +%Y%m%d-%H%M%S)-$PHASE"
RUN_DIR="$REPO/$RUN_DIR_REL"
mkdir -p "$RUN_DIR"
APP_LOG="$RUN_DIR/app.log"

# --- 6. start the application; the trap stops it on any exit ---------------------------------
APP_PID=""

stop_app() {
    [[ -n "$APP_PID" ]] || return 0
    if kill -0 "$APP_PID" 2> /dev/null; then
        kill "$APP_PID" 2> /dev/null || true
        for _ in $(seq 1 15); do
            kill -0 "$APP_PID" 2> /dev/null || break
            sleep 1
        done
        kill -9 "$APP_PID" 2> /dev/null || true
    fi
    wait "$APP_PID" 2> /dev/null || true
    APP_PID=""
}
trap stop_app EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

echo "Lab $LAB_ID ($PHASE) on branch $BRANCH, Java $JAVA_MAJOR, JVM options: ${JVM_OPTS[*]-}"
echo "Results: $RUN_DIR"
# Started inside the run directory, so gc.log is a relative path (no spaces or drive-letter colons for -Xlog).
(cd "$RUN_DIR" && exec "$JAVA" ${JVM_OPTS[@]+"${JVM_OPTS[@]}"} \
    "-Xlog:gc*:file=gc.log:uptime,level,tags" \
    -jar "$JAR" "--spring.profiles.active=$PROFILE" "--server.port=$PORT") > "$APP_LOG" 2>&1 &
APP_PID=$!

# --- 7. wait for /actuator/health to report UP -----------------------------------------------
echo "Waiting for the application on port $PORT (up to ${HEALTH_TIMEOUT}s)..."
healthy=false
for _ in $(seq 1 "$HEALTH_TIMEOUT"); do
    if ! kill -0 "$APP_PID" 2> /dev/null; then
        break
    fi
    if curl -fs --max-time 2 "http://localhost:$PORT/actuator/health" 2> /dev/null | grep -q '"status" *: *"UP"'; then
        healthy=true
        break
    fi
    sleep 1
done
if [[ "$healthy" != true ]]; then
    if kill -0 "$APP_PID" 2> /dev/null; then
        echo "The application did not become healthy within ${HEALTH_TIMEOUT}s. Last lines of $APP_LOG:" >&2
    else
        echo "The application stopped during startup. Last lines of $APP_LOG:" >&2
    fi
    tail -40 "$APP_LOG" >&2 || true
    stop_app
    exit 3
fi

# --- 8. the Gatling runs ---------------------------------------------------------------------
# The simulations judge themselves; a failed assertion is the expected baseline result, so it
# must not stop the run (failOnError=false). The before/after table below is the verdict here.
for i in $(seq 1 "$RUNS"); do
    echo "Gatling run $i of $RUNS (${DURATION}s, $SIMULATION)..."
    gatling_exit=0
    (cd "$REPO" && LAB_BASE_URL="http://localhost:$PORT" LAB_DURATION_SECONDS="$DURATION" \
            ./mvnw -q gatling:test "-Dgatling.simulationClass=$SIMULATION" -Dgatling.failOnError=false \
            "-Dgatling.resultsFolder=$RUN_DIR_REL/gatling-$i") || gatling_exit=$?
    # failOnError=false also hides a crash, so check that the run left a report behind
    if ! compgen -G "$RUN_DIR/gatling-$i/*/js/stats.js" > /dev/null; then
        echo "Gatling run $i failed to produce a report" >&2
        exit 1
    fi
    if [[ $gatling_exit -ne 0 ]]; then
        echo "Gatling run $i failed" >&2
        exit 1
    fi
done

# --- 9. lab statistics while the app still runs; log counters once it has stopped -------------
if [[ -n "$STATS_PATH" ]]; then
    curl -fs --max-time 10 "http://localhost:$PORT$STATS_PATH" -o "$RUN_DIR/stats.json" \
        || echo "Warning: could not read $STATS_PATH — continuing without lab statistics" >&2
fi

# --- 10. stop the app, record the run and print the comparison -------------------------------
stop_app

COUNTER_ARGS=()
while IFS=$'\t' read -r name spec; do
    [[ -n "$name" ]] || continue
    file=${spec%%:*}
    text=${spec#*:}
    case "$file" in
        app.log|gc.log) ;;
        *) echo "Warning: log.counter.$name must be app.log:<text> or gc.log:<text> — skipped" >&2; continue ;;
    esac
    count=0
    if [[ -f "$RUN_DIR/$file" ]]; then
        count=$(grep -c -F -- "$text" "$RUN_DIR/$file" || true)
    fi
    COUNTER_ARGS+=(--counter "$name=${count:-0}")
done < <(log_counters)

"$JAVA" -cp "$JAR" "-Dloader.main=$REPORT_MAIN" "$LAUNCHER" \
    --lab "$LAB_ID" --phase "$PHASE" --branch "$BRANCH" --run-dir "$RUN_DIR" --props "$PROPS" \
    ${COUNTER_ARGS[@]+"${COUNTER_ARGS[@]}"} || { echo "Recording the run failed" >&2; exit 1; }
