package net.safedata.performance.training.lab.simulations;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.bodyString;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

/**
 * Base class of the lab simulations: a lab puts load on the application, then reads the lab's
 * {@code /stats} endpoint once and is judged either by a count from it or by latency assertions.
 * <p>
 * A subclass builds its load and calls {@link #judgeByCount} or {@link #judgeByLatency} from its
 * constructor. This class checks, before the load starts, that the application runs with the
 * lab's profile.
 */
abstract class LabSimulation extends Simulation {

    protected static final String BASE_URL = System.getenv().getOrDefault("LAB_BASE_URL", "http://localhost:8080");

    private final String profile;
    private final String statsPath;
    private boolean printResult;

    protected LabSimulation(String profile, String statsPath) {
        this.profile = profile;
        this.statsPath = statsPath;
    }

    /** How long the load runs: {@code LAB_DURATION_SECONDS}, or the lab's default. */
    protected static Duration duration(int defaultSeconds) {
        return Duration.ofSeconds(Integer.parseInt(
                System.getenv().getOrDefault("LAB_DURATION_SECONDS", String.valueOf(defaultSeconds))));
    }

    /**
     * The lab passes when the number {@code jsonField} reported by {@code /stats} is at most {@code limit}.
     * Prints {@code LAB RESULT: ...} as the last line of the run.
     */
    protected void judgeByCount(PopulationBuilder load, String loadRequest, String what, String jsonField, long limit) {
        String verdict = "verdict: " + jsonField + " <= " + limit;
        ScenarioBuilder check = scenario("verdict")
                .exec(http(verdict).get(statsPath)
                        .check(bodyString().saveAs("stats"),
                                status().is(200),
                                jsonPath("$." + jsonField).ofLong().saveAs("actual"),
                                jsonPath("$." + jsonField).ofLong().lte(limit)))
                .exec(session -> {
                    printStats(session);
                    LabResult.record(what, session.contains("actual") ? session.getLong("actual") : null, limit);
                    return session;
                });
        printResult = true;
        run(load, check, List.of(noFailedRequests(loadRequest), details(verdict).failedRequests().count().is(0L)));
    }

    /** The lab passes when every latency or throughput assertion holds; {@code /stats} is printed for reference. */
    protected void judgeByLatency(PopulationBuilder load, String loadRequest, Assertion... assertions) {
        ScenarioBuilder stats = scenario("verdict")
                .exec(http("verdict: stats").get(statsPath)
                        .check(bodyString().saveAs("stats"), status().is(200)))
                .exec(session -> {
                    printStats(session);
                    return session;
                });
        List<Assertion> all = new ArrayList<>();
        all.add(noFailedRequests(loadRequest));
        all.addAll(List.of(assertions));
        run(load, stats, all);
    }

    private void run(PopulationBuilder load, ScenarioBuilder afterLoad, List<Assertion> assertions) {
        setUp(load.andThen(afterLoad.injectOpen(atOnceUsers(1))))
                .protocols(protocol())
                .assertions(assertions);
    }

    private static HttpProtocolBuilder protocol() {
        return http.baseUrl(BASE_URL).shareConnections();
    }

    private static Assertion noFailedRequests(String loadRequest) {
        return details(loadRequest).failedRequests().count().is(0L);
    }

    private static void printStats(Session session) {
        System.out.println("Lab statistics: " + session.getString("stats"));
    }

    @Override
    public void before() {
        LabPreflight.requireLab(BASE_URL, statsPath, profile);
    }

    @Override
    public void after() {
        if (printResult) {
            LabResult.printAtExit();
        }
    }
}
