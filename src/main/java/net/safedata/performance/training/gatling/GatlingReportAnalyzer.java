package net.safedata.performance.training.gatling;

import net.safedata.performance.training.gatling.analyzer.NFREvaluator;
import net.safedata.performance.training.gatling.model.EvaluationResult;
import net.safedata.performance.training.gatling.model.GatlingReport;
import net.safedata.performance.training.gatling.model.NFRConfig;
import net.safedata.performance.training.gatling.parser.GatlingPdfParser;
import net.safedata.performance.training.gatling.report.ConsoleReportWriter;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

public class GatlingReportAnalyzer {

    private final GatlingPdfParser parser;
    private final NFREvaluator evaluator;
    private final ConsoleReportWriter writer;

    public GatlingReportAnalyzer() {
        this(System.out);
    }

    public GatlingReportAnalyzer(PrintStream out) {
        this.parser = new GatlingPdfParser();
        this.evaluator = new NFREvaluator();
        this.writer = new ConsoleReportWriter(out);
    }

    public EvaluationResult analyze(Path pdfPath, NFRConfig nfrConfig) throws IOException {
        GatlingReport report = parser.parse(pdfPath);
        EvaluationResult result = evaluator.evaluate(report, nfrConfig);
        writer.write(report, result);
        return result;
    }

    // Static convenience method
    public static EvaluationResult analyze(Path pdfPath, NFRConfig nfrConfig, PrintStream out) throws IOException {
        return new GatlingReportAnalyzer(out).analyze(pdfPath, nfrConfig);
    }

    public static EvaluationResult analyze(String pdfPath, NFRConfig nfrConfig) throws IOException {
        return new GatlingReportAnalyzer().analyze(Path.of(pdfPath), nfrConfig);
    }
}
