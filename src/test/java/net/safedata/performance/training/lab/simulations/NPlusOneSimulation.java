package net.safedata.performance.training.lab.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

/** Lab 2 - N+1 queries: 10 users load the store summary, then passes at 3 or fewer SQL statements per request. */
public class NPlusOneSimulation extends LabSimulation {

    // Lab threshold (labs/2-nplus1/lab.properties): SQL statements one /stores request may prepare
    private static final int MAX_SQL_STATEMENTS_PER_REQUEST = 3;
    // Concurrent users in the load phase
    private static final int USERS = 10;

    public NPlusOneSimulation() {
        super("n-plus-one-queries", "/lab/nplus1/stats");
        ScenarioBuilder load = scenario("nplus1")
                .exec(http("stores").get("/lab/nplus1/stores"));
        judgeByCount(load.injectClosed(constantConcurrentUsers(USERS).during(duration(60))),
                     "stores", "SQL statements per request", "sqlStatementsPerRequest", MAX_SQL_STATEMENTS_PER_REQUEST);
    }
}
