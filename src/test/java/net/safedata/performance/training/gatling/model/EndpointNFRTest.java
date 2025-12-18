package net.safedata.performance.training.gatling.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EndpointNFRTest {

    @Test
    void createWithAllThresholds() {
        EndpointNFR nfr = new EndpointNFR(
            "test-endpoint",
            List.of(new LatencyThreshold(95.0, 400, 350)),
            new ThroughputThreshold(1500.0, 1350.0, 1400.0),
            new ErrorRateThreshold(0.1, 0.05)
        );

        assertEquals("test-endpoint", nfr.endpointName());
        assertEquals(1, nfr.latencyThresholds().size());
        assertTrue(nfr.throughput().isPresent());
        assertTrue(nfr.errorRate().isPresent());
    }

    @Test
    void createWithOnlyLatency() {
        EndpointNFR nfr = new EndpointNFR(
            "test-endpoint",
            List.of(new LatencyThreshold(95.0, 400, null)),
            null,
            null
        );

        assertEquals("test-endpoint", nfr.endpointName());
        assertTrue(nfr.throughput().isEmpty());
        assertTrue(nfr.errorRate().isEmpty());
    }
}
