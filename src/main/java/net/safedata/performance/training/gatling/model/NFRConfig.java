package net.safedata.performance.training.gatling.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record NFRConfig(List<EndpointNFR> endpoints) {

    public Optional<EndpointNFR> getEndpoint(String name) {
        return endpoints.stream()
            .filter(e -> e.endpointName().equals(name))
            .findFirst();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<EndpointNFR> endpoints = new ArrayList<>();

        public EndpointBuilder endpoint(String name) {
            return new EndpointBuilder(this, name);
        }

        private void addEndpoint(EndpointNFR endpoint) {
            endpoints.add(endpoint);
        }

        public NFRConfig build() {
            return new NFRConfig(List.copyOf(endpoints));
        }
    }

    public static class EndpointBuilder {
        private final Builder parent;
        private final String name;
        private final List<LatencyThreshold> latencyThresholds = new ArrayList<>();
        private ThroughputThreshold throughputThreshold;
        private ErrorRateThreshold errorRateThreshold;

        EndpointBuilder(Builder parent, String name) {
            this.parent = parent;
            this.name = name;
        }

        public EndpointBuilder latency(double percentile, int maxMs, Integer warnMs) {
            latencyThresholds.add(new LatencyThreshold(percentile, maxMs, warnMs));
            return this;
        }

        public EndpointBuilder throughput(double targetTps, Double sustainedMinTps, Double warnTps) {
            this.throughputThreshold = new ThroughputThreshold(targetTps, sustainedMinTps, warnTps);
            return this;
        }

        public EndpointBuilder errorRate(double maxPercent, Double warnPercent) {
            this.errorRateThreshold = new ErrorRateThreshold(maxPercent, warnPercent);
            return this;
        }

        public EndpointBuilder endpoint(String name) {
            finishCurrentEndpoint();
            return new EndpointBuilder(parent, name);
        }

        public NFRConfig build() {
            finishCurrentEndpoint();
            return parent.build();
        }

        private void finishCurrentEndpoint() {
            parent.addEndpoint(new EndpointNFR(
                name,
                List.copyOf(latencyThresholds),
                throughputThreshold,
                errorRateThreshold
            ));
        }
    }
}
