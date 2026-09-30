package net.safedata.performance.training.lab.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

/** Lab 3 - lock contention: 20 users convert currencies; passes when p95 and throughput show no queuing. */
public class ContentionSimulation extends LabSimulation {

    // 95th percentile of /convert, in ms: each conversion waits ~10 ms, so a lock-free app stays far below this
    private static final int MAX_P95_MILLIS = 50;

    // Mean throughput over the run: 20 users that do not queue for one lock clear this easily
    private static final double MIN_REQUESTS_PER_SECOND = 500;

    // Concurrent users: matches Tomcat's threads.max
    private static final int USERS = 20;
    private static final String CONVERT = "convert";

    public ContentionSimulation() {
        super("lock-contention", "/lab/contention/stats");
        ScenarioBuilder load = scenario("contention")
                .exec(http(CONVERT).get("/lab/contention/convert/EUR?amount=10"));
        judgeByLatency(load.injectClosed(constantConcurrentUsers(USERS).during(duration(60))), CONVERT,
                       details(CONVERT).responseTime().percentile(95.0).lt(MAX_P95_MILLIS),
                       global().requestsPerSec().gt(MIN_REQUESTS_PER_SECOND));
    }
}
