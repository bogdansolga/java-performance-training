package net.safedata.performance.training.lab.simulations;

import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

public class SmokeSimulation extends Simulation {

    private static final String BASE_URL = System.getenv().getOrDefault("LAB_BASE_URL", "http://localhost:8080");

    {
        setUp(scenario("smoke").exec(http("health").get("/actuator/health"))
                .injectOpen(atOnceUsers(20)))
                .protocols(http.baseUrl(BASE_URL));
    }
}
