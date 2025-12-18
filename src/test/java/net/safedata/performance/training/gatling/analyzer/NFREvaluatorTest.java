package net.safedata.performance.training.gatling.analyzer;

import net.safedata.performance.training.gatling.model.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class NFREvaluatorTest {

    private final NFREvaluator evaluator = new NFREvaluator();

    @Test
    void evaluatePassingEndpoint() {
        GatlingReport report = createReport("test-endpoint", 1000.0, 0.0,
            Map.of(50, 10, 75, 15, 95, 20, 99, 40));

        NFRConfig nfr = NFRConfig.builder()
            .endpoint("test-endpoint")
                .latency(95.0, 100, 80)
                .throughput(500.0, null, null)
                .errorRate(1.0, null)
            .build();

        EvaluationResult result = evaluator.evaluate(report, nfr);

        assertEquals(Result.PASS, result.overallResult());
        assertEquals(1, result.endpointEvaluations().size());
    }

    @Test
    void evaluateFailingLatency() {
        GatlingReport report = createReport("test-endpoint", 1000.0, 0.0,
            Map.of(50, 10, 75, 15, 95, 200, 99, 400));

        NFRConfig nfr = NFRConfig.builder()
            .endpoint("test-endpoint")
                .latency(95.0, 100, 80)
            .build();

        EvaluationResult result = evaluator.evaluate(report, nfr);

        assertEquals(Result.FAIL, result.overallResult());
    }

    @Test
    void evaluateWarningThroughput() {
        GatlingReport report = createReport("test-endpoint", 550.0, 0.0,
            Map.of(50, 10, 75, 15, 95, 20, 99, 40));

        NFRConfig nfr = NFRConfig.builder()
            .endpoint("test-endpoint")
                .latency(95.0, 100, null)
                .throughput(500.0, null, 600.0)
            .build();

        EvaluationResult result = evaluator.evaluate(report, nfr);

        assertEquals(Result.WARN, result.overallResult());
    }

    @Test
    void skipEndpointsNotInNFR() {
        GatlingReport report = new GatlingReport(
            new RunInfo("Test", LocalDateTime.now(), "1m", "desc"),
            List.of(
                new EndpointStats("configured", 100L, 100L, 0L, 0.0, 100.0, Map.of(95, 20)),
                new EndpointStats("not-configured", 200L, 200L, 0L, 0.0, 200.0, Map.of(95, 30))
            )
        );

        NFRConfig nfr = NFRConfig.builder()
            .endpoint("configured")
                .latency(95.0, 100, null)
            .build();

        EvaluationResult result = evaluator.evaluate(report, nfr);

        assertEquals(1, result.endpointEvaluations().size());
        assertEquals("configured", result.endpointEvaluations().get(0).endpointName());
    }

    private GatlingReport createReport(String endpointName, double tps, double errorPct,
                                       Map<Integer, Integer> percentiles) {
        long total = 1000L;
        long ko = (long)(total * errorPct / 100);
        EndpointStats stats = new EndpointStats(endpointName, total, total - ko, ko, errorPct, tps, percentiles);
        return new GatlingReport(
            new RunInfo("Test", LocalDateTime.now(), "1m", "desc"),
            List.of(stats)
        );
    }
}
