# Plan B — Lab Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Six reproduce → investigate → fix → re-measure labs, a harness that turns each run into a stored before/after comparison, participant preflight, and the lab-facing slides in the course decks.

**Architecture:** One Spring Boot app (D1). Every defect lives in its own package `net.safedata.performance.training.lab.<slug>`, behind a Spring profile `lab-<slug>` that is off by default, so `master` behaves as today. A thin driver (`scripts/lab.sh` / `scripts/lab.ps1`) starts the app with the lab's profile and JVM options, drives it with a Gatling simulation, scrapes the lab's `/lab/<slug>/stats` counters, and hands everything to a Java CLI (`LabReportMain`, run from the boot jar) that stores a `summary.json` and prints the before/after table. Slides are added by a new `scripts/deck-add-slide.sh` that duplicates an existing plain slide and sets its text, so new slides keep the deck's styling.

**Tech Stack:** Java 17 bytecode (runs on 17/21/25), Spring Boot 4.1.1, Spring Data JPA + Hibernate statistics, H2 in-memory, Gatling 3.15.1 Java DSL via `gatling-maven-plugin` 4.21.12, Jackson 3 (`tools.jackson.*`, shipped by Boot 4), bash + PowerShell 7, Google Slides API via `gslides.sh personal`.

**Spec:** `docs/superpowers/specs/2026-09-22-java-perf-labs-and-deck-refresh-design.md` — §5, §6.5 (virtual threads), §6.6, §7 in full. Deck content drafts: `docs/deck-changes/00-training-overview.md`, `03-1-profiling.md`, `06-1-heap-objects.md`.

## Global Constraints

- Compile with `<java.version>17</java.version>` (D7, §7.5). No API newer than 17 in `src/main` or `src/test`. Virtual-thread code lives outside the Maven build (Task 7).
- Participants run **Windows 10/11**; every script ships as `.sh` **and** `.ps1` with identical behaviour and exit codes (§7.4).
- Every defect sits behind a profile **disabled by default**; `master` stays clean (§7.3).
- Lab slides **describe the issue only, never the fix** (D4, §6.6).
- Duration thresholds are advisory; **counts** are what the harness asserts where deterministic (D2).
- Lab numbering on slides and in code is **1–6**, matching the Training-overview slide and deck 4.1's applied "see Lab 2" text. Mapping to spec §7.1 catalogue numbers: 1→1 retention, 2→2 N+1, 3→3 contention, 4→5 GC, 5→8 code cache, 6→9 humongous.
- Version facts used below, each checked against its source on 2026-09-29: virtual threads final JDK 21 (JEP 444); `synchronized` no longer pins virtual threads from JDK 24 (JEP 491); generational ZGC opt-in with `-XX:+ZGenerational` in 21 (JEP 439), default in 23 (JEP 474), non-generational removed in 24 (JEP 490); compact object headers product in 25 (JEP 519), experimental in 24 (JEP 450).
- Nothing is pushed, tagged on origin, or published without the trainer's explicit go-ahead.
- Deck edits: always `gslides.sh personal`; verify every applied slide by thumbnail and by `grep -c` on a fresh text dump, never by exit code alone.

## Review Focus

1. **Port 8080 already taken** (a previous run's app still alive, or the participant's IDE run). Expected: driver refuses with exit 2 and names the port — it must never measure the wrong process. Test in Task 5 Step 6.
2. **App never becomes healthy** (bad JVM flag, JDK 17 missing a flag, profile typo). Expected: driver gives up within 90 s, prints the last 40 lines of `app.log`, leaves no Java process behind, exit 3. Test in Task 5 Step 7.
3. **`verify` before any `baseline`.** Expected: the run is stored and the report says `No baseline yet for lab N on branch B — run: lab.sh N baseline`, exit 0. Test in Task 3 Step 1 (`compareWithoutBaselineSaysSo`).
4. **Branch names containing `/`** (`solution/lab-1-retention`) and repo paths containing spaces (`C:\Users\First Last\...`). Expected: results land in `results/solution_lab-1-retention/...`; driver works from a spaced path. Tests in Task 3 Step 1 (`branchWithSlashIsFlattened`) and Task 5 Step 8.
5. **Several Gatling runs in one results folder.** Expected: the parser picks the newest run directory, not an arbitrary one. Test in Task 2 Step 5 (`picksNewestRunDirectory`).

---

### Task 0: Verification toolchain (scratch only, nothing installed system-wide)

Only JDK 25.0.4 works locally (`/Volumes/NVMe/Development/jdks/jdk-25.0.4.jdk`). Labs 3–5 behave differently per JDK and the `.ps1` scripts need PowerShell to test.

- [ ] **Step 1:** Download Temurin 17 and 21 JDKs and PowerShell 7 into the session scratchpad `$SCRATCH/tools/`:

```bash
ARCH=$(uname -m | sed 's/arm64/aarch64/; s/x86_64/x64/')
for v in 17 21; do
  curl -sSfL "https://api.adoptium.net/v3/binary/latest/$v/ga/mac/$ARCH/jdk/hotspot/normal/eclipse" -o "$SCRATCH/tools/jdk$v.tar.gz"
  mkdir -p "$SCRATCH/tools/jdk$v" && tar xzf "$SCRATCH/tools/jdk$v.tar.gz" -C "$SCRATCH/tools/jdk$v" --strip-components=1
done
PW=$(curl -sSf https://api.github.com/repos/PowerShell/PowerShell/releases/latest | jq -r ".assets[].browser_download_url | select(test(\"osx-$(uname -m | sed 's/aarch64/arm64/').tar.gz$\"))")
curl -sSfL "$PW" -o "$SCRATCH/tools/pwsh.tar.gz" && mkdir -p "$SCRATCH/tools/pwsh" && tar xzf "$SCRATCH/tools/pwsh.tar.gz" -C "$SCRATCH/tools/pwsh" && chmod +x "$SCRATCH/tools/pwsh/pwsh"
```

- [ ] **Step 2:** Verify: `$SCRATCH/tools/jdk17/Contents/Home/bin/java -version` prints 17, likewise 21; `$SCRATCH/tools/pwsh/pwsh -v` prints 7.x.

---

### Task 1: Repo hygiene and the shared `lab` profile

**Files:**
- Modify: `pom.xml` (java.version 17), `.gitignore`, `README.md`, `src/main/resources/application.yml`, `src/main/java/.../config/SchedulingConfig.java`
- Create: `src/main/resources/application-lab.yml`, `src/test/java/.../lab/LabProfileTest.java`

