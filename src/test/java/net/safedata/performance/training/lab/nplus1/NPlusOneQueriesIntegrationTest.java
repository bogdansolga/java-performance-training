package net.safedata.performance.training.lab.nplus1;

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
@ActiveProfiles(RunProfiles.N_PLUS_ONE_QUERIES)
class NPlusOneQueriesIntegrationTest {

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper mapper = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    @Test
    void oneRequestIssuesOneQueryPerStoreAndPerSection() throws Exception {
        HttpResponse<String> stores = get("/lab/nplus1/stores");
        assertThat(stores.statusCode()).isEqualTo(200);
        JsonNode body = mapper.readTree(stores.body());
        assertThat(body).hasSize(20);
        body.forEach(store -> assertThat(store.get("items").asLong()).isEqualTo(50));

        HttpResponse<String> stats = get("/lab/nplus1/stats");
        assertThat(stats.statusCode()).isEqualTo(200);
        // 1 (stores) + 20 (sections of each store) + 100 (items of each section)
        assertThat(mapper.readTree(stats.body()).get("sqlStatementsPerRequest").asLong()).isEqualTo(121L);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
