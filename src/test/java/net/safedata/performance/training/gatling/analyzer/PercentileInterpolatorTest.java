package net.safedata.performance.training.gatling.analyzer;

import net.safedata.performance.training.gatling.model.EndpointStats;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PercentileInterpolatorTest {

    private final PercentileInterpolator interpolator = new PercentileInterpolator();

    @Test
    void returnMeasuredValueForKnownPercentile() {
        EndpointStats stats = createStats(Map.of(50, 12, 75, 15, 95, 21, 99, 44));

        PercentileInterpolator.InterpolatedValue result = interpolator.getPercentile(stats, 95);

        assertEquals(21.0, result.value(), 0.01);
        assertEquals(PercentileInterpolator.Accuracy.MEASURED, result.accuracy());
    }

    @Test
    void interpolateP90BetweenP75AndP95() {
        EndpointStats stats = createStats(Map.of(50, 12, 75, 15, 95, 21, 99, 44));

        PercentileInterpolator.InterpolatedValue result = interpolator.getPercentile(stats, 90);

        // p90 = p75 + (p95 - p75) * ((90 - 75) / (95 - 75)) = 15 + 6 * 0.75 = 19.5
        assertEquals(19.5, result.value(), 0.01);
        assertEquals(PercentileInterpolator.Accuracy.INTERPOLATED, result.accuracy());
    }

    @Test
    void extrapolateP9999BeyondP99() {
        EndpointStats stats = createStats(Map.of(50, 12, 75, 15, 95, 21, 99, 44));

        PercentileInterpolator.InterpolatedValue result = interpolator.getPercentile(stats, 99.99);

        // Log-linear extrapolation from p95->p99 trend
        // p99.99 = p99 * (p99/p95)^((99.99 - 99) / (99 - 95))
        // = 44 * (44/21)^(0.99/4) = 44 * 2.095^0.2475 ≈ 53.5
        assertTrue(result.value() > 44);
        assertTrue(result.value() < 100);
        assertEquals(PercentileInterpolator.Accuracy.EXTRAPOLATED, result.accuracy());
    }

    @Test
    void interpolateP60BetweenP50AndP75() {
        EndpointStats stats = createStats(Map.of(50, 10, 75, 25, 95, 40, 99, 60));

        PercentileInterpolator.InterpolatedValue result = interpolator.getPercentile(stats, 60);

        // p60 = p50 + (p75 - p50) * ((60 - 50) / (75 - 50)) = 10 + 15 * 0.4 = 16
        assertEquals(16.0, result.value(), 0.01);
        assertEquals(PercentileInterpolator.Accuracy.INTERPOLATED, result.accuracy());
    }

    private EndpointStats createStats(Map<Integer, Integer> percentiles) {
        return new EndpointStats("test", 1000L, 1000L, 0L, 0.0, 100.0, percentiles);
    }
}
