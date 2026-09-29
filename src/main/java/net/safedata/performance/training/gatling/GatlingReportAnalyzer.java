package net.safedata.performance.training.gatling;

import net.safedata.performance.training.gatling.analyzer.NFREvaluator;
import net.safedata.performance.training.gatling.model.EvaluationResult;
import net.safedata.performance.training.gatling.model.GatlingReport;
import net.safedata.performance.training.gatling.model.NFRConfig;
import net.safedata.performance.training.gatling.parser.GatlingStatsParser;
import net.safedata.performance.training.gatling.report.ConsoleReportWriter;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

public class GatlingReportAnalyzer {

    private final GatlingStatsParser parser;
    private final NFREvaluator evaluator;
    private final ConsoleReportWriter writer;

    public GatlingReportAnalyzer() {
        this(System.out);
    }

    public GatlingReportAnalyzer(PrintStream out) {
        this.parser = new GatlingStatsParser();
        this.evaluator = new NFREvaluator();
        this.writer = new ConsoleReportWriter(out);
    }

    public EvaluationResult analyze(Path runDir, NFRConfig nfrConfig) throws IOException {
        GatlingReport report = parser.parse(runDir);
        EvaluationResult result = evaluator.evaluate(report, nfrConfig);
        writer.write(report, result);
        return result;
    }

    /** @param runDir a Gatling run directory (containing js/stats.js) */
    // Static convenience method
    public static EvaluationResult analyze(Path runDir, NFRConfig nfrConfig, PrintStream out) throws IOException {
        return new GatlingReportAnalyzer(out).analyze(runDir, nfrConfig);
    }

    public static EvaluationResult analyze(String runDir, NFRConfig nfrConfig) throws IOException {
        return new GatlingReportAnalyzer().analyze(Path.of(runDir), nfrConfig);
    }
}
