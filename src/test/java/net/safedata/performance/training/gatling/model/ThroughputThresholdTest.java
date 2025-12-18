package net.safedata.performance.training.gatling.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ThroughputThresholdTest {

    @Test
    void createWithAllParameters() {
        ThroughputThreshold threshold = new ThroughputThreshold(1500.0, 1350.0, 1400.0);
        assertEquals(1500.0, threshold.targetTps());
        assertEquals(1350.0, threshold.sustainedMinTps().orElse(-1.0));
        assertEquals(1400.0, threshold.warnTps().orElse(-1.0));
    }

    @Test
    void evaluatePass() {
        ThroughputThreshold threshold = new ThroughputThreshold(1500.0, 1350.0, 1400.0);
        assertEquals(Result.PASS, threshold.evaluate(1600.0, 1400.0));
    }

    @Test
    void evaluateWarnBelowWarnThreshold() {
        ThroughputThreshold threshold = new ThroughputThreshold(1500.0, 1350.0, 1400.0);
        assertEquals(Result.WARN, threshold.evaluate(1550.0, 1380.0));
    }

    @Test
    void evaluateFailBelowTarget() {
        ThroughputThreshold threshold = new ThroughputThreshold(1500.0, 1350.0, 1400.0);
        assertEquals(Result.FAIL, threshold.evaluate(1400.0, 1300.0));
    }

    @Test
    void evaluateFailBelowSustained() {
        ThroughputThreshold threshold = new ThroughputThreshold(1500.0, 1350.0, 1400.0);
        assertEquals(Result.FAIL, threshold.evaluate(1600.0, 1200.0));
    }

    @Test
    void evaluateWithoutSustainedCheck() {
        ThroughputThreshold threshold = new ThroughputThreshold(1500.0, null, null);
        assertEquals(Result.PASS, threshold.evaluate(1500.0, 100.0));
    }
}
