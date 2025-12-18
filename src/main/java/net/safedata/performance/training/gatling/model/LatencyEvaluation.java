package net.safedata.performance.training.gatling.model;

import net.safedata.performance.training.gatling.analyzer.PercentileInterpolator.Accuracy;
import java.util.Optional;

public record LatencyEvaluation(
    double percentile,
    double actualMs,
    Accuracy accuracy,
    int limitMs,
    Integer warnMsThreshold,
    Result result
) {
    public Optional<Integer> warnMs() {
        return Optional.ofNullable(warnMsThreshold);
    }
}
