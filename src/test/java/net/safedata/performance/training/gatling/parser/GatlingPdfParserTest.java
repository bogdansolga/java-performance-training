package net.safedata.performance.training.gatling.parser;

import net.safedata.performance.training.gatling.model.EndpointStats;
import net.safedata.performance.training.gatling.model.GatlingReport;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class GatlingPdfParserTest {

    private final GatlingPdfParser parser = new GatlingPdfParser();

    @Test
    void parseReportFromPdf() throws IOException {
        Path pdfPath = Path.of("/Volumes/NVMe/Development/IdeaProjects/training/java-performance-training/VopProvisioning-07.10.2025.pdf");

        // Skip test if file doesn't exist (CI environment)
        if (!pdfPath.toFile().exists()) {
            return;
        }

        GatlingReport report = parser.parse(pdfPath);

        assertNotNull(report.runInfo());
        assertEquals("VOPProvisioningAPISimulation", report.runInfo().simulationName());
        assertFalse(report.endpoints().isEmpty());

        // Verify surepay endpoint was parsed
        EndpointStats surepay = report.getEndpoint("surepay-verify-account").orElseThrow();
        assertEquals(850730, surepay.totalRequests());
        assertTrue(surepay.requestsPerSecond() > 900);

        // Verify percentiles were parsed
        assertTrue(surepay.percentiles().containsKey(50));
        assertTrue(surepay.percentiles().containsKey(95));
        assertTrue(surepay.percentiles().containsKey(99));
    }

    @Test
    void throwExceptionForNonExistentFile() {
        Path nonExistent = Path.of("non-existent.pdf");

        assertThrows(IOException.class, () -> parser.parse(nonExistent));
    }
}
