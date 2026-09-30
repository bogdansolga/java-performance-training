package net.safedata.performance.training.lab.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.bodyString;
import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class HumongousSimulation extends Simulation {

    // Lab threshold (labs/5-humongous/lab.properties): collections the JVM may start because of large allocations
    private static final int MAX_HUMONGOUS_GC_EVENTS = 1;
    // Open model: each arrival requests one 6 MB response
    private static final int REQUESTS_PER_SECOND = 15;

    private static final String BASE_URL = System.getenv().getOrDefault("LAB_BASE_URL", "http://localhost:8080");
    private static final int DURATION_SECONDS =
            Integer.parseInt(System.getenv().getOrDefault("LAB_DURATION_SECONDS", "60"));
    private static final String VERDICT = "verdict: humongousGcEvents <= " + MAX_HUMONGOUS_GC_EVENTS;

    private final ScenarioBuilder load = scenario("humongous")
            .exec(http("humongous").get("/product/humongous"));

    // Runs once, after the load: reads the collections caused by large allocations and checks the threshold
    private final ScenarioBuilder verdict = scenario("verdict")
            .exec(http(VERDICT).get("/lab/humongous/stats")
                    .check(bodyString().saveAs("stats"),
                            status().is(200),
                            jsonPath("$.humongousGcEvents").ofLong().saveAs("actual"),
                            jsonPath("$.humongousGcEvents").ofLong().lte((long) MAX_HUMONGOUS_GC_EVENTS)))
            .exec(session -> {
                System.out.println("Lab statistics: " + session.getString("stats"));
                LabResult.record("collections caused by large allocations", session.contains("actual") ? session.getLong("actual") : null, MAX_HUMONGOUS_GC_EVENTS);
                return session;
            });

    {
        setUp(load.injectOpen(constantUsersPerSec(REQUESTS_PER_SECOND).during(Duration.ofSeconds(DURATION_SECONDS)))
                .andThen(verdict.injectOpen(atOnceUsers(1))))
                .protocols(http.baseUrl(BASE_URL).shareConnections())
                .assertions(
                        details("humongous").failedRequests().count().is(0L),
                        details(VERDICT).failedRequests().count().is(0L));
    }

    @Override
    public void after() {
        LabResult.printAtExit();
    }

    @Override
    public void before() {
        LabPreflight.requireLab(BASE_URL, "/lab/humongous/stats", "humongous-allocations");
    }

}
