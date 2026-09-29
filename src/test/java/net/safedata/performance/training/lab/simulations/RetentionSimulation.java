package net.safedata.performance.training.lab.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.bodyString;
import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class RetentionSimulation extends Simulation {

    // Lab threshold (labs/1-retention/lab.properties): quotes the app may still hold after the load
    private static final int MAX_RETAINED_QUOTES = 1_000;
    // Arrival rate of the load phase
    private static final int REQUESTS_PER_SECOND = 200;

    private static final String BASE_URL = System.getenv().getOrDefault("LAB_BASE_URL", "http://localhost:8080");
    private static final int DURATION_SECONDS =
            Integer.parseInt(System.getenv().getOrDefault("LAB_DURATION_SECONDS", "60"));
    private static final String VERDICT = "verdict: retainedQuotes <= " + MAX_RETAINED_QUOTES;

    private static final int SKU_COUNT = 500;

    private final Iterator<Map<String, Object>> skus = Stream.iterate(0, i -> (i + 1) % SKU_COUNT)
            .map(i -> Map.<String, Object>of("sku", "SKU-" + i))
            .iterator();

    private final ScenarioBuilder load = scenario("retention")
            .feed(skus)
            .exec(http("quote").get("/lab/retention/quote/#{sku}"));

    // Runs once, after the load: reads the lab statistics and checks them against the threshold
    private final ScenarioBuilder verdict = scenario("verdict")
            .exec(http(VERDICT).get("/lab/retention/stats")
                    .check(bodyString().saveAs("stats"),
                            status().is(200),
                            jsonPath("$.retainedQuotes").ofLong().lte((long) MAX_RETAINED_QUOTES)))
            .exec(session -> {
                System.out.println("Lab statistics: " + session.getString("stats"));
                return session;
            });

    {
        setUp(load.injectOpen(constantUsersPerSec(REQUESTS_PER_SECOND).during(Duration.ofSeconds(DURATION_SECONDS)))
                .andThen(verdict.injectOpen(atOnceUsers(1))))
                .protocols(http.baseUrl(BASE_URL).shareConnections())
                .assertions(
                        global().failedRequests().count().is(0L),
                        details(VERDICT).failedRequests().count().is(0L));
    }
}
