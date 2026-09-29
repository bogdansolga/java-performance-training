package net.safedata.performance.training.lab.harness;

import net.safedata.performance.training.gatling.model.EndpointStats;
import net.safedata.performance.training.gatling.parser.GatlingStatsParser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Records one lab run and prints the before/after comparison.
 * <pre>
 * LabReportMain --lab L --phase P --branch B --run-dir D --props F [--counter k=v]...
 * </pre>
 * Reads {@code D/gatling-*}/&lt;newest run&gt; (one Gatling run each) and the optional {@code D/stats.json}
 * (a flat JSON object of numbers), writes {@code D/summary.json}. The results root is three levels above D
 * ({@code <root>/<branch>/<lab>/<run>}). Exit 0, or 64 for invalid arguments.
 */
public final class LabReportMain {

    private LabReportMain() {
    }

    public static void main(String[] args) throws IOException {
        Map<String, String> options = new TreeMap<>();
        Map<String, Long> counters = new TreeMap<>();
        try {
            for (int i = 0; i < args.length; i += 2) {
                if (!args[i].startsWith("--") || i + 1 >= args.length) {
                    throw new IllegalArgumentException("Bad argument: " + args[i]);
                }
                String key = args[i].substring(2);
                String value = args[i + 1];
                if (key.equals("counter")) {
                    int eq = value.indexOf('=');
                    if (eq <= 0) {
                        throw new IllegalArgumentException("--counter expects name=value, got: " + value);
                    }
                    counters.put(value.substring(0, eq), Long.parseLong(value.substring(eq + 1).trim()));
                } else {
                    options.put(key, value);
                }
            }
            for (String required : List.of("lab", "phase", "branch", "run-dir", "props")) {
                if (!options.containsKey(required)) {
                    throw new IllegalArgumentException("Missing --" + required);
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("Usage: LabReportMain --lab L --phase P --branch B --run-dir D --props F [--counter k=v]...");
            System.exit(64);
            return;
        }

        Path runDir = Path.of(options.get("run-dir")).toAbsolutePath().normalize();
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(Path.of(options.get("props")))) {
            props.load(in);
        }

        readStatsJson(runDir.resolve("stats.json"), counters);
        List<RunMetrics> runs = readGatlingRuns(runDir);

        LabRunSummary summary = new LabRunSummary(options.get("lab"), options.get("phase"), options.get("branch"),
                LocalDateTime.now().toString(), System.getProperty("java.version"),
                props.getProperty("jvm.options", "n/a"), runs, counters);

        LabResultsStore store = new LabResultsStore(runDir.getParent().getParent().getParent());
        // looked up before this run is written, so a baseline run compares with the previous baseline
        var baseline = store.latest(options.get("branch"), options.get("lab"), "baseline");
        store.write(runDir, summary);
        System.out.print(LabComparison.render(baseline, summary, CountThreshold.fromProperties(props)));
    }

    private static void readStatsJson(Path file, Map<String, Long> counters) throws IOException {
        if (!Files.isRegularFile(file)) {
            return;
        }
        JsonNode node = JsonMapper.builder().build().readTree(Files.readString(file));
        for (var entry : node.properties()) {
            if (entry.getValue().isNumber()) {
                counters.putIfAbsent(entry.getKey(), entry.getValue().asLong());
            }
        }
    }

    private static List<RunMetrics> readGatlingRuns(Path runDir) throws IOException {
        List<Path> folders;
        try (Stream<Path> children = Files.list(runDir)) {
            folders = children.filter(d -> Files.isDirectory(d) && d.getFileName().toString().startsWith("gatling-"))
                    .sorted().toList();
        }
        List<RunMetrics> runs = new ArrayList<>();
        GatlingStatsParser parser = new GatlingStatsParser();
        for (Path folder : folders) {
            EndpointStats all = parser.parse(GatlingStatsParser.newestRunDir(folder)).getEndpoint("All Requests")
                    .orElseThrow(() -> new IOException("No 'All Requests' row in " + folder));
            runs.add(RunMetrics.from(all));
        }
        if (runs.isEmpty()) {
            throw new IOException("No gatling-* folders in " + runDir);
        }
        return runs;
    }
}
