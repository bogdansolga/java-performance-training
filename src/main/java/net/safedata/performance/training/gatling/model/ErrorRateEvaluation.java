package net.safedata.performance.training.gatling.model;

import java.util.Optional;

public record ErrorRateEvaluation(
    double actualPercent,
    double limitPercent,
    Double warnPercentThreshold,
    Result result
) {
    public Optional<Double> warnPercent() {
        return Optional.ofNullable(warnPercentThreshold);
    }
}
