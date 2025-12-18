package net.safedata.performance.training.gatling.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NFRConfigTest {

    @Test
    void buildConfigWithFluentApi() {
        NFRConfig config = NFRConfig.builder()
            .endpoint("surepay-verify-account")
                .latency(90.0, 150, 120)
                .latency(95.0, 400, 350)
                .latency(99.99, 1000, null)
                .throughput(1500.0, 1350.0, 1400.0)
                .errorRate(0.1, 0.05)
            .endpoint("eba-payee-information")
                .latency(95.0, 500, null)
            .build();

        assertEquals(2, config.endpoints().size());

        EndpointNFR surepay = config.getEndpoint("surepay-verify-account").orElseThrow();
        assertEquals(3, surepay.latencyThresholds().size());
        assertTrue(surepay.throughput().isPresent());
        assertTrue(surepay.errorRate().isPresent());

        EndpointNFR eba = config.getEndpoint("eba-payee-information").orElseThrow();
        assertEquals(1, eba.latencyThresholds().size());
        assertTrue(eba.throughput().isEmpty());
    }

    @Test
    void getEndpointReturnsEmptyForUnknown() {
        NFRConfig config = NFRConfig.builder()
            .endpoint("known")
                .latency(95.0, 400, null)
            .build();

        assertTrue(config.getEndpoint("unknown").isEmpty());
    }
}
