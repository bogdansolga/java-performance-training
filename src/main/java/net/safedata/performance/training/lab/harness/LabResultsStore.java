package net.safedata.performance.training.lab.harness;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Stream;

/** Layout: {@code <root>/<branch>/<lab>/<yyyyMMdd-HHmmss>-<phase>/summary.json}. */
public final class LabResultsStore {

    static final String SUMMARY_FILE = "summary.json";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path resultsRoot;
    private final ObjectMapper mapper = JsonMapper.builder().build();

    public LabResultsStore(Path resultsRoot) {
        this.resultsRoot = resultsRoot;
    }

    public static String safeBranch(String branch) {
        return branch.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    public Path newRunDir(String branch, String lab, String phase, LocalDateTime now) {
        return resultsRoot.resolve(safeBranch(branch)).resolve(lab).resolve(now.format(STAMP) + "-" + phase);
    }

    public void write(Path runDir, LabRunSummary summary) {
        try {
            Files.createDirectories(runDir);
            mapper.writerWithDefaultPrettyPrinter().writeValue(runDir.resolve(SUMMARY_FILE), summary);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Optional<LabRunSummary> latest(String branch, String lab, String phase) {
        Path labDir = resultsRoot.resolve(safeBranch(branch)).resolve(lab);
        if (!Files.isDirectory(labDir)) {
            return Optional.empty();
        }
        try (Stream<Path> dirs = Files.list(labDir)) {
            return dirs.filter(d -> d.getFileName().toString().endsWith("-" + phase))
                    .filter(d -> Files.isRegularFile(d.resolve(SUMMARY_FILE)))
                    .max((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()))
                    .map(d -> mapper.readValue(d.resolve(SUMMARY_FILE).toFile(), LabRunSummary.class));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
