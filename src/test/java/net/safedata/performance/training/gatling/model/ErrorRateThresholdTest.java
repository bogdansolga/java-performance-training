package net.safedata.performance.training.gatling.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ErrorRateThresholdTest {

    @Test
    void evaluatePass() {
        ErrorRateThreshold threshold = new ErrorRateThreshold(0.1, 0.05);
        assertEquals(Result.PASS, threshold.evaluate(0.03));
    }

    @Test
    void evaluateWarn() {
        ErrorRateThreshold threshold = new ErrorRateThreshold(0.1, 0.05);
        assertEquals(Result.WARN, threshold.evaluate(0.07));
    }

    @Test
    void evaluateFail() {
        ErrorRateThreshold threshold = new ErrorRateThreshold(0.1, 0.05);
        assertEquals(Result.FAIL, threshold.evaluate(0.15));
    }

    @Test
    void evaluatePassWhenNoWarnThreshold() {
        ErrorRateThreshold threshold = new ErrorRateThreshold(0.1, null);
        assertEquals(Result.PASS, threshold.evaluate(0.09));
    }
}
