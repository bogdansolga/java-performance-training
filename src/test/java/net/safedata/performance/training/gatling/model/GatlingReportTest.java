package net.safedata.performance.training.gatling.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GatlingReportTest {

    @Test
    void createReportAndAccessFields() {
        RunInfo runInfo = new RunInfo(
            "TestSimulation",
            LocalDateTime.of(2025, 10, 6, 12, 4, 51),
            "15m 2s",
            "Performance test"
        );

        EndpointStats endpoint = new EndpointStats(
            "test-endpoint", 1000L, 1000L, 0L, 0.0, 100.0,
            Map.of(50, 10, 75, 15, 95, 20, 99, 40)
        );

        GatlingReport report = new GatlingReport(runInfo, List.of(endpoint));

        assertEquals("TestSimulation", report.runInfo().simulationName());
        assertEquals(1, report.endpoints().size());
    }

    @Test
    void getEndpointByName() {
        EndpointStats ep1 = new EndpointStats("ep1", 100L, 100L, 0L, 0.0, 10.0, Map.of());
        EndpointStats ep2 = new EndpointStats("ep2", 200L, 200L, 0L, 0.0, 20.0, Map.of());

        GatlingReport report = new GatlingReport(
            new RunInfo("Test", LocalDateTime.now(), "1m", "desc"),
            List.of(ep1, ep2)
        );

        assertTrue(report.getEndpoint("ep1").isPresent());
        assertTrue(report.getEndpoint("ep2").isPresent());
        assertTrue(report.getEndpoint("unknown").isEmpty());
    }
}
