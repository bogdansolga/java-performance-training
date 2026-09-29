package net.safedata.performance.training.lab.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.bodyString;
import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class NPlusOneSimulation extends Simulation {

    // Lab threshold (labs/2-nplus1/lab.properties): SQL statements one /stores request may prepare
    private static final int MAX_SQL_STATEMENTS_PER_REQUEST = 3;
    // Concurrent users in the load phase
    private static final int USERS = 10;

    private static final String BASE_URL = System.getenv().getOrDefault("LAB_BASE_URL", "http://localhost:8080");
    private static final int DURATION_SECONDS =
            Integer.parseInt(System.getenv().getOrDefault("LAB_DURATION_SECONDS", "60"));
    private static final String VERDICT = "verdict: sqlStatementsPerRequest <= " + MAX_SQL_STATEMENTS_PER_REQUEST;

    private final ScenarioBuilder load = scenario("nplus1")
            .exec(http("stores").get("/lab/nplus1/stores"));

    // Runs once, after the load: /stats serves one request and reports the statements it prepared
    private final ScenarioBuilder verdict = scenario("verdict")
            .exec(http(VERDICT).get("/lab/nplus1/stats")
                    .check(bodyString().saveAs("stats"),
                            status().is(200),
                            jsonPath("$.sqlStatementsPerRequest").ofLong().lte((long) MAX_SQL_STATEMENTS_PER_REQUEST)))
            .exec(session -> {
                System.out.println("Lab statistics: " + session.getString("stats"));
                return session;
            });

    {
        setUp(load.injectClosed(constantConcurrentUsers(USERS).during(Duration.ofSeconds(DURATION_SECONDS)))
                .andThen(verdict.injectOpen(atOnceUsers(1))))
                .protocols(http.baseUrl(BASE_URL).shareConnections())
                .assertions(
                        global().failedRequests().count().is(0L),
                        details(VERDICT).failedRequests().count().is(0L));
    }
}
