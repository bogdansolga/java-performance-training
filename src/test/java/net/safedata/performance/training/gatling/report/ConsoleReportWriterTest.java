package net.safedata.performance.training.gatling.report;

import net.safedata.performance.training.gatling.analyzer.PercentileInterpolator.Accuracy;
import net.safedata.performance.training.gatling.model.*;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleReportWriterTest {

    @Test
    void writeReportContainsKeyElements() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        ConsoleReportWriter writer = new ConsoleReportWriter(out);

        RunInfo runInfo = new RunInfo(
            "TestSimulation",
            LocalDateTime.of(2025, 10, 6, 12, 4, 51),
            "15m 2s",
            "Test description"
        );

        GatlingReport report = new GatlingReport(runInfo, List.of());

        LatencyEvaluation latency = new LatencyEvaluation(
            95.0, 21.0, Accuracy.MEASURED, 400, 350, Result.PASS
        );

        ThroughputEvaluation throughput = new ThroughputEvaluation(
            942.0, 1500.0, 1400.0, Result.WARN
        );

        EndpointEvaluation endpoint = new EndpointEvaluation(
            "test-endpoint",
            Result.WARN,
            List.of(latency),
            throughput,
            null
        );

        EvaluationResult result = new EvaluationResult(List.of(endpoint));

        writer.write(report, result);

        String output = baos.toString();

        assertTrue(output.contains("TestSimulation"));
        assertTrue(output.contains("test-endpoint"));
        assertTrue(output.contains("PASS") || output.contains("✓"));
        assertTrue(output.contains("WARN") || output.contains("⚠"));
        assertTrue(output.contains("p95"));
        assertTrue(output.contains("942"));
    }
}
