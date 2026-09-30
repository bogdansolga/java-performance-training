package net.safedata.performance.training.lab.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

/** Lab 1 - unbounded retention: 200 quotes/s, then passes when the app holds at most 1 000 of them. */
public class RetentionSimulation extends LabSimulation {

    // Lab threshold (labs/1-retention/lab.properties): quotes the app may still hold after the load
    private static final int MAX_RETAINED_QUOTES = 1_000;

    // Arrival rate of the load phase
    private static final int REQUESTS_PER_SECOND = 200;
    private static final int SKU_COUNT = 500;

    public RetentionSimulation() {
        super("unbounded-retention", "/lab/retention/stats");
        Iterator<Map<String, Object>> skus = Stream.iterate(0, i -> (i + 1) % SKU_COUNT)
                                                   .map(i -> Map.<String, Object>of("sku", "SKU-" + i))
                                                   .iterator();
        ScenarioBuilder load = scenario("retention")
                .feed(skus)
                .exec(http("quote").get("/lab/retention/quote/#{sku}"));
        judgeByCount(load.injectOpen(constantUsersPerSec(REQUESTS_PER_SECOND).during(duration(60))),
                     "quote", "retained quotes", "retainedQuotes", MAX_RETAINED_QUOTES);
    }
}
