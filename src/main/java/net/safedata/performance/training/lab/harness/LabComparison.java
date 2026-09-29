package net.safedata.performance.training.lab.harness;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Renders the before/after table: {@code metric | baseline | now | limit | verdict}. */
public final class LabComparison {

    private static final String ROW = "%-28s | %14s | %14s | %10s | %s%n";

    private LabComparison() {
    }

    public static String render(Optional<LabRunSummary> baseline, LabRunSummary current, List<CountThreshold> thresholds) {
        StringBuilder out = new StringBuilder();
        if (baseline.isEmpty()) {
            if ("baseline".equals(current.phase())) {
                out.append(String.format("First baseline for lab %s on branch %s%n", current.lab(), current.branch()));
            } else {
                out.append(String.format("No baseline yet for lab %s on branch %s - run: labs/lab.sh %s baseline (Windows: labs\\lab.ps1 %s baseline)%n",
                        current.lab(), current.branch(), current.lab(), current.lab()));
            }
        } else {
            out.append(String.format("Lab %s on branch %s: baseline (%s) vs %s (%s)%n",
                    current.lab(), current.branch(), baseline.get().timestamp(), current.phase(), current.timestamp()));
        }
        out.append(String.format(ROW, "metric", "baseline", "now", "limit", "verdict"));

        RunMetrics now = current.median();
        Optional<RunMetrics> before = baseline.map(LabRunSummary::median);
        metricRow(out, "p50 (ms)", before.map(RunMetrics::p50Ms), now.p50Ms());
        metricRow(out, "p95 (ms)", before.map(RunMetrics::p95Ms), now.p95Ms());
        metricRow(out, "p99 (ms)", before.map(RunMetrics::p99Ms), now.p99Ms());
        metricRow(out, "throughput (req/s)", before.map(RunMetrics::requestsPerSecond), now.requestsPerSecond());
        metricRow(out, "errors (%)", before.map(RunMetrics::errorPercent), now.errorPercent());

        Map<String, Long> baselineCounters = baseline.map(LabRunSummary::counters).orElse(Map.of());
        List<String> names = new ArrayList<>(new LinkedHashMap<String, Long>(current.counters()).keySet());
        baselineCounters.keySet().stream().filter(k -> !names.contains(k)).forEach(names::add);
        thresholds.stream().map(CountThreshold::counter).filter(k -> !names.contains(k)).forEach(names::add);
        names.sort(null);
        for (String name : names) {
            Optional<CountThreshold> threshold = thresholds.stream().filter(t -> t.counter().equals(name)).findFirst();
            out.append(String.format(ROW, name,
                    baselineCounters.containsKey(name) ? baselineCounters.get(name).toString() : "-",
                    current.counters().containsKey(name) ? current.counters().get(name).toString() : "-",
                    threshold.map(t -> Long.toString(t.max())).orElse(""),
                    threshold.map(t -> t.passes(current.counters()) ? "PASS" : "FAIL").orElse("")));
        }
        return out.toString();
    }

    private static void metricRow(StringBuilder out, String name, Optional<Double> before, double now) {
        out.append(String.format(ROW, name, before.map(LabComparison::number).orElse("-"), number(now), "", ""));
    }

    private static String number(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