**Interfaces — Produces:** profile `lab` (in-memory H2, scheduling off, tracing off); property `demo.scheduling.enabled` (default true); profile groups `lab-retention`, `lab-nplus1`, `lab-contention`, `lab-gc`, `lab-codecache`, `lab-humongous` → each includes `lab`.

- [ ] **Step 1: Failing test** `LabProfileTest`:

```java
@SpringBootTest
@ActiveProfiles("lab-retention")
class LabProfileTest {
    @Autowired ApplicationContext context;
    @Value("${spring.datasource.url}") String url;

    @Test
    void labProfilesUseAnInMemoryDatabaseAndNoDemoScheduling() {
        assertThat(url).startsWith("jdbc:h2:mem:");
        assertThat(context.getBeansOfType(ThreadPoolTaskScheduler.class)).isEmpty();
    }
}
```

- [ ] **Step 2:** `./mvnw -q test -Dtest=LabProfileTest` → FAIL (url is `jdbc:h2:~/test`).
- [ ] **Step 3: Implement.** `application.yml` gains:

```yaml
spring:
  profiles:
    group:
      lab-retention: lab
      lab-nplus1: lab
      lab-contention: lab
      lab-gc: lab
      lab-codecache: lab
      lab-humongous: lab
```

`application-lab.yml`:

```yaml
# Shared by every lab profile: an isolated in-memory database, and none of the demo
# background work (scheduled product generation, execution-time tracing) that would
# otherwise leak memory and add noise to every measurement.
spring:
  datasource:
    url: jdbc:h2:mem:lab;DB_CLOSE_DELAY=-1
  jpa:
    hibernate:
      ddl-auto: create-drop
demo:
  scheduling:
    enabled: false
execution:
  time:
    tracing: false
decorator.datasource.p6spy.enable-logging: false
```

`SchedulingConfig`: replace the commented annotation with `@ConditionalOnBooleanProperty(name = "demo.scheduling.enabled", matchIfMissing = true)`.
`pom.xml`: `<java.version>17</java.version>`. `.gitignore`: add `.DS_Store`, `results/`. `README.md`: drop the `-Xverify:none` advice; point to `PREREQUISITES.md` and `docs/LAB-WORKFLOW.md` (both created later — add the links now, the files land in Tasks 11–12).
- [ ] **Step 4:** `./mvnw -q verify` → all tests pass, including `LabProfileTest`; `ProfilingDemoApplicationTests` still loads the default context. Compile errors from `--release 17` are fixed in place (report each one).
- [ ] **Step 5: Commit** `[improve] Target Java 17 and add the shared lab profile`.

---

### Task 2: Gatling harness and `stats.json` parser (replaces the PDF parser)

**Files:**
- Modify: `pom.xml` (add `io.gatling.highcharts:gatling-charts-highcharts:3.15.1` test scope; plugin `io.gatling:gatling-maven-plugin:4.21.12`; remove `pdfbox`), `gatling/GatlingReportAnalyzer.java`, `gatling/GatlingNFRAnalyzerExample.java`
- Create: `gatling/parser/GatlingStatsParser.java`, `src/test/java/.../gatling/parser/GatlingStatsParserTest.java`, `src/test/resources/gatling/sample-run/js/stats.json` (captured, not hand-written), `src/test/java/.../lab/simulations/SmokeSimulation.java`
- Delete: `gatling/parser/GatlingPdfParser.java`, `GatlingPdfParserTest.java`

**Interfaces — Produces:** `GatlingStatsParser#parse(Path runDir): GatlingReport`; `GatlingStatsParser#newestRunDir(Path resultsFolder): Path`; `GatlingReportAnalyzer#analyze(Path runDir, NFRConfig)`. The global row is an `EndpointStats` named `All Requests`; percentiles map keys are `50, 75, 95, 99`.
**Consumes:** existing `GatlingReport(RunInfo, List<EndpointStats>)`, `EndpointStats(name, totalRequests, okCount, koCount, errorPercent, requestsPerSecond, Map<Integer,Integer> percentiles)`, `RunInfo(simulationName, LocalDateTime date, String duration, String description)`.

- [ ] **Step 1:** Add the dependency and plugin. Confirm the plugin's results-folder property name with `./mvnw help:describe -Dplugin=io.gatling:gatling-maven-plugin -Ddetail | grep -i -A2 resultsFolder` and record it (expected `gatling.resultsFolder`).
- [ ] **Step 2:** `SmokeSimulation` (hits `/actuator/health` 20 times, base URL from env `LAB_BASE_URL`, default `http://localhost:8080`):

```java
public class SmokeSimulation extends Simulation {
    private static final String BASE_URL = System.getenv().getOrDefault("LAB_BASE_URL", "http://localhost:8080");
    {
        setUp(scenario("smoke").exec(http("health").get("/actuator/health"))
                .injectOpen(atOnceUsers(20)))
                .protocols(http.baseUrl(BASE_URL));
    }
}
```

Run the app (`java -jar target/*.jar --spring.profiles.active=lab-retention &`), then `./mvnw -q gatling:test -Dgatling.simulationClass=...SmokeSimulation -Dgatling.resultsFolder=$SCRATCH/gatling`. Copy the produced `js/stats.json` to `src/test/resources/gatling/sample-run/js/stats.json`. **Read its real schema before writing the parser** — expected: a root `GROUP` "All Requests" with `stats.numberOfRequests.{total,ok,ko}`, `stats.percentiles1..4.total` (50/75/95/99), `stats.meanNumberOfRequestsPerSecond.total`, and per-request entries under `contents`; values can be the string `"-"`. If the schema differs, the parser follows the file, and this plan's field names are corrected in the commit message.
- [ ] **Step 3: Failing tests:**

```java
class GatlingStatsParserTest {
    private final GatlingStatsParser parser = new GatlingStatsParser();

    @Test
    void parsesGlobalAndPerRequestStats() throws Exception {
        GatlingReport report = parser.parse(Path.of("src/test/resources/gatling/sample-run"));
        EndpointStats all = report.getEndpoint("All Requests").orElseThrow();
        assertThat(all.totalRequests()).isEqualTo(20);
        assertThat(all.okCount() + all.koCount()).isEqualTo(20);
        assertThat(all.percentiles()).containsKeys(50, 75, 95, 99);
        assertThat(report.getEndpoint("health")).isPresent();
    }

    @Test
    void dashValuesBecomeZero(@TempDir Path run) throws Exception {
        Path fixture = Path.of("src/test/resources/gatling/sample-run/js/stats.json");
        String json = Files.readString(fixture).replaceFirst("\"ko\"\\s*:\\s*\\d+", "\"ko\": \"-\"");
        Files.createDirectories(run.resolve("js"));
        Files.writeString(run.resolve("js/stats.json"), json);
        assertThat(parser.parse(run).getEndpoint("All Requests").orElseThrow().koCount()).isZero();
    }

    @Test
    void picksNewestRunDirectory(@TempDir Path results) throws Exception {
        Files.createDirectories(results.resolve("smokesimulation-20260929080000000/js"));
        Files.createDirectories(results.resolve("smokesimulation-20260929090000000/js"));
        assertThat(GatlingStatsParser.newestRunDir(results).getFileName().toString())
                .isEqualTo("smokesimulation-20260929090000000");
    }
}
```

