package net.safedata.performance.training.lab.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

/** Lab 5 - humongous allocations: 6 MB responses, then passes at 1 or fewer collections caused by large allocations. */
public class HumongousSimulation extends LabSimulation {

    // Lab threshold (labs/5-humongous/lab.properties): collections the JVM may start because of large allocations
    private static final int MAX_HUMONGOUS_GC_EVENTS = 1;

    // Open model: each arrival requests one 6 MB response
    private static final int REQUESTS_PER_SECOND = 15;

    public HumongousSimulation() {
        super("humongous-allocations", "/lab/humongous/stats");
        ScenarioBuilder load = scenario("humongous")
                .exec(http("humongous").get("/product/humongous"));
        judgeByCount(load.injectOpen(constantUsersPerSec(REQUESTS_PER_SECOND).during(duration(60))),
                     "humongous", "collections caused by large allocations", "humongousGcEvents", MAX_HUMONGOUS_GC_EVENTS);
    }
}
