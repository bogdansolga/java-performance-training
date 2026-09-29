package net.safedata.performance.training.lab.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.bodyString;
import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class GcSimulation extends Simulation {

    // 99th percentile of /report, in ms: long stop-the-world pauses push the tail above this
    // (measured: Serial 22-46 ms, G1/ZGC 7-9 ms; 15 ms sits between them with margin on both sides)
    private static final int MAX_P99_MILLIS = 15;
    // Open model at a fixed arrival rate: a GC pause shows up as latency, not as lower throughput
    private static final int REQUESTS_PER_SECOND = 200;

    private static final String BASE_URL = System.getenv().getOrDefault("LAB_BASE_URL", "http://localhost:8080");
    private static final int DURATION_SECONDS =
            Integer.parseInt(System.getenv().getOrDefault("LAB_DURATION_SECONDS", "60"));
    private static final String REPORT = "report";

    private final ScenarioBuilder load = scenario("gc")
            .exec(http(REPORT).get("/lab/gc/report"));

    // Runs once, after the load: prints the collections per collector (the verdict here is the p99 assertion)
    private final ScenarioBuilder verdict = scenario("verdict")
            .exec(http("verdict: stats").get("/lab/gc/stats")
                    .check(bodyString().saveAs("stats"), status().is(200)))
            .exec(session -> {
                System.out.println("Lab statistics: " + session.getString("stats"));
                return session;
            });

    {
        setUp(load.injectOpen(
                        // 15 s warm-up ramp, so JIT compilation does not land in the tail
                        rampUsersPerSec(10).to(REQUESTS_PER_SECOND).during(Duration.ofSeconds(15)),
                        constantUsersPerSec(REQUESTS_PER_SECOND).during(Duration.ofSeconds(DURATION_SECONDS)))
                .andThen(verdict.injectOpen(atOnceUsers(1))))
                .protocols(http.baseUrl(BASE_URL).shareConnections())
                .assertions(
                        global().failedRequests().count().is(0L),
                        details(REPORT).responseTime().percentile(99.0).lt(MAX_P99_MILLIS));
    }
}
