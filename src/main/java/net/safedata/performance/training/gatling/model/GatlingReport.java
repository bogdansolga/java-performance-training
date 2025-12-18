package net.safedata.performance.training.gatling.model;

import java.util.List;
import java.util.Optional;

public record GatlingReport(
    RunInfo runInfo,
    List<EndpointStats> endpoints
) {
    public Optional<EndpointStats> getEndpoint(String name) {
        return endpoints.stream()
            .filter(e -> e.name().equals(name))
            .findFirst();
    }
}
