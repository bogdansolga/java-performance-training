package net.safedata.performance.training.gatling.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LatencyThresholdTest {

    @Test
    void createWithAllParameters() {
        LatencyThreshold threshold = new LatencyThreshold(95.0, 400, 350);
        assertEquals(95.0, threshold.percentile());
        assertEquals(400, threshold.maxMs());
        assertEquals(350, threshold.warnMs().orElse(-1));
    }

    @Test
    void createWithoutWarnThreshold() {
        LatencyThreshold threshold = new LatencyThreshold(99.99, 1000, null);
        assertEquals(99.99, threshold.percentile());
        assertEquals(1000, threshold.maxMs());
        assertTrue(threshold.warnMs().isEmpty());
    }

    @Test
    void evaluatePass() {
        LatencyThreshold threshold = new LatencyThreshold(95.0, 400, 350);
        assertEquals(Result.PASS, threshold.evaluate(300));
    }

    @Test
    void evaluateWarn() {
        LatencyThreshold threshold = new LatencyThreshold(95.0, 400, 350);
        assertEquals(Result.WARN, threshold.evaluate(375));
    }

    @Test
    void evaluateFail() {
        LatencyThreshold threshold = new LatencyThreshold(95.0, 400, 350);
        assertEquals(Result.FAIL, threshold.evaluate(450));
    }

    @Test
    void evaluatePassWhenNoWarnThreshold() {
        LatencyThreshold threshold = new LatencyThreshold(95.0, 400, null);
        assertEquals(Result.PASS, threshold.evaluate(399));
    }
}