- [ ] **Step 4:** Run → FAIL (class missing).
- [ ] **Step 5: Implement** `GatlingStatsParser` with Jackson 3 (`tools.jackson.databind.json.JsonMapper`; confirm with `./mvnw dependency:tree | grep jackson`): read `runDir/js/stats.json`, map root + each `contents` entry of `type == REQUEST` to `EndpointStats`; `errorPercent = ko*100/total`; `RunInfo.simulationName` and `date` from the directory name `<name>-<yyyyMMddHHmmssSSS>`; `newestRunDir` = lexically greatest subdirectory that contains `js/stats.json`. Switch `GatlingReportAnalyzer` and the example to it; delete the PDF parser, its test and `pdfbox`.
- [ ] **Step 6:** `./mvnw -q verify` → PASS (existing 13 analyzer test classes untouched and green).
- [ ] **Step 7: Commit** `[improve] Read Gatling results from stats.json instead of the PDF report`.

---

### Task 3: Lab results store and before/after report

**Files:** Create in `src/main/java/.../lab/harness/`: `RunMetrics.java`, `LabRunSummary.java`, `CountThreshold.java`, `LabResultsStore.java`, `LabComparison.java`, `LabReportMain.java`; tests `src/test/java/.../lab/harness/LabResultsStoreTest.java`, `LabComparisonTest.java`.

**Interfaces — Produces:**

```java
public record RunMetrics(double p50Ms, double p95Ms, double p99Ms, double requestsPerSecond, double errorPercent) {
    public static RunMetrics from(EndpointStats all);
    public static RunMetrics median(List<RunMetrics> runs);   // per-field median
}
public record LabRunSummary(String lab, String phase, String branch, String timestamp, String javaVersion,
                            String jvmOptions, List<RunMetrics> runs, Map<String, Long> counters) {
    public RunMetrics median();
}
public record CountThreshold(String counter, long max) {
    public boolean passes(Map<String, Long> counters);          // missing counter => fails
    public static List<CountThreshold> fromProperties(Properties p); // keys threshold.count.<counter>=<max>
}
public final class LabResultsStore {
    public LabResultsStore(Path resultsRoot);
    public static String safeBranch(String branch);            // [^A-Za-z0-9._-] -> _
    public Path newRunDir(String branch, String lab, String phase, LocalDateTime now); // <root>/<branch>/<lab>/<yyyyMMdd-HHmmss>-<phase>
    public void write(Path runDir, LabRunSummary summary);     // runDir/summary.json
    public Optional<LabRunSummary> latest(String branch, String lab, String phase);
}
public final class LabComparison {
    public static String render(Optional<LabRunSummary> baseline, LabRunSummary current, List<CountThreshold> thresholds);
}
// LabReportMain: record --lab L --phase P --branch B --run-dir D --props F [--counter k=v]...
//                 reads D/gatling-*/<newest>/js/stats.json and D/stats.json (flat JSON object of numbers, optional),
//                 writes D/summary.json, prints LabComparison.render(latest baseline, this run, thresholds),
//                 exit 0 always unless arguments are invalid (exit 64).
```

- [ ] **Step 1: Failing tests:**

```java
class LabResultsStoreTest {
    @TempDir Path root;

    @Test
    void branchWithSlashIsFlattened() {
        assertThat(LabResultsStore.safeBranch("solution/lab-1-retention")).isEqualTo("solution_lab-1-retention");
    }

    @Test
    void latestReturnsNewestRunOfThatPhase() {
        LabResultsStore store = new LabResultsStore(root);
        Path older = store.newRunDir("me", "1-retention", "baseline", LocalDateTime.of(2026, 9, 29, 9, 0));
        Path newer = store.newRunDir("me", "1-retention", "baseline", LocalDateTime.of(2026, 9, 29, 10, 0));
        store.write(older, summary("baseline", 100));
        store.write(newer, summary("baseline", 200));
        assertThat(store.latest("me", "1-retention", "baseline").orElseThrow().counters()).containsEntry("retainedQuotes", 200L);
    }

    private static LabRunSummary summary(String phase, long retained) {
        return new LabRunSummary("1-retention", phase, "me", "t", "21", "-Xmx256m",
                List.of(new RunMetrics(1, 2, 3, 100, 0)), Map.of("retainedQuotes", retained));
    }
}

class LabComparisonTest {
    @Test
    void compareWithoutBaselineSaysSo() {
        String out = LabComparison.render(Optional.empty(), verifyRun(10), List.of());
        assertThat(out).contains("No baseline yet for lab 1-retention on branch me");
    }

    @Test
    void showsBeforeAfterAndThresholdVerdict() {
        String out = LabComparison.render(Optional.of(baselineRun(12000)), verifyRun(1000),
                List.of(new CountThreshold("retainedQuotes", 1000)));
        assertThat(out).contains("retainedQuotes").contains("12000").contains("1000").contains("PASS");
    }

    private static LabRunSummary baselineRun(long retained) { return run("baseline", retained); }
    private static LabRunSummary verifyRun(long retained) { return run("verify", retained); }

    private static LabRunSummary run(String phase, long retained) {
        return new LabRunSummary("1-retention", phase, "me", "t", "21", "-Xmx256m",
                List.of(new RunMetrics(1, 2, 3, 100, 0)), Map.of("retainedQuotes", retained));
    }
}
```

- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement (Jackson 3 `JsonMapper` for summary.json; table rendered with `String.format` columns `metric | baseline | now | limit | verdict`, latency/throughput rows first, counters after, a `PASS`/`FAIL` verdict only on rows that have a threshold). **Step 4:** `./mvnw -q verify` → PASS. **Step 5: Commit** `[feature] Store lab runs and print before/after comparisons`.

---

### Task 4: Lab 1 — unbounded retention

**Files:** Create `lab/retention/{Quote,QuoteResponse,QuoteService,QuoteController}.java`, `lab/common/LabMemory.java`, `src/test/.../lab/simulations/RetentionSimulation.java`, `src/test/.../lab/retention/RetentionLabTest.java`, `labs/1-retention/lab.properties`, `docs/labs/lab-1-retention.md`.

