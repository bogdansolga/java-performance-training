package net.safedata.performance.training.lab.contention;

import net.safedata.performance.training.RunProfiles;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(RunProfiles.LOCK_CONTENTION)
class LockContentionIntegrationTest {

    private static final int REQUESTS = 40;

    private final JsonMapper mapper = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    @Test
    void concurrentConversionsSerialiseOnOneLock() throws Exception {
        long start;
        // ExecutorService and HttpClient are AutoCloseable only from 19/21: no try-with-resources on 17
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            HttpClient client = HttpClient.newBuilder().executor(executor).build();
            List<CompletableFuture<HttpResponse<String>>> responses = new ArrayList<>();
            start = System.nanoTime();
            for (int i = 0; i < REQUESTS; i++) {
                responses.add(client.sendAsync(request("/lab/contention/convert/EUR?amount=10"),
                        HttpResponse.BodyHandlers.ofString()));
            }
            responses.forEach(response -> assertThat(response.join().statusCode()).isEqualTo(200));
        } finally {
            executor.shutdown();
        }
        // 40 conversions x 10 ms compliance check, serialised by the lock: never faster than 400 ms
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(400));

        HttpClient client = HttpClient.newHttpClient();
        JsonNode stats = mapper.readTree(client.send(request("/lab/contention/stats"),
                HttpResponse.BodyHandlers.ofString()).body());
        assertThat(stats.get("conversions").asLong()).isEqualTo(REQUESTS);
        assertThat(stats.get("httpThreadsBlockedCount").asLong()).isPositive();
    }

    private HttpRequest request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
    }
}
