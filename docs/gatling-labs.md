# Gatling labs

Each lab is one Gatling simulation that judges the running application: it **fails while the problem is there**
and **passes once it is gone**.

You need JDK 17 or newer, and port 8080 free. Use two terminals, both in the repository folder.

## 1. Start the application with the lab's profile

```bash
./mvnw -q -DskipTests package          # once, and again after every code change (stop the application first — Windows cannot replace a running jar)
java <JVM options> -jar target/java-performance-training-0.0.1-SNAPSHOT.jar --spring.profiles.active=<profile>
```

Wait for `Started ProfilingDemoApplication`. Stop it with Ctrl+C when you are done.

## 2. Run the simulation (second terminal)

```bash
./mvnw gatling:test -Dgatling.simulationClass=net.safedata.performance.training.lab.simulations.<Simulation>
```

A run takes about 60 seconds (75 for `GcSimulation`, 30 for `PinningSimulation`); prefix `LAB_DURATION_SECONDS=30` to shorten it.

## 3. Read the result

The last line before `BUILD SUCCESS` / `BUILD FAILURE` gives the verdict (labs 1, 2 and 5):

```
LAB RESULT: FAILED - retained quotes = 12000 (must be <= 1000)
```

Labs 3 and 4 judge latency: read the `p95` / `p99` / `requests per second` lines — `false` means failed, the measured value is in `(actual : …)`.
If the application is not running, or runs without the lab's profile, the run stops at once and says so.
The full HTML report: `target/gatling/<simulation>-<timestamp>/index.html`.

## Labs

| Lab | Profile | JVM options | Simulation | Passes when |
|---|---|---|---|---|
| 1 Retention | `unbounded-retention` | `-Xms256m -Xmx256m -XX:+UseG1GC` | `RetentionSimulation` | retained quotes ≤ 1 000 |
| 2 N+1 | `n-plus-one-queries` | `-Xms512m -Xmx512m -XX:+UseG1GC` | `NPlusOneSimulation` | SQL statements per request ≤ 3 |
| 3 Contention | `lock-contention` | `-Xms256m -Xmx256m -XX:+UseG1GC` | `ContentionSimulation` | p95 < 50 ms and > 500 req/s |
| 4 GC | `gc-mismatch` | `-Xms1g -Xmx1g -XX:+UseSerialGC` | `GcSimulation` | p99 < 15 ms |
| 5 Humongous | `humongous-allocations` | `-Xms1g -Xmx1g -XX:+UseG1GC` | `HumongousSimulation` | collections caused by large allocations ≤ 1 |

**Example — lab 2:**

```bash
java -Xms512m -Xmx512m -XX:+UseG1GC -jar target/java-performance-training-0.0.1-SNAPSHOT.jar --spring.profiles.active=n-plus-one-queries
./mvnw gatling:test -Dgatling.simulationClass=net.safedata.performance.training.lab.simulations.NPlusOneSimulation
```

Windows (PowerShell) — `mvnw.cmd`, and quote the `-D` argument:

```powershell
.\mvnw.cmd -q -DskipTests package
.\mvnw.cmd gatling:test "-Dgatling.simulationClass=net.safedata.performance.training.lab.simulations.NPlusOneSimulation"
$env:LAB_DURATION_SECONDS=30     # optional, before the gatling:test line
```

## Virtual threads

Start the application with profile `lock-contention` and JVM options `-Xms256m -Xmx256m`, then run `PinningSimulation`
three times, restarting the application in between:

1. as it is;
2. with the program argument `--spring.threads.virtual.enabled=true`, started with JDK 21;
3. the same, started with JDK 25.

Compare requests/s and p95; `PinningSimulation` passes when > 1 000 req/s. The JDK that matters is the one running `java -jar`, not the one running Maven.
