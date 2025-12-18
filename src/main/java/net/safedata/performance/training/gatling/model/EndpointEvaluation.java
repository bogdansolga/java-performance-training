package net.safedata.performance.training.gatling.model;

import java.util.List;
import java.util.Optional;

public record EndpointEvaluation(
    String endpointName,
    Result result,
    List<LatencyEvaluation> latencyEvaluations,
    ThroughputEvaluation throughputEvaluation,
    ErrorRateEvaluation errorRateEvaluation
) {
    public Optional<ThroughputEvaluation> throughput() {
        return Optional.ofNullable(throughputEvaluation);
    }

    public Optional<ErrorRateEvaluation> errorRate() {
        return Optional.ofNullable(errorRateEvaluation);
    }
}
