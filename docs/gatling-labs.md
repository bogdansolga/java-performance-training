# Gatling labs

Each lab is a small part of this application with one real-world performance problem built in.
You switch a lab on with a Spring profile, put load on it with a Gatling test, watch it in a profiler,
and the test tells you whether the problem is still there.

## The labs

| Lab | The problem you will see | Code (under `src/main/java/net/safedata/performance/training/`) | Endpoint under load |
|---|---|---|---|
| 1 Retention | memory grows with every request and never comes back | `lab/retention/QuoteService.java` | `GET /lab/retention/quote/{sku}` |
| 2 N+1 | one page view runs a lot of SQL statements | `lab/nplus1/StoreSummaryService.java` | `GET /lab/nplus1/stores` |
| 3 Contention | more users, but no more throughput | `lab/contention/ExchangeRateService.java` | `GET /lab/contention/convert/EUR?amount=10` |
| 4 GC | long, irregular response-time spikes | `lab/gc/ReportService.java` | `GET /lab/gc/report` |
| 5 Humongous | frequent collections under large responses | `service/ProductService.java` (`getHumongousPayload`) | `GET /product/humongous` |

Every lab also has a `/stats` endpoint, which the test reads at the end to judge the result.
The Gatling tests are in `src/test/java/net/safedata/performance/training/lab/simulations/`.

| Lab | Profile | JVM options | Gatling test | Passes when |
|---|---|---|---|---|
| 1 Retention | `unbounded-retention` | `-Xms256m -Xmx256m -XX:+UseG1GC` | `RetentionSimulation` | retained quotes ≤ 1 000 |
| 2 N+1 | `n-plus-one-queries` | `-Xms512m -Xmx512m -XX:+UseG1GC` | `NPlusOneSimulation` | SQL statements per request ≤ 3 |
| 3 Contention | `lock-contention` | `-Xms256m -Xmx256m -XX:+UseG1GC` | `ContentionSimulation` | p95 < 50 ms and > 500 req/s |
| 4 GC | `gc-mismatch` | `-Xms1g -Xmx1g -XX:+UseSerialGC` | `GcSimulation` | p99 < 15 ms |
| 5 Humongous | `humongous-allocations` | `-Xms1g -Xmx1g -XX:+UseG1GC` | `HumongousSimulation` | collections caused by large allocations ≤ 1 |

To try another lab, put its profile, JVM options and Gatling test into the commands below.

## Steps (example: lab 1, retention)

1. Open PowerShell in the repository folder, build once, and start the application with the lab's profile:

   ```powershell
   .\mvnw.cmd -q -DskipTests package
   java -Xms256m -Xmx256m -XX:+UseG1GC -jar target\java-performance-training-0.0.1-SNAPSHOT.jar --spring.profiles.active=unbounded-retention
   ```

2. Start your profiler and attach it to the application. No profiler? Download VisualVM: https://visualvm.github.io/download.html

3. Open a second PowerShell in the repository folder and run the lab's Gatling test:

   ```powershell
   .\mvnw.cmd gatling:test "-Dgatling.simulationClass=net.safedata.performance.training.lab.simulations.RetentionSimulation"
   ```

4. Watch heap, CPU and threads in the profiler while the test runs (about 60 seconds).

5. Read the last line before `BUILD SUCCESS` / `BUILD FAILURE`:

   ```
   LAB RESULT: FAILED - retained quotes = 12000 (must be <= 1000)
   ```

   Labs 3 and 4 judge latency instead: read the `p95` / `p99` / `requests per second` lines - `false` means failed, the measured value is in `(actual : …)`.

6. Change the code of the lab (table above), rebuild with `.\mvnw.cmd -q -DskipTests package`, restart the
   application, and run the test again - until it passes.

7. Want an `OutOfMemoryError` (lab 1)? Start the application with `-Xms128m -Xmx128m`, then in the second
   PowerShell set a longer run before the step 3 command:

   ```powershell
   $env:LAB_DURATION_SECONDS=150
   ```

   Or keep 256 MB and run the test 3 times without restarting the application.

## Good to know

- A run takes about 60 seconds (75 for `GcSimulation`). `$env:LAB_DURATION_SECONDS=30` shortens it.
- Stop the application (Ctrl+C) before you rebuild - Windows cannot replace a running jar.
- If the application is not running, or runs without the lab's profile, the test stops at once and says so.
- The full HTML report of a run: `target\gatling\<simulation>-<timestamp>\index.html`.
- macOS / Linux: use `./mvnw` instead of `.\mvnw.cmd`, `/` instead of `\`, and no quotes around the `-D` argument.

## Virtual threads (trainer demo)

Same profile as lab 3 (`lock-contention`), test `PinningSimulation`. It passes above 1 000 requests/s.
Run it three times, restarting the application in between:

1. as it is;
2. with the program argument `--spring.threads.virtual.enabled=true`, on JDK 21;
3. the same, on JDK 25.

Compare requests/s and p95. The JDK that matters is the one running `java -jar`, not the one running Maven.
