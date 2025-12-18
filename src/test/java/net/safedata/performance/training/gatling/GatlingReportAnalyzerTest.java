package net.safedata.performance.training.gatling;

import net.safedata.performance.training.gatling.model.*;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class GatlingReportAnalyzerTest {

    @Test
    void analyzeIntegrationTest() throws Exception {
        Path pdfPath = Path.of("VopProvisioning-07.10.2025.pdf");

        // Skip if PDF not available
        if (!pdfPath.toFile().exists()) {
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);

        NFRConfig nfr = NFRConfig.builder()
            .endpoint("surepay-verify-account")
                .latency(90.0, 150, 120)
                .latency(95.0, 400, 350)
                .latency(99.99, 1000, null)
                .throughput(1500.0, 1350.0, 1400.0)
                .errorRate(0.1, 0.05)
            .endpoint("eba-payee-information")
                .latency(95.0, 500, null)
            .build();

        EvaluationResult result = GatlingReportAnalyzer.analyze(pdfPath, nfr, out);

        assertNotNull(result);
        assertEquals(2, result.endpointEvaluations().size());

        String output = baos.toString();
        assertTrue(output.contains("GATLING REPORT ANALYSIS"));
        assertTrue(output.contains("surepay-verify-account"));
        assertTrue(output.contains("OVERALL RESULT"));
    }
}