**Interfaces — Produces:** `GET /lab/retention/quote/{sku}` → `QuoteResponse(id, sku, price)`; `GET /lab/retention/stats` → `{"retainedQuotes": n, "heapUsedAfterGcMb": m}`; `LabMemory.heapUsedAfterGcMb(): long`.

- [ ] **Step 1: Failing test:**

```java
@SpringBootTest
@ActiveProfiles("lab-retention")
class RetentionLabTest {
    @Autowired QuoteController controller;

    @Test
    void defectRetainsEveryQuote() {
        IntStream.range(0, 50).forEach(i -> controller.quote("SKU-" + i));
        assertThat(controller.stats()).containsEntry("retainedQuotes", 50L);
    }
}
```

- [ ] **Step 2:** Run → FAIL. **Step 3: Implement:**

```java
public record Quote(String id, String sku, double price, Instant createdAt, byte[] pricingSnapshot) {}
public record QuoteResponse(String id, String sku, double price) {}

@Service
@Profile("lab-retention")
public class QuoteService {
    private static final int SNAPSHOT_BYTES = 8 * 1024;

    // Every quote is kept, so a disputed price can be traced back to its exact pricing input.
    private final Map<String, Quote> auditTrail = new ConcurrentHashMap<>();

    public Quote quote(String sku) {
        Quote quote = new Quote(UUID.randomUUID().toString(), sku, priceFor(sku), Instant.now(), new byte[SNAPSHOT_BYTES]);
        auditTrail.put(quote.id(), quote);
        return quote;
    }

    public int auditedQuotes() {
        return auditTrail.size();
    }

    private static double priceFor(String sku) {
        return 10 + Math.floorMod(sku.hashCode(), 990);
    }
}

@RestController
@Profile("lab-retention")
@RequestMapping("/lab/retention")
public class QuoteController {
    private final QuoteService quoteService;
    public QuoteController(QuoteService quoteService) { this.quoteService = quoteService; }

    @GetMapping("/quote/{sku}")
    public QuoteResponse quote(@PathVariable String sku) {
        Quote quote = quoteService.quote(sku);
        return new QuoteResponse(quote.id(), quote.sku(), quote.price());
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return Map.of("retainedQuotes", (long) quoteService.auditedQuotes(),
                      "heapUsedAfterGcMb", LabMemory.heapUsedAfterGcMb());
    }
}

public final class LabMemory {
    private LabMemory() {}
    /** Heap in use after an explicit full GC — the retained set, not the garbage. */
    public static long heapUsedAfterGcMb() {
        System.gc();
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }
}
```

`RetentionSimulation`: open model, `constantUsersPerSec(200).during(LAB_DURATION_SECONDS)` (env, default 60) on `/lab/retention/quote/#{sku}` with a feeder of `SKU-0..499`, base URL from `LAB_BASE_URL`.
`labs/1-retention/lab.properties`:

```properties
lab=1-retention
profile=lab-retention
jvm.options=-Xms256m -Xmx256m -XX:+UseG1GC
simulation=net.safedata.performance.training.lab.simulations.RetentionSimulation
stats.path=/lab/retention/stats
threshold.count.retainedQuotes=1000
```

`docs/labs/lab-1-retention.md`: symptom, how to run (`lab.sh 1 baseline` / `verify`), tools (`jcmd <pid> GC.heap_dump`, Eclipse MAT dominator tree, JMC), what to measure. **No fix.**
- [ ] **Step 4:** `./mvnw -q verify` → PASS. **Step 5: Commit** `[feature] Lab 1: unbounded retention in the quote audit trail`.

---

### Task 5: Lab drivers — `lab.sh` and `lab.ps1`

**Files:** Create `scripts/lab.sh`, `scripts/lab.ps1`.

**Interfaces — Consumes:** `labs/<n>-<slug>/lab.properties` keys `lab, profile, jvm.options, simulation, stats.path` (may be empty), optional `log.counter.<name>=<app.log|gc.log>:<fixed text>`, `threshold.count.*`; `LabReportMain` CLI (Task 3). **Produces:** `lab.{sh,ps1} <n|slug> <baseline|verify> [--runs N (default 3)] [--gc serial|g1|zgc] [--seconds S (default 60)]`. Exit codes: 0 ok, 2 port busy, 3 app not healthy, 64 usage.

- [ ] **Step 1:** Write `lab.sh`. Behaviour, in order:
  1. Resolve lab dir by number or slug (`labs/<n>-*` or `labs/*-<slug>`); usage error → 64.
  2. `PORT=8080`; if `curl -s -o /dev/null http://localhost:$PORT` connects → print `Port 8080 is already in use — stop the other process first` → exit 2.
  3. Build if `target/*.jar` is missing or older than any file under `src/`: `./mvnw -q -DskipTests package`.
  4. Java major version from `java -XshowSettings:properties -version` (`java.specification.version`). `--gc` strips any `-XX:+Use*GC` from `jvm.options` and appends `-XX:+UseSerialGC` / `-XX:+UseG1GC` / `-XX:+UseZGC` (plus `-XX:+ZGenerational` when major == 21; on 17 print `ZGC on 17 is non-generational`).
  5. `RUN_DIR` from `LabResultsStore` layout: `results/<safe-branch>/<lab>/<yyyyMMdd-HHmmss>-<phase>`; branch from `git rev-parse --abbrev-ref HEAD`, flattened with `tr -c 'A-Za-z0-9._-\n' '_'`.
  6. Start `java $JVM_OPTS -Xlog:gc*:file="$RUN_DIR/gc.log":uptime,level,tags -jar "$JAR" --spring.profiles.active=$PROFILE > "$RUN_DIR/app.log" 2>&1 &`; `trap` kills it on any exit.
  7. Poll `/actuator/health` for `"status":"UP"` once a second, 90 s max; if the process dies or time runs out → `tail -40 app.log`, kill, exit 3.
  8. For `i in 1..runs`: `LAB_BASE_URL=... LAB_DURATION_SECONDS=$SECONDS ./mvnw -q gatling:test -Dgatling.simulationClass=$SIM -Dgatling.resultsFolder="$RUN_DIR/gatling-$i"`.
  9. If `stats.path` non-empty: `curl -fs` it → `$RUN_DIR/stats.json`. For each `log.counter.*`: `grep -c -F` the text in the named file → `--counter name=N`.
  10. Stop the app, then `java -cp "$JAR" -Dloader.main=net.safedata.performance.training.lab.harness.LabReportMain org.springframework.boot.loader.launch.PropertiesLauncher record ...`.
