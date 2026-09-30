package net.safedata.performance.training.lab.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

/**
 * Virtual-thread pinning. Start the app with --spring.profiles.active=lock-contention and
 * --spring.threads.virtual.enabled=true, then compare JDK 21 with JDK 25.
 */
public class PinningSimulation extends LabSimulation {

    // Mean throughput: each request waits 50 ms, so 200 users that are not pinned reach ~3 200 req/s (measured);
    // pinned or platform threads stay near 250-370 req/s. The margin leaves room for slower laptops
    private static final double MIN_REQUESTS_PER_SECOND = 1_000;

    // Concurrent users, far more than the CPU count (the number of carrier threads)
    private static final int USERS = 200;
    private static final String PINNED = "pinned";

    public PinningSimulation() {
        super("lock-contention", "/lab/contention/stats");
        ScenarioBuilder load = scenario("pinning")
                .exec(http(PINNED).get("/lab/contention/pinned"));
        judgeByLatency(load.injectClosed(constantConcurrentUsers(USERS).during(duration(30))), PINNED,
                       global().requestsPerSec().gt(MIN_REQUESTS_PER_SECOND));
    }
}
