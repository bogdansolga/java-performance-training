package net.safedata.performance.training.analysis.parser;

import net.safedata.performance.training.analysis.model.EndpointStats;
import net.safedata.performance.training.analysis.model.GatlingReport;
import net.safedata.performance.training.analysis.model.RunInfo;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads a Gatling run directory into a {@link GatlingReport}.
 * <p>
 * Gatling up to 3.13 keeps the numbers in {@code js/stats.js} ({@code var stats = {...}}: unquoted keys,
 * numbers as strings, {@code "-"} for "no data"). From 3.14 that file holds only UI code, and the numbers
 * are read from the statistics table in {@code index.html} instead.
 */
public class GatlingStatsParser {

    static final String ALL_REQUESTS = "All Requests";
    private static final String STATS_FILE = "js/stats.js";
    private static final String REPORT_FILE = "index.html";
    // One statistics row of index.html: the request name, then the value cells col-2 .. col-14
    private static final Pattern STATS_ROW = Pattern.compile(
            "<tr id=\"[^\"]*\"\\s*>.*?class=\"ellipsed-name\">(.*?)</span>(.*?)</tr>", Pattern.DOTALL);
    private static final Pattern VALUE_CELL = Pattern.compile("<td class=\"value [a-z]+ col-(\\d+)\">([^<]*)</td>");
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
            return parseReportTable(runDir);
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

    /** Gatling 3.14+: the statistics table of index.html (columns: total, ok, ko, % ko, req/s, min, p50, p75, p95, p99, ...). */
    private GatlingReport parseReportTable(Path runDir) throws IOException {
        Path reportFile = runDir.resolve(REPORT_FILE);
        String html = Files.readString(reportFile);
        List<EndpointStats> endpoints = new ArrayList<>();
        Matcher row = STATS_ROW.matcher(html);
        while (row.find()) {
            Map<Integer, String> cells = new LinkedHashMap<>();
            Matcher cell = VALUE_CELL.matcher(row.group(2));
            while (cell.find()) {
                cells.put(Integer.parseInt(cell.group(1)), cell.group(2));
            }
            if (cells.size() < 11) {
                continue;
            }
            long total = (long) number(cells.get(2));
            long ko = (long) number(cells.get(4));
            Map<Integer, Integer> percentiles = new LinkedHashMap<>();
            for (int i = 0; i < PERCENTILES.length; i++) {
                percentiles.put(PERCENTILES[i], (int) number(cells.get(8 + i)));
            }
            endpoints.add(new EndpointStats(row.group(1).trim(), total, (long) number(cells.get(3)), ko,
                    total > 0 ? ko * 100.0 / total : 0.0, number(cells.get(6)), percentiles));
        }
        if (endpoints.isEmpty()) {
            throw new IOException("No statistics found in " + reportFile + " or " + runDir.resolve(STATS_FILE));
        }
        return new GatlingReport(parseRunInfo(runDir), endpoints);
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
        return number(node.asString("-"));
    }

    private static double number(String raw) {
        String text = raw == null ? "-" : raw.trim();
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
