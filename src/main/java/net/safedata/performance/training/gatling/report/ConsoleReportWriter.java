package net.safedata.performance.training.gatling.report;

import net.safedata.performance.training.gatling.analyzer.PercentileInterpolator.Accuracy;
import net.safedata.performance.training.gatling.model.*;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ConsoleReportWriter {

    private static final String DOUBLE_LINE = "═".repeat(70);
    private static final String SINGLE_LINE = "─".repeat(70);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final NumberFormat NUM_FMT = NumberFormat.getInstance(Locale.US);

    private final PrintStream out;

    public ConsoleReportWriter() {
        this(System.out);
    }

    public ConsoleReportWriter(PrintStream out) {
        this.out = out;
    }

    public void write(GatlingReport report, EvaluationResult result) {
        writeHeader(report);

        for (EndpointEvaluation endpoint : result.endpointEvaluations()) {
            writeEndpoint(endpoint, report);
        }

        writeSummary(result);
    }

    private void writeHeader(GatlingReport report) {
        out.println(DOUBLE_LINE);
        out.println("GATLING REPORT ANALYSIS");
        out.println(DOUBLE_LINE);
        out.printf("Report:   %s%n", report.runInfo().simulationName());
        out.printf("Date:     %s%n", report.runInfo().date().format(DATE_FMT));
        out.printf("Duration: %s%n", report.runInfo().duration());
        out.println();
    }

    private void writeEndpoint(EndpointEvaluation endpoint, GatlingReport report) {
        out.println(SINGLE_LINE);
        out.printf("ENDPOINT: %s%n", endpoint.endpointName());
        out.println(SINGLE_LINE);

        // Get request stats from report
        report.getEndpoint(endpoint.endpointName()).ifPresent(stats -> {
            out.printf("Requests: %s total | %.2f%% errors%n%n",
                NUM_FMT.format(stats.totalRequests()),
                stats.errorPercent());
        });

        writeLatencySection(endpoint);
        writeThroughputSection(endpoint);
        writeErrorRateSection(endpoint);

        out.printf("%nEndpoint Result: %s%n", formatResult(endpoint.result()));
    }

    private void writeLatencySection(EndpointEvaluation endpoint) {
        if (endpoint.latencyEvaluations().isEmpty()) {
            return;
        }

        out.println("LATENCY:");
        for (LatencyEvaluation eval : endpoint.latencyEvaluations()) {
            String icon = getResultIcon(eval.result());
            String percentileLabel = formatPercentile(eval.percentile());
            String valueStr = formatLatencyValue(eval.actualMs(), eval.accuracy());
            String limitStr = formatLimits(eval.limitMs(), eval.warnMs().orElse(null));
            String accuracyStr = formatAccuracy(eval.accuracy());

            out.printf("  %s %s  %s: %s %s %s%n",
                icon,
                formatResult(eval.result()),
                percentileLabel,
                valueStr,
                limitStr,
                accuracyStr);
        }
        out.println();
    }

    private void writeThroughputSection(EndpointEvaluation endpoint) {
        endpoint.throughput().ifPresent(eval -> {
            out.println("THROUGHPUT:");
            String icon = getResultIcon(eval.result());

            String warnStr = eval.warnTps()
                .map(w -> String.format(", warn: %.0f", w))
                .orElse("");

            out.printf("  %s %s  Avg TPS: %.0f req/s (target: %.0f%s)%n",
                icon,
                formatResult(eval.result()),
                eval.actualTps(),
                eval.targetTps(),
                warnStr);
            out.println();
        });
    }

    private void writeErrorRateSection(EndpointEvaluation endpoint) {
        endpoint.errorRate().ifPresent(eval -> {
            out.println("ERROR RATE:");
            String icon = getResultIcon(eval.result());

            String warnStr = eval.warnPercent()
                .map(w -> String.format(", warn: %.2f%%", w))
                .orElse("");

            out.printf("  %s %s  %.2f%% (limit: %.2f%%%s)%n",
                icon,
                formatResult(eval.result()),
                eval.actualPercent(),
                eval.limitPercent(),
                warnStr);
            out.println();
        });
    }

    private void writeSummary(EvaluationResult result) {
        out.println(DOUBLE_LINE);
        out.printf("OVERALL RESULT: %s%n", formatResult(result.overallResult()));
        out.printf("  - %d endpoint(s) analyzed%n", result.endpointEvaluations().size());
        out.printf("  - %d warning(s), %d failure(s)%n",
            result.warningCount(), result.failureCount());
        out.println(DOUBLE_LINE);
    }

    private String getResultIcon(Result result) {
        return switch (result) {
            case PASS -> "✓";
            case WARN -> "⚠";
            case FAIL -> "✗";
        };
    }

    private String formatResult(Result result) {
        return result.name();
    }

    private String formatPercentile(double percentile) {
        if (percentile == (int) percentile) {
            return String.format("p%d", (int) percentile);
        }
        return String.format("p%.2f", percentile).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private String formatLatencyValue(double value, Accuracy accuracy) {
        String prefix = accuracy == Accuracy.MEASURED ? "" : "~";
        return String.format("%s%.0fms", prefix, value);
    }

    private String formatLimits(int limitMs, Integer warnMs) {
        if (warnMs != null) {
            return String.format("(limit: %dms, warn: %dms)", limitMs, warnMs);
        }
        return String.format("(limit: %dms)", limitMs);
    }

    private String formatAccuracy(Accuracy accuracy) {
        return switch (accuracy) {
            case MEASURED -> "[measured]";
            case INTERPOLATED -> "[interpolated]";
            case EXTRAPOLATED -> "[extrapolated]";
        };
    }
}
