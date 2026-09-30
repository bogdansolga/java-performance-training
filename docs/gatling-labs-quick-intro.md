# Gatling labs — quick intro

## The labs

| Lab | Profile | JVM options | Simulation | Passes when |
|---|---|---|---|---|
| 1 Retention | `unbounded-retention` | `-Xms256m -Xmx256m -XX:+UseG1GC` | `RetentionSimulation` | retained quotes ≤ 1 000 |
| 2 N+1 | `n-plus-one-queries` | `-Xms512m -Xmx512m -XX:+UseG1GC` | `NPlusOneSimulation` | SQL statements per request ≤ 3 |
| 3 Contention | `lock-contention` | `-Xms256m -Xmx256m -XX:+UseG1GC` | `ContentionSimulation` | p95 < 50 ms and > 500 req/s |
| 4 GC | `gc-mismatch` | `-Xms1g -Xmx1g -XX:+UseSerialGC` | `GcSimulation` | p99 < 15 ms |
| 5 Humongous | `humongous-allocations` | `-Xms1g -Xmx1g -XX:+UseG1GC` | `HumongousSimulation` | collections caused by large allocations ≤ 1 |

## Steps (example: lab 1)

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

   Labs 3 and 4 judge latency instead: read the `p95` / `p99` / `requests per second` lines — `false` means failed, the measured value is in `(actual : …)`.

6. Want an `OutOfMemoryError` (lab 1)? Start the application with `-Xms128m -Xmx128m` and run the test once for longer:

   ```powershell
   $env:LAB_DURATION_SECONDS=150
   ```

   Or keep 256 MB and run the test 3 times without restarting the application.

More detail: [gatling-labs.md](gatling-labs.md).
