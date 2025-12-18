package net.safedata.performance.training.gatling.model;

import net.safedata.performance.training.gatling.analyzer.PercentileInterpolator.Accuracy;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EvaluationResultTest {

    @Test
    void overallResultIsFailWhenAnyEndpointFails() {
        EndpointEvaluation ep1 = new EndpointEvaluation("ep1", Result.PASS, List.of(), null, null);
        EndpointEvaluation ep2 = new EndpointEvaluation("ep2", Result.FAIL, List.of(), null, null);

        EvaluationResult result = new EvaluationResult(List.of(ep1, ep2));

        assertEquals(Result.FAIL, result.overallResult());
    }

    @Test
    void overallResultIsWarnWhenNoFailButHasWarn() {
        EndpointEvaluation ep1 = new EndpointEvaluation("ep1", Result.PASS, List.of(), null, null);
        EndpointEvaluation ep2 = new EndpointEvaluation("ep2", Result.WARN, List.of(), null, null);

        EvaluationResult result = new EvaluationResult(List.of(ep1, ep2));

        assertEquals(Result.WARN, result.overallResult());
    }

    @Test
    void overallResultIsPassWhenAllPass() {
        EndpointEvaluation ep1 = new EndpointEvaluation("ep1", Result.PASS, List.of(), null, null);
        EndpointEvaluation ep2 = new EndpointEvaluation("ep2", Result.PASS, List.of(), null, null);

        EvaluationResult result = new EvaluationResult(List.of(ep1, ep2));

        assertEquals(Result.PASS, result.overallResult());
    }

    @Test
    void latencyEvaluationHoldsAllFields() {
        LatencyEvaluation eval = new LatencyEvaluation(
            95.0, 21.0, Accuracy.MEASURED, 400, 350, Result.PASS
        );

        assertEquals(95.0, eval.percentile());
        assertEquals(21.0, eval.actualMs());
        assertEquals(Accuracy.MEASURED, eval.accuracy());
        assertEquals(400, eval.limitMs());
        assertEquals(350, eval.warnMs().orElse(-1));
        assertEquals(Result.PASS, eval.result());
    }
}
