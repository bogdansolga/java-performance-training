package net.safedata.performance.training.gatling.model;

import java.util.List;
import java.util.Optional;

public record EndpointNFR(
    String endpointName,
    List<LatencyThreshold> latencyThresholds,
    ThroughputThreshold throughputThreshold,
    ErrorRateThreshold errorRateThreshold
) {
    public Optional<ThroughputThreshold> throughput() {
        return Optional.ofNullable(throughputThreshold);
    }

    public Optional<ErrorRateThreshold> errorRate() {
        return Optional.ofNullable(errorRateThreshold);
    }
}
