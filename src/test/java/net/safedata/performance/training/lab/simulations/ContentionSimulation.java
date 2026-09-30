package net.safedata.performance.training.lab.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.bodyString;
import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class ContentionSimulation extends Simulation {

    // 95th percentile of /convert, in ms: each conversion waits ~10 ms, so a lock-free app stays far below this
    private static final int MAX_P95_MILLIS = 50;
    // Mean throughput over the run: 20 users that do not queue for one lock clear this easily
    private static final double MIN_REQUESTS_PER_SECOND = 500;
    // Concurrent users: matches Tomcat's threads.max
    private static final int USERS = 20;

    private static final String BASE_URL = System.getenv().getOrDefault("LAB_BASE_URL", "http://localhost:8080");
    private static final int DURATION_SECONDS =
            Integer.parseInt(System.getenv().getOrDefault("LAB_DURATION_SECONDS", "60"));
    private static final String CONVERT = "convert";

    private final ScenarioBuilder load = scenario("contention")
            .exec(http(CONVERT).get("/lab/contention/convert/EUR?amount=10"));

    // Runs once, after the load: prints the lab statistics (the verdict here is the latency assertions)
    private final ScenarioBuilder verdict = scenario("verdict")
            .exec(http("verdict: stats").get("/lab/contention/stats")
                    .check(bodyString().saveAs("stats"), status().is(200)))
            .exec(session -> {
                System.out.println("Lab statistics: " + session.getString("stats"));
                return session;
            });

    {
        setUp(load.injectClosed(constantConcurrentUsers(USERS).during(Duration.ofSeconds(DURATION_SECONDS)))
                .andThen(verdict.injectOpen(atOnceUsers(1))))
                .protocols(http.baseUrl(BASE_URL).shareConnections())
                .assertions(
                        details(CONVERT).failedRequests().count().is(0L),
                        details(CONVERT).responseTime().percentile(95.0).lt(MAX_P95_MILLIS),
                        global().requestsPerSec().gt(MIN_REQUESTS_PER_SECOND));
    }

    @Override
    public void before() {
        LabPreflight.requireLab(BASE_URL, "/lab/contention/stats", "lock-contention");
    }

}
