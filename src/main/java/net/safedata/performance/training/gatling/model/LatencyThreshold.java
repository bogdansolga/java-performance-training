package net.safedata.performance.training.gatling.model;

import java.util.Optional;

public record LatencyThreshold(double percentile, int maxMs, Integer warnMsThreshold) {

    public Optional<Integer> warnMs() {
        return Optional.ofNullable(warnMsThreshold);
    }

    public Result evaluate(double actualMs) {
        if (actualMs > maxMs) {
            return Result.FAIL;
        }
        if (warnMsThreshold != null && actualMs >= warnMsThreshold) {
            return Result.WARN;
        }
        return Result.PASS;
    }
}

