package net.safedata.performance.training.gatling.model;

import java.util.Optional;

public record ThroughputEvaluation(
    double actualTps,
    double targetTps,
    Double warnTpsThreshold,
    Result result
) {
    public Optional<Double> warnTps() {
        return Optional.ofNullable(warnTpsThreshold);
    }
}
