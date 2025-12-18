package net.safedata.performance.training.gatling.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void passIsMoreSevereThanPass() {
        assertEquals(Result.PASS, Result.PASS.worseOf(Result.PASS));
    }

    @Test
    void warnIsMoreSevereThanPass() {
        assertEquals(Result.WARN, Result.PASS.worseOf(Result.WARN));
        assertEquals(Result.WARN, Result.WARN.worseOf(Result.PASS));
    }

    @Test
    void failIsMoreSevereThanWarn() {
        assertEquals(Result.FAIL, Result.WARN.worseOf(Result.FAIL));
        assertEquals(Result.FAIL, Result.FAIL.worseOf(Result.WARN));
    }

    @Test
    void failIsMoreSevereThanPass() {
        assertEquals(Result.FAIL, Result.PASS.worseOf(Result.FAIL));
    }
}