- [ ] **Step 2:** Write `lab.ps1` with the same steps and exit codes (`Test-NetConnection` is Windows-only: use `[System.Net.Sockets.TcpClient]` for the port check; `Start-Process -PassThru -RedirectStandardOutput`; `Invoke-RestMethod` for health; `.\mvnw.cmd` on Windows, `./mvnw` elsewhere).
- [ ] **Step 3:** `bash -n scripts/lab.sh`; `$SCRATCH/tools/pwsh/pwsh -NoProfile -Command "[ScriptBlock]::Create((Get-Content -Raw scripts/lab.ps1)) | Out-Null"` → both parse.
- [ ] **Step 4:** `./scripts/lab.sh 1 baseline --runs 1 --seconds 30` → prints a table with `retainedQuotes` ≥ 5000 and `FAIL` against the 1000 limit; `results/<branch>/1-retention/*-baseline/summary.json` exists.
- [ ] **Step 5:** Same through `pwsh scripts/lab.ps1 1 verify --runs 1 --seconds 30` → table shows baseline and now side by side.
- [ ] **Step 6 (Review Focus 1):** `python3 -m http.server 8080 &` → `./scripts/lab.sh 1 baseline` exits 2 with the port message; repeat with `lab.ps1`; kill the server.
- [ ] **Step 7 (Review Focus 2):** `LAB_EXTRA_JVM_OPTS=-XX:+NoSuchFlag ./scripts/lab.sh 1 baseline` (the driver appends `$LAB_EXTRA_JVM_OPTS`) → exit 3 within 90 s, app log tail shown, `pgrep -f java-performance-training` empty afterwards. Same for `lab.ps1`.
- [ ] **Step 8 (Review Focus 4):** `ln -s "$PWD" "$SCRATCH/lab repo" && cd "$SCRATCH/lab repo" && ./scripts/lab.sh 1 baseline --runs 1 --seconds 10` → exit 0. Same for `lab.ps1`.
- [ ] **Step 9: Commit** `[feature] Add the lab.sh / lab.ps1 lab drivers`.

---

### Task 6: Lab 2 — N+1 queries

**Files:** Create `lab/nplus1/{StoreEntity,SectionEntity,ItemEntity,StoreRepository,StoreSummary,StoreSummaryService,StoreDataSeeder,StoreSummaryController}.java`, `src/main/resources/application-lab-nplus1.yml`, `src/test/.../lab/nplus1/NPlusOneLabTest.java`, `src/test/.../lab/simulations/NPlusOneSimulation.java`, `labs/2-nplus1/lab.properties`, `docs/labs/lab-2-nplus1.md`.

**Interfaces — Produces:** `GET /lab/nplus1/stores` → `List<StoreSummary(store, sections, items, stockValue)>`; `GET /lab/nplus1/stats` → `{"sqlStatementsPerRequest": n}` (clears Hibernate statistics, serves one request, returns `getPrepareStatementCount()`).

- [ ] **Step 1: Failing test:**

```java
@SpringBootTest
@ActiveProfiles("lab-nplus1")
class NPlusOneLabTest {
    @Autowired StoreSummaryController controller;

    @Test
    void defectIssuesOneQueryPerStoreAndPerSection() {
        assertThat(controller.stores()).hasSize(20).allSatisfy(s -> assertThat(s.items()).isEqualTo(50));
        // 1 (stores) + 20 (sections of each store) + 100 (items of each section)
        assertThat(controller.stats()).containsEntry("sqlStatementsPerRequest", 121L);
    }
}
```

- [ ] **Step 2:** Run → FAIL. **Step 3: Implement.** Entities (`lab_store`, `lab_section`, `lab_item`; `@OneToMany(mappedBy=...)` lists, `@ManyToOne(fetch = LAZY)`, assigned ids, protected no-arg constructors). Seeder: `ApplicationRunner` in a `TransactionTemplate`, 20 stores × 5 sections × 10 items, prices `1 + (id % 97)`. Service (the defect):

```java
@Transactional(readOnly = true)
public List<StoreSummary> summaries() {
    return storeRepository.findAll().stream()
            .map(store -> {
                long items = 0;
                double stockValue = 0;
                for (SectionEntity section : store.getSections()) {
                    for (ItemEntity item : section.getItems()) {
                        items++;
                        stockValue += item.getPrice();
                    }
                }
                return new StoreSummary(store.getName(), store.getSections().size(), items, stockValue);
            })
            .toList();
}
```

`application-lab-nplus1.yml`: `spring.jpa.properties.hibernate.generate_statistics: true` and `decorator.datasource.p6spy.enable-logging: true` (participants read the statement log). Simulation: closed model, `constantConcurrentUsers(10)` on `/lab/nplus1/stores`. `lab.properties`: `threshold.count.sqlStatementsPerRequest=3`, `jvm.options=-Xms512m -Xmx512m -XX:+UseG1GC`. Participant sheet: tools = p6spy log, Hibernate statistics, JFR JDBC events; **no fix**.
- [ ] **Step 4:** `./mvnw -q verify` → PASS; `./scripts/lab.sh 2 baseline --runs 1 --seconds 20` → `sqlStatementsPerRequest 121  FAIL`.
- [ ] **Step 5: Commit** `[feature] Lab 2: N+1 queries on the store summary`.

---

### Task 7: Lab 3 — lock contention, plus the virtual-threads example

**Files:** Create `lab/contention/{ExchangeRateService,ExchangeController}.java`, `src/test/.../lab/contention/ContentionLabTest.java`, `src/test/.../lab/simulations/ContentionSimulation.java`, `labs/3-contention/lab.properties`, `labs/3-contention/VirtualThreadsDemo.java`, `docs/labs/lab-3-contention.md`.

**Interfaces — Produces:** `GET /lab/contention/convert/{currency}?amount=` → `{"amount": ...}`; `GET /lab/contention/stats` → `{"conversions": n, "httpThreadsBlockedCount": m}` (sum of `ThreadInfo.getBlockedCount()` over threads named `http-nio-*`).

- [ ] **Step 1: Failing test** (a lower bound, so it cannot flake):

```java
@SpringBootTest
@ActiveProfiles("lab-contention")
class ContentionLabTest {
    @Autowired ExchangeRateService service;

    @Test
    void defectSerialisesEveryConversion() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);   // no try-with-resources: ExecutorService is AutoCloseable only from 19
        long start = System.nanoTime();
        for (int t = 0; t < 8; t++) {
            pool.submit(() -> IntStream.range(0, 5).forEach(i -> service.convert("EUR", BigDecimal.TEN)));
        }
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        // 40 conversions x 10 ms compliance check, serialised by the lock: never faster than 400 ms
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(400));
    }
}
```
- [ ] **Step 2:** Run → FAIL. **Step 3: Implement:**

