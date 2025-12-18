package net.safedata.performance.training.gatling.parser;

import net.safedata.performance.training.gatling.model.EndpointStats;
import net.safedata.performance.training.gatling.model.GatlingReport;
import net.safedata.performance.training.gatling.model.RunInfo;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GatlingPdfParser {

    private static final Pattern SIMULATION_NAME_PATTERN =
        Pattern.compile("^([A-Za-z0-9]+Simulation)$", Pattern.MULTILINE);

    private static final Pattern DATE_PATTERN =
        Pattern.compile("Date:\\s*(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})");

    private static final Pattern DURATION_PATTERN =
        Pattern.compile("Duration:\\s*([\\d]+m\\s*[\\d]*s?)");

    private static final Pattern DESCRIPTION_PATTERN =
        Pattern.compile("Description:\\s*(.+?)(?=\\n|Expand)");

    // Pattern for endpoint stats rows
    // Format: endpoint-name total ok ko %ko cnt/s min p50 p75 p95 p99 max mean stddev
    // Looking at PDF: surepay-verify-account 850730 850730 0 0% 942.115 5 11 12 15 21 444 11 4
    private static final Pattern ENDPOINT_STATS_PATTERN =
        Pattern.compile("([a-z][a-z0-9-]+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+\\d+%?\\s+([\\d.]+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)");

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public GatlingReport parse(Path pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            RunInfo runInfo = parseRunInfo(text);
            List<EndpointStats> endpoints = parseEndpoints(text);

            return new GatlingReport(runInfo, endpoints);
        }
    }

    private RunInfo parseRunInfo(String text) {
        String simulationName = extractPattern(SIMULATION_NAME_PATTERN, text, 1)
            .orElse("Unknown");

        LocalDateTime date = extractPattern(DATE_PATTERN, text, 1)
            .map(s -> s.replace(" GMT", "").trim())
            .map(s -> LocalDateTime.parse(s, DATE_FORMATTER))
            .orElse(LocalDateTime.now());

        String duration = extractPattern(DURATION_PATTERN, text, 1)
            .orElse("Unknown");

        String description = extractPattern(DESCRIPTION_PATTERN, text, 1)
            .orElse("");

        return new RunInfo(simulationName, date, duration, description);
    }

    private List<EndpointStats> parseEndpoints(String text) {
        List<EndpointStats> endpoints = new ArrayList<>();

        // Clean up text - remove extra whitespace while preserving structure
        String cleanedText = text.replaceAll("\\s+", " ");

        Matcher matcher = ENDPOINT_STATS_PATTERN.matcher(cleanedText);

        while (matcher.find()) {
            String name = matcher.group(1);

            // Skip "All Requests" aggregate row and header-like matches
            if (name.equals("all") || name.equals("requests") || name.equals("pct") ||
                name.equals("equests") || name.contains("vop")) {
                continue;
            }

            try {
                long total = Long.parseLong(matcher.group(2));
                long ok = Long.parseLong(matcher.group(3));
                long ko = Long.parseLong(matcher.group(4));
                double errorPercent = ko > 0 ? (ko * 100.0 / total) : 0.0;
                double tps = Double.parseDouble(matcher.group(5));
                int min = Integer.parseInt(matcher.group(6));
                int p50 = Integer.parseInt(matcher.group(7));
                int p75 = Integer.parseInt(matcher.group(8));
                int p95 = Integer.parseInt(matcher.group(9));
                int p99 = Integer.parseInt(matcher.group(10));
                int max = Integer.parseInt(matcher.group(11));

                Map<Integer, Integer> percentiles = Map.of(
                    50, p50,
                    75, p75,
                    95, p95,
                    99, p99
                );

                endpoints.add(new EndpointStats(name, total, ok, ko, errorPercent, tps, percentiles));
            } catch (NumberFormatException e) {
                // Skip malformed rows
            }
        }

        return endpoints;
    }

    private Optional<String> extractPattern(Pattern pattern, String text, int group) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Optional.of(matcher.group(group));
        }
        return Optional.empty();
    }
}
