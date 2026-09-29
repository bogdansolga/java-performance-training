package net.safedata.performance.training.gatling.parser;

import net.safedata.performance.training.gatling.model.EndpointStats;
import net.safedata.performance.training.gatling.model.GatlingReport;
import net.safedata.performance.training.gatling.model.RunInfo;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Reads a Gatling run directory (Gatling 3.13.x layout) into a {@link GatlingReport}.
 * <p>
 * The numbers come from {@code js/stats.js}: a JavaScript file starting with {@code var stats = {...}}
 * (unquoted keys, numbers as strings, {@code "-"} for "no data"), followed by UI helper functions.
 */
public class GatlingStatsParser {

    static final String ALL_REQUESTS = "All Requests";
    private static final String STATS_FILE = "js/stats.js";
    private static final String PREFIX = "var stats = ";
    private static final DateTimeFormatter RUN_DIR_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final int[] PERCENTILES = {50, 75, 95, 99};

    private final JsonMapper mapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
            .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    public GatlingReport parse(Path runDir) throws IOException {
        Path statsFile = runDir.resolve(STATS_FILE);
        String content = Files.readString(statsFile);
        int start = content.indexOf(PREFIX);
        if (start < 0) {
            throw new IOException("Not a Gatling stats.js (no '" + PREFIX.trim() + "'): " + statsFile);
        }
        // trailing tokens (the JS helper functions) are ignored: only the first value is read
        JsonNode root = mapper.readTree(content.substring(start + PREFIX.length()));

        List<EndpointStats> endpoints = new ArrayList<>();
        endpoints.add(toEndpoint(ALL_REQUESTS, root.path("stats")));
        collectRequests(root.path("contents"), endpoints);
        return new GatlingReport(parseRunInfo(runDir), endpoints);
    }

    /** The lexically greatest sub-directory of {@code resultsFolder} that contains {@code js/stats.js}. */
    public static Path newestRunDir(Path resultsFolder) throws IOException {
        try (Stream<Path> dirs = Files.list(resultsFolder)) {
            return dirs.filter(d -> Files.isRegularFile(d.resolve(STATS_FILE)))
                    .max((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()))
                    .orElseThrow(() -> new IOException("No Gatling run directory (with " + STATS_FILE + ") in " + resultsFolder));
        }
    }

    private void collectRequests(JsonNode contents, List<EndpointStats> out) {
        for (JsonNode entry : contents.values()) {
            String type = entry.path("type").asString("");
            if ("REQUEST".equals(type)) {
                out.add(toEndpoint(entry.path("name").asString(), entry.path("stats")));
            } else if ("GROUP".equals(type)) {
                collectRequests(entry.path("contents"), out);
            }
        }
    }

    private EndpointStats toEndpoint(String name, JsonNode stats) {
        long total = (long) number(stats.path("numberOfRequests").path("total"));
        long ok = (long) number(stats.path("numberOfRequests").path("ok"));
        long ko = (long) number(stats.path("numberOfRequests").path("ko"));
        double errorPercent = total > 0 ? ko * 100.0 / total : 0.0;
        double rps = number(stats.path("meanNumberOfRequestsPerSecond").path("total"));
        Map<Integer, Integer> percentiles = new LinkedHashMap<>();
        for (int i = 0; i < PERCENTILES.length; i++) {
            percentiles.put(PERCENTILES[i], (int) number(stats.path("percentiles" + (i + 1)).path("total")));
        }
        return new EndpointStats(name, total, ok, ko, errorPercent, rps, percentiles);
    }

    /** Gatling writes numbers as strings, and "-" when there is no data. */
    private static double number(JsonNode node) {
        if (node.isNumber()) {
            return node.asDouble();
        }
        String text = node.asString("-").trim();
        if (text.isEmpty() || "-".equals(text)) {
            return 0;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Run directories are named {@code <simulation>-<yyyyMMddHHmmssSSS>}, the timestamp in UTC. */
    private static RunInfo parseRunInfo(Path runDir) {
        String dir = runDir.toAbsolutePath().normalize().getFileName().toString();
        int dash = dir.lastIndexOf('-');
        String name = dir;
        LocalDateTime date = LocalDateTime.now(java.time.ZoneOffset.UTC);
        if (dash > 0) {
            try {
                date = LocalDateTime.parse(dir.substring(dash + 1), RUN_DIR_DATE);
                name = dir.substring(0, dash);
            } catch (DateTimeParseException ignored) {
                // not a Gatling-named directory: keep the directory name and the current time
            }
        }
        return new RunInfo(name, date, "n/a", "");
    }
}