```java
@Service
@Profile("lab-contention")
public class ExchangeRateService {
    private static final long COMPLIANCE_CHECK_NANOS = Duration.ofMillis(10).toNanos();

    private final Map<String, BigDecimal> rates = new HashMap<>();
    private final LongAdder conversions = new LongAdder();

    // One lock keeps the rate cache consistent.
    public synchronized BigDecimal convert(String currency, BigDecimal amount) {
        BigDecimal rate = rates.computeIfAbsent(currency, ExchangeRateService::loadRate);
        complianceCheck();
        conversions.increment();
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_EVEN);
    }

    public long conversions() {
        return conversions.sum();
    }

    private static BigDecimal loadRate(String currency) {
        return BigDecimal.valueOf(1 + Math.floorMod(currency.hashCode(), 400) / 100.0);
    }

    // Stands in for a remote compliance service: blocks, burns no CPU.
    private static void complianceCheck() {
        LockSupport.parkNanos(COMPLIANCE_CHECK_NANOS);
    }
}
```

Simulation: closed model `constantConcurrentUsers(20)` (Tomcat `threads.max` is 20). `lab.properties`: `jvm.options=-Xms256m -Xmx256m -XX:+UseG1GC`, no count threshold (latency/throughput shown; the participant evidence is JFR *Java Monitor Blocked*).
`VirtualThreadsDemo.java` — a single-file program (`java labs/3-contention/VirtualThreadsDemo.java`, JDK 21+), outside the Maven build so the project keeps `--release 17`:

```java
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Same blocking workload three ways. Run on JDK 21, then on 25:
 * the third line is slow on 21 (a virtual thread blocking inside synchronized pins its carrier)
 * and fast on 24+ (JEP 491 removed that pinning).
 */
public class VirtualThreadsDemo {
    private static final int TASKS = 2_000;
    private static final Duration BLOCKING_CALL = Duration.ofMillis(50);

    public static void main(String[] args) throws Exception {
        System.out.println("JDK " + Runtime.version() + ", " + Runtime.getRuntime().availableProcessors() + " CPUs");
        run("fixed pool of 50 platform threads", Executors.newFixedThreadPool(50), false);
        run("one virtual thread per task", Executors.newVirtualThreadPerTaskExecutor(), false);
        run("virtual threads, blocking inside synchronized", Executors.newVirtualThreadPerTaskExecutor(), true);
    }

    private static void run(String label, ExecutorService executor, boolean insideSynchronized) {
        long start = System.nanoTime();
        try (executor) {
            for (int i = 0; i < TASKS; i++) {
                Object lock = new Object();  // one lock per task: no contention, only pinning
                executor.submit(() -> {
                    if (insideSynchronized) {
                        synchronized (lock) {
                            block();
                        }
                    } else {
                        block();
                    }
                });
            }
        }
        System.out.printf("%-50s %6d ms%n", label, Duration.ofNanos(System.nanoTime() - start).toMillis());
    }

    private static void block() {
        try {
            Thread.sleep(BLOCKING_CALL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

- [ ] **Step 4:** `./mvnw -q verify` → PASS. Run the demo on `$SCRATCH/tools/jdk21` and on JDK 25; record both outputs in `docs/labs/lab-3-contention.md` (trainer notes section). Expected: line 3 ≫ line 2 on 21, line 3 ≈ line 2 on 25. If not, the doc states what was measured instead.
- [ ] **Step 5:** `./scripts/lab.sh 3 baseline --runs 1 --seconds 20` → rps near 100 (20 threads, 10 ms, serialised).
- [ ] **Step 6: Commit** `[feature] Lab 3: lock contention, and the virtual-threads example`.

---

### Task 8: Lab 4 — GC mismatch

**Files:** Create `lab/gc/{ReportService,ReportController}.java`, `lab/common/GcCounters.java`, tests `lab/gc/GcLabTest.java`, `lab/common/GcCountersTest.java`, simulation `GcSimulation.java`, `labs/4-gc/lab.properties`, `docs/labs/lab-4-gc.md`.

**Interfaces — Produces:** `GET /lab/gc/report` → `{"rows": n, "bytes": b}`; `GET /lab/gc/stats` → `GcCounters.snapshot()`: for each `GarbageCollectorMXBean`, keys `gc.<bean name lowercased, non-alphanumerics → _>.count` and `.timeMs`.

- [ ] **Step 1: Failing tests:**

```java
class GcCountersTest {
    @Test
    void oneCountAndOneTimePerCollector() {
        Map<String, Long> snapshot = GcCounters.snapshot();
        assertThat(snapshot).isNotEmpty();
        assertThat(snapshot.keySet()).allMatch(k -> k.matches("gc\\.[a-z0-9_]+\\.(count|timeMs)"));
    }
}

@SpringBootTest
@ActiveProfiles("lab-gc")
class GcLabTest {
    @Autowired ReportService service;

    @Test
    void keepsTheLast400Reports() {
        assertThat(service.report().rows()).isEqualTo(5_000);
        IntStream.range(0, 499).forEach(i -> service.report());
        assertThat(service.retainedReports()).isEqualTo(400);
    }
}
```
- [ ] **Step 2:** Run → FAIL. **Step 3: Implement** `ReportService#report()`: build 5 000 `Row(int, String, double)` (short-lived), join to CSV, keep the bytes in a synchronized `ArrayDeque` capped at 400 (medium-lived — survives young collections, ~40 MB live). `lab.properties`: `jvm.options=-Xms256m -Xmx256m -XX:+UseSerialGC` (today's `vm-options.txt` default), no count threshold — the participant compares collectors via `--gc`.
- [ ] **Step 4: Calibrate** on JDK 21 and 25: `lab.sh 4 baseline --gc serial`, `--gc g1`, `--gc zgc`, `--runs 1 --seconds 60` each. Acceptance: Serial's p99 is at least 2× G1's. If not, raise the retained window or row count and re-run; record the final numbers per JDK in the trainer section of `docs/labs/lab-4-gc.md`.
- [ ] **Step 5: Commit** `[feature] Lab 4: one workload across Serial, G1 and ZGC`.

---

### Task 9: Lab 5 — code cache exhaustion

**Files:** Create `lab/codecache/{WorkloadService,WorkloadController}.java`, `lab/common/CodeCacheCounters.java`, tests, `CodeCacheSimulation.java`, `labs/5-codecache/lab.properties`, `docs/labs/lab-5-codecache.md`.

