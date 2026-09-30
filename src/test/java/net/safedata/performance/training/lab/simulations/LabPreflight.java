package net.safedata.performance.training.lab.simulations;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Stops a lab run before the load starts when the application is not running with the lab's profile. */
final class LabPreflight {

    private LabPreflight() {
    }

    static void requireLab(String baseUrl, String statsPath, String profile) {
        int status;
        try {
            status = HttpClient.newHttpClient()
                    .send(HttpRequest.newBuilder(URI.create(baseUrl + statsPath)).GET().build(),
                            HttpResponse.BodyHandlers.discarding())
                    .statusCode();
        } catch (IOException e) {
            throw new IllegalStateException("No application at " + baseUrl + " - start it first (docs/gatling-labs.md)");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while checking " + baseUrl, e);
        }
        if (status == 404) {
            throw new IllegalStateException("Lab not found at " + baseUrl
                    + " - start the application with --spring.profiles.active=" + profile);
        }
    }
}
