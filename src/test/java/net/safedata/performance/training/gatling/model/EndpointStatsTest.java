package net.safedata.performance.training.gatling.model;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class EndpointStatsTest {

    @Test
    void createAndAccessFields() {
        EndpointStats stats = new EndpointStats(
            "test-endpoint",
            1000L, 990L, 10L,
            1.0,
            100.5,
            Map.of(50, 12, 75, 15, 95, 21, 99, 44)
        );

        assertEquals("test-endpoint", stats.name());
        assertEquals(1000L, stats.totalRequests());
        assertEquals(990L, stats.okCount());
        assertEquals(10L, stats.koCount());
        assertEquals(1.0, stats.errorPercent());
        assertEquals(100.5, stats.requestsPerSecond());
        assertEquals(12, stats.percentiles().get(50));
    }

    @Test
    void getPercentileReturnsValueForKnown() {
        EndpointStats stats = new EndpointStats(
            "test", 100L, 100L, 0L, 0.0, 10.0,
            Map.of(50, 10, 75, 20, 95, 30, 99, 50)
        );

        assertEquals(30, stats.getPercentile(95).orElse(-1));
    }

    @Test
    void getPercentileReturnsEmptyForUnknown() {
        EndpointStats stats = new EndpointStats(
            "test", 100L, 100L, 0L, 0.0, 10.0,
            Map.of(50, 10)
        );

        assertTrue(stats.getPercentile(90).isEmpty());
    }
}
