package net.safedata.performance.training.gatling;

import net.safedata.performance.training.gatling.model.EvaluationResult;
import net.safedata.performance.training.gatling.model.NFRConfig;
import net.safedata.performance.training.gatling.model.Result;

import java.nio.file.Path;

public class GatlingNFRAnalyzerExample {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: GatlingNFRAnalyzerExample <path-to-pdf>");
            System.exit(1);
        }

        Path pdfPath = Path.of(args[0]);

        // Define NFRs
        NFRConfig nfr = NFRConfig.builder()
            .endpoint("surepay-verify-account")
                .latency(90.0, 150, 120)
                .latency(95.0, 400, 350)
                .latency(99.99, 1000, null)
                .throughput(1500.0, 1350.0, 1400.0)
                .errorRate(0.1, 0.05)
            .endpoint("eba-payee-information")
                .latency(90.0, 150, 120)
                .latency(95.0, 400, 350)
                .latency(99.99, 1000, null)
                .throughput(500.0, null, null)
                .errorRate(0.1, null)
            .build();

        // Analyze and print report
        EvaluationResult result = GatlingReportAnalyzer.analyze(pdfPath.toString(), nfr);

        // Exit with appropriate code
        System.exit(result.overallResult() == Result.FAIL ? 1 : 0);
    }
}
