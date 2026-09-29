package net.safedata.performance.training.lab.simulations;

import io.gatling.javaapi.core.Simulation;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

/**
 * Virtual-thread pinning. Start the app with --spring.profiles.active=lock-contention and
 * --spring.threads.virtual.enabled=true, then compare JDK 21 with JDK 24+.
 */
public class PinningSimulation extends Simulation {

    // Mean throughput: each request waits 50 ms, so 200 users that are not pinned reach ~3 200 req/s (measured);
    // pinned or platform threads stay near 250-370 req/s. The margin leaves room for slower laptops
    private static final double MIN_REQUESTS_PER_SECOND = 1_000;
    // Concurrent users, far more than the CPU count (the number of carrier threads)
    private static final int USERS = 200;

    private static final String BASE_URL = System.getenv().getOrDefault("LAB_BASE_URL", "http://localhost:8080");
    private static final int DURATION_SECONDS =
            Integer.parseInt(System.getenv().getOrDefault("LAB_DURATION_SECONDS", "30"));

    {
        setUp(scenario("pinning")
                .exec(http("pinned").get("/lab/contention/pinned"))
                .injectClosed(constantConcurrentUsers(USERS).during(Duration.ofSeconds(DURATION_SECONDS))))
                .protocols(http.baseUrl(BASE_URL).shareConnections())
                .assertions(
                        global().failedRequests().count().is(0L),
                        global().requestsPerSec().gt(MIN_REQUESTS_PER_SECOND));
    }
}