**Interfaces — Produces:** `GET /lab/codecache/work` (CPU-bound, deliberately wide: Jackson round-trip of a varied object graph, `java.time` formatting in 10 patterns, regex, `BigDecimal` arithmetic, `Base64`, `Deflater`, `MessageDigest`, sorting with composed comparators); `GET /lab/codecache/stats` → `codeCacheUsedKb`, `codeCacheMaxKb` (sum over non-heap pools whose name starts with `CodeHeap` or equals `CodeCache`), `compilationTimeMs`. Log counter `codeCacheFullWarnings=app.log:CodeCache is full`.

- [ ] **Step 1: Failing tests:**

```java
class CodeCacheCountersTest {
    @Test
    void reportsCodeCacheUse() {
        Map<String, Long> snapshot = CodeCacheCounters.snapshot();
        assertThat(snapshot.get("codeCacheMaxKb")).isPositive();
        assertThat(snapshot.get("codeCacheUsedKb")).isPositive();
        assertThat(snapshot).containsKey("compilationTimeMs");
    }
}

class WorkloadServiceTest {
    @Test
    void sameSeedSameDigest() {
        WorkloadService service = new WorkloadService();
        assertThat(service.work(42)).isNotBlank().isEqualTo(service.work(42));
    }
}
```

(`WorkloadService#work(long seed): String` returns a hex SHA-256 over everything the workload produced; the controller passes `ThreadLocalRandom.current().nextLong()`.)
- [ ] **Step 2–3:** Run → FAIL; implement.
- [ ] **Step 4: Calibrate** on JDK 21 and 25. Start at `-XX:ReservedCodeCacheSize=16m`; find the largest size where `codeCacheFullWarnings ≥ 1` within 60 s **and** throughput is ≤ 50 % of the same run at `-XX:ReservedCodeCacheSize=240m`. If the JVM refuses a size, the next larger one is the floor. Write the chosen value into `lab.properties` (`jvm.options=-Xms512m -Xmx512m -XX:+UseG1GC -XX:ReservedCodeCacheSize=<calibrated>`), `threshold.count.codeCacheFullWarnings=0`, and the measured before/after into the trainer section of the sheet. If no size reproduces the cliff on a JDK, say so in the sheet — do not ship a lab that does not reproduce.
- [ ] **Step 5: Commit** `[feature] Lab 5: code cache exhaustion`.

---

### Task 10: Lab 6 — humongous allocations (local, no container)

**Files:** Create `HumongousSimulation.java`, `labs/6-humongous/lab.properties`, `docs/labs/lab-6-humongous.md`.

Uses the existing `GET /product/humongous` (6 MB `byte[]`). §7.1 note: G1 region size is heap/2048, clamped to 1–32 MB, so `-Xmx1g` gives 1 MB regions — the same as the 2 GB production pod — with no container needed. The Kubernetes variant stays in `docs/k8s-lab-playbook.md`.

- [ ] **Step 1:** `lab.properties`:

```properties
lab=6-humongous
profile=lab-humongous
jvm.options=-Xms1g -Xmx1g -XX:+UseG1GC
simulation=net.safedata.performance.training.lab.simulations.HumongousSimulation
stats.path=
log.counter.humongousGcEvents=gc.log:G1 Humongous Allocation
threshold.count.humongousGcEvents=5
```

Simulation: open model `constantUsersPerSec(40)`.
- [ ] **Step 2:** `./scripts/lab.sh 6 baseline --runs 1 --seconds 30` → `humongousGcEvents` well above 5, FAIL. Then a manual check that the lab is fixable: `LAB_EXTRA_JVM_OPTS=-XX:G1HeapRegionSize=16m ./scripts/lab.sh 6 verify --runs 1 --seconds 30` → count ≤ 5, PASS. Record both in the trainer section.
- [ ] **Step 3: Commit** `[feature] Lab 6: humongous allocations without a container`.

---

### Task 11: Preflight, prerequisites and the troubleshooting map

**Files:** Create `scripts/preflight.sh`, `scripts/preflight.ps1`, `PREREQUISITES.md`, `docs/PREFLIGHT-TROUBLESHOOTING.md`.

Checks, each printing `[ OK ]`, `[WARN]` or `[FAIL]` + `PF-nn` + a one-line remedy pointer `docs/PREFLIGHT-TROUBLESHOOTING.md#pf-nn`; exit 1 if any FAIL:

| Code | Check | Level |
|---|---|---|
| PF-01 | `java` on PATH | FAIL |
| PF-02 | `java` is 17 or newer | FAIL |
| PF-03 | a JDK 21 is installed (`JAVA21_HOME`, `/usr/libexec/java_home -v 21`, `C:\Program Files\*\jdk-21*`) | FAIL |
| PF-04 | a JDK 17 is installed (same search) | FAIL |
| PF-05 | a JDK 25 is installed | WARN |
| PF-06 | `./mvnw -v` resolves Maven | FAIL |
| PF-07 | dependencies and the Gatling plugin prefetched: `./mvnw -q -DskipTests test-compile` | FAIL |
| PF-08 | port 8080 free | FAIL |
| PF-09 | `jmc` on PATH | WARN |
| PF-10 | ≥ 5 GB free disk in the repo's volume (heap dumps) | FAIL |
| PF-11 | `git` on PATH | FAIL |

- [ ] **Step 1:** Write both scripts. **Step 2:** Run `preflight.sh` and `pwsh preflight.ps1` here; expected FAIL on PF-04 until `JAVA17_HOME=$SCRATCH/tools/jdk17/Contents/Home` is exported, then all OK except PF-09 WARN. **Step 3:** Review Focus 1 again: with `python3 -m http.server 8080` running, PF-08 is FAIL in both. **Step 4:** Every PF code has a section in the troubleshooting doc with a Windows remedy first (winget / Temurin MSI / freeing a port with `netstat -ano | findstr :8080`). `PREREQUISITES.md` ≤ 30 lines. **Step 5: Commit** `[feature] Add participant preflight and its troubleshooting map`.

---

### Task 12: Branch workflow, solution branches, retirement

**Files:** Create `docs/LAB-WORKFLOW.md`. Delete `scripts/load-test.sh`, `vm-options.txt` (spec §7.2, §7.5; references checked 2026-09-29: only the spec mentions them).

