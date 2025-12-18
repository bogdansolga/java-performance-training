package net.safedata.performance.training.gatling.model;

import java.util.Optional;

public record ThroughputThreshold(double targetTps, Double sustainedMinTpsThreshold, Double warnTpsThreshold) {

    public Optional<Double> sustainedMinTps() {
        return Optional.ofNullable(sustainedMinTpsThreshold);
    }

    public Optional<Double> warnTps() {
        return Optional.ofNullable(warnTpsThreshold);
    }

    public Result evaluate(double avgTps, double minTps) {
        // Check hard failures first
        if (avgTps < targetTps) {
            return Result.FAIL;
        }
        if (sustainedMinTpsThreshold != null && minTps < sustainedMinTpsThreshold) {
            return Result.FAIL;
        }
        // Check warnings - if minTps is below warnTps threshold (warning level for sustained throughput)
        if (warnTpsThreshold != null && minTps < warnTpsThreshold) {
            return Result.WARN;
        }
        return Result.PASS;
    }
}
