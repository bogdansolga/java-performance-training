package net.safedata.performance.training.gatling.model;

import java.util.Optional;

public record ErrorRateThreshold(double maxPercent, Double warnPercentThreshold) {

    public Optional<Double> warnPercent() {
        return Optional.ofNullable(warnPercentThreshold);
    }

    public Result evaluate(double actualPercent) {
        if (actualPercent > maxPercent) {
            return Result.FAIL;
        }
        if (warnPercentThreshold != null && actualPercent >= warnPercentThreshold) {
            return Result.WARN;
        }
        return Result.PASS;
    }
}