- [ ] **Step 1:** `docs/LAB-WORKFLOW.md` — the three commands of spec §7.3, using tag `labs-2026-10` and branches `solution/lab-<n>-<slug>`.
- [ ] **Step 2:** For each lab create a local branch `solution/lab-<n>-<slug>` from `master` with the reference fix and its test flipped to the fixed expectation:
  - 1: bounded audit window (`LinkedHashMap` with `removeEldestEntry` > 1000, synchronized) → `retainedQuotes ≤ 1000`.
  - 2: aggregate JPQL projection `select new ...StoreSummary(s.name, count(distinct sec), count(i), coalesce(sum(i.price), 0)) from StoreEntity s left join s.sections sec left join sec.items i group by s.id, s.name` → 1 statement.
  - 3: `ConcurrentHashMap.computeIfAbsent` for the cache, compliance check outside any lock → elapsed ≤ 250 ms in the test.
  - 4: `lab.properties` switched to the collector the calibration favoured, with the reasoning in the commit message.
  - 5: `ReservedCodeCacheSize` raised to 240m.
  - 6: `-XX:G1HeapRegionSize=16m`, plus an alternative commit that streams the payload with `StreamingResponseBody` instead of materialising 6 MB.
  Each branch: `./mvnw -q verify` passes and `lab.sh <n> verify --runs 1 --seconds 30` prints PASS.
- [ ] **Step 3:** Local tag `labs-2026-10` on `master`. **Do not push branches or tag** — list them for the trainer.
- [ ] **Step 4: Commit** `[docs] Document the lab branch workflow; retire load-test.sh and vm-options.txt`.

---

### Task 13: `deck-add-slide.sh` — scripted new slides that keep the deck's styling

**Files:** Create `scripts/deck-add-slide.sh`.

**Interfaces — Produces:** `deck-add-slide.sh <DECK_ID> --template-title "<title of a plain title+body slide>" --after-title "<title>" --title "<new title>" --body-file <file>`. Exit 0 = added and verified; 3 = a slide with that title already exists (idempotent re-runs); 4 = template or anchor title not found or not unique.

- [ ] **Step 1:** Implement: dump `gslides.sh personal text`, map each `=== Slide N [objectId] ===` to its first non-empty line (the title); refuse on 0 or >1 matches; `duplicate` the template; read the new slide with `gslides.sh personal slide`; `set-text` its `TITLE` and `BODY` placeholders (body lines = file lines); delete every other page element on the new slide; move it with `batch updateSlidesPosition` to the index after the anchor slide; re-dump and assert the new title occurs once and each body line occurs ≥ 1.
- [ ] **Step 2:** Test on a scratch copy: `gdrive.sh personal copy 1sagmLntUl2W-3fkbL5_f_K15FL30cAy4iR1S_B8bVeg --name "[scratch] 3.1 slide-tool test"`; add one slide; `thumb` it and inspect; re-run → exit 3; bad anchor → exit 4. Leave the scratch copy in Drive and name it in the task report.
- [ ] **Step 3: Commit** `[feature] Add deck-add-slide.sh for scripted, styled new slides`.

---

### Task 14: Lab-facing slides

**Files:** Create `docs/deck-changes/10-lab-slides.md` — one row per new slide: deck, template title, after-title, new title, body, source. Apply with `deck-add-slide.sh`, then thumbnail every new slide and fix any overflow (shrink body font or split).

Slides (bodies are the source of truth in `10-lab-slides.md`; content below):

| Deck | After | New slide |
|---|---|---|
| Training overview | "Training objectives" | **The six labs** — body from `00-training-overview.md` manual action 1 |
| Training overview | "The six labs" | **Before day one** — body from `00-training-overview.md` manual action 2, pointing at `scripts\preflight.ps1` |
| 6.2 Memory leaks | the last content slide before Q&A | **Lab 1 — Unbounded retention**: "The quote service's heap grows with every request, and a full GC gives none of it back. Run: `lab 1 baseline`. Measure: retained quotes, heap after full GC. Tools: `jcmd GC.heap_dump`, Eclipse MAT dominator tree, JMC. Find what keeps the memory reachable — and why." |
| 4.1 Improvements | the slide carrying "Investing time in optimizing the database access" | **Lab 2 — N+1 queries**: "The store summary is slow, and slower as data grows. Measure: SQL statements per request (Hibernate statistics), p95. Tools: p6spy log, Hibernate statistics, JFR. Count the statements, then explain the count." |
| 4.1 Improvements | the slide carrying "Tomcat thread pool" | **Lab 3 — Lock contention**: "The currency endpoint stops scaling: more threads, same throughput. Measure: throughput, p90, JFR *Java Monitor Blocked*. Tools: JFR + JMC, `jcmd Thread.print`. Find where the threads wait." |
| 5.2 Choosing a GC | the last content slide | **Lab 4 — GC mismatch**: "One workload, three collectors: Serial, G1, ZGC — on JDK 17 and 21 (25 optional). Measure: pause count and total pause time, p99, throughput. Choose a collector and defend it with numbers." |
| 4.2 JIT | the last content slide | **Lab 5 — Code cache exhaustion `[17 · 21 · 25]`**: "Throughput collapses a minute into the run, and the client sees no error. Measure: throughput over time, code cache use (`jcmd Compiler.codecache`), JFR compilation events. Find why the JIT stopped helping." |
| 5.3 GC tuning | the last content slide | **Lab 6 — Humongous allocations**: "A service returning 6 MB responses runs in a 1 GB heap; the GC log shows a sawtooth and constant collections. Measure: humongous-allocation GC events (`-Xlog:gc+heap`). Explain why these objects never pass through the young generation." |
| 3.1 Profiling | per `03-1-profiling.md` running order | **JFR**, **JMC**, **async-profiler**, **Flame graphs: how to read one**, **Choosing a tool** — bodies from `03-1-profiling.md` §A–E |
| 6.1 Heap objects | "Class histogram sample" | **Compact object headers `[25]`** — body from `06-1-heap-objects.md` manual action 1 |

"The last content slide" is resolved at apply time from a live dump and written into `10-lab-slides.md` before applying — never guessed.

- [ ] **Step 1:** Fill `10-lab-slides.md` from live dumps (anchor titles verified with `grep -c -F` = 1). **Step 2:** Apply per deck. **Step 3:** Thumbnail every new slide; fix overflow. **Step 4:** Update `docs/deck-changes/README.md` index (3.1, 6.1 and 0 become applied) and close the corresponding `manual` rows in their documents. **Step 5: Commit** `[docs] Add the lab and tooling slides to the course decks`.

---

### Task 15: Wrap-up

- [ ] Update spec §7.1 with the 1–6 numbering mapping; README "Labs" section linking `docs/labs/*`; `./mvnw -q verify` green; `git status` clean apart from `.DS_Store`; new handoff under `docs/handoffs/`.
- [ ] Report to the trainer: what was verified on which JDK, calibration numbers for labs 4–5, the local-only branches and tag awaiting a push decision, and the scratch deck copy.
