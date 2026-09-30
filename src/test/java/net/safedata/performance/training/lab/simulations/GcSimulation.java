package net.safedata.performance.training.lab.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

/** Lab 4 - GC mismatch: a steady 200 reports/s; passes when the slowest 1% stay under 15 ms. */
public class GcSimulation extends LabSimulation {

    // 99th percentile of /report, in ms: long stop-the-world pauses push the tail above this
    // (measured: Serial 22-46 ms, G1/ZGC 7-9 ms; 15 ms sits between them with margin on both sides)
    private static final int MAX_P99_MILLIS = 15;

    // Open model at a fixed arrival rate: a GC pause shows up as latency, not as lower throughput
    private static final int REQUESTS_PER_SECOND = 200;
    private static final String REPORT = "report";

    public GcSimulation() {
        super("gc-mismatch", "/lab/gc/stats");
        ScenarioBuilder load = scenario("gc")
                .exec(http(REPORT).get("/lab/gc/report"));
        judgeByLatency(load.injectOpen(
                        // 15 s warm-up ramp, so JIT compilation does not land in the tail
                        rampUsersPerSec(10).to(REQUESTS_PER_SECOND).during(Duration.ofSeconds(15)),
                        constantUsersPerSec(REQUESTS_PER_SECOND).during(duration(60))),
                       REPORT,
                       details(REPORT).responseTime().percentile(99.0).lt(MAX_P99_MILLIS));
    }
}
