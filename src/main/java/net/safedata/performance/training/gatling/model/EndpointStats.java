package net.safedata.performance.training.gatling.model;

import java.util.Map;
import java.util.Optional;

public record EndpointStats(
    String name,
    long totalRequests,
    long okCount,
    long koCount,
    double errorPercent,
    double requestsPerSecond,
    Map<Integer, Integer> percentiles
) {
    public Optional<Integer> getPercentile(int p) {
        return Optional.ofNullable(percentiles.get(p));
    }
}
