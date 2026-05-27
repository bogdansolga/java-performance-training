package net.safedata.performance.training.gatling.report;

import net.safedata.performance.training.gatling.analyzer.PercentileInterpolator.Accuracy;
import net.safedata.performance.training.gatling.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Manual test to visualize the console output.
 * Run with: mvn test -Dtest=ConsoleReportWriterManualTest
 */
public class ConsoleReportWriterManualTest {

    public static void main(String[] args) {
        // Create a sample report
        RunInfo runInfo = new RunInfo(
            "VOPProvisioningAPISimulation",
            LocalDateTime.of(2025, 10, 6, 12, 4, 51),
            "15m 2s",
            "VOP Provisioning Performance Test"
        );

        EndpointStats surepayStats = new EndpointStats(
            "surepay-verify-account",
            850730L,
            850000L,
            730L,
            0.086,
            942.0,
            Map.of(50, 12, 75, 15, 95, 21, 99, 44)
        );

        EndpointStats ebaStats = new EndpointStats(
            "eba-payee-information",
            200000L,
            199950L,
            50L,
            0.025,
            220.0,
            Map.of(50, 45, 75, 60, 95, 120, 99, 250)
        );

        GatlingReport report = new GatlingReport(
            runInfo,
            List.of(surepayStats, ebaStats)
        );

        // Create evaluations
        LatencyEvaluation surepayP90 = new LatencyEvaluation(
            90.0, 18.0, Accuracy.INTERPOLATED, 150, 120, Result.PASS
        );

        LatencyEvaluation surepayP95 = new LatencyEvaluation(
            95.0, 21.0, Accuracy.MEASURED, 400, 350, Result.PASS
        );

        LatencyEvaluation surepayP9999 = new LatencyEvaluation(
            99.99, 53.5, Accuracy.EXTRAPOLATED, 1000, null, Result.PASS
        );

        ThroughputEvaluation surepayThroughput = new ThroughputEvaluation(
            942.0, 1500.0, 1400.0, Result.WARN
        );

        ErrorRateEvaluation surepayError = new ErrorRateEvaluation(
            0.086, 0.1, 0.05, Result.WARN
        );

        EndpointEvaluation surepayEval = new EndpointEvaluation(
            "surepay-verify-account",
            Result.WARN,
            List.of(surepayP90, surepayP95, surepayP9999),
            surepayThroughput,
            surepayError
        );

        LatencyEvaluation ebaP95 = new LatencyEvaluation(
            95.0, 120.0, Accuracy.MEASURED, 500, null, Result.PASS
        );

        ThroughputEvaluation ebaThroughput = new ThroughputEvaluation(
            220.0, 500.0, null, Result.FAIL
        );

        ErrorRateEvaluation ebaError = new ErrorRateEvaluation(
            0.025, 0.1, null, Result.PASS
        );

        EndpointEvaluation ebaEval = new EndpointEvaluation(
            "eba-payee-information",
            Result.FAIL,
            List.of(ebaP95),
            ebaThroughput,
            ebaError
        );

        EvaluationResult result = new EvaluationResult(List.of(surepayEval, ebaEval));

        // Write the report
        ConsoleReportWriter writer = new ConsoleReportWriter();
        writer.write(report, result);
    }
}
