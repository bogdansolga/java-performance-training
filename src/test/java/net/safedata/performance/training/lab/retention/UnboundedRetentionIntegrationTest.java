package net.safedata.performance.training.lab.retention;

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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(RunProfiles.UNBOUNDED_RETENTION)
class UnboundedRetentionIntegrationTest {

    private static final int REQUESTS = 200;

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper mapper = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    @Test
    void everyQuoteIsRetainedForever() throws Exception {
        long before = retainedQuotes();

        for (int i = 0; i < REQUESTS; i++) {
            HttpResponse<String> response = get("/lab/retention/quote/SKU-" + i);
            assertThat(response.statusCode()).isEqualTo(200);
        }

        assertThat(retainedQuotes() - before).isEqualTo(REQUESTS);
    }

    private long retainedQuotes() throws Exception {
        HttpResponse<String> response = get("/lab/retention/stats");
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode stats = mapper.readTree(response.body());
        return stats.get("retainedQuotes").asLong();
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
