package net.safedata.performance.training.lab.harness;

import net.safedata.performance.training.analysis.model.EndpointStats;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Headline numbers of one Gatling run. */
public record RunMetrics(double p50Ms, double p95Ms, double p99Ms, double requestsPerSecond, double errorPercent) {

    public static RunMetrics from(EndpointStats all) {
        return new RunMetrics(
                all.getPercentile(50).orElse(0),
                all.getPercentile(95).orElse(0),
                all.getPercentile(99).orElse(0),
                all.requestsPerSecond(),
                all.errorPercent());
    }

    /** Per-field median (the mean of the two middle values for an even number of runs). */
    public static RunMetrics median(List<RunMetrics> runs) {
        if (runs.isEmpty()) {
            throw new IllegalArgumentException("No runs to take the median of");
        }
        return new RunMetrics(
                median(runs, RunMetrics::p50Ms),
                median(runs, RunMetrics::p95Ms),
                median(runs, RunMetrics::p99Ms),
                median(runs, RunMetrics::requestsPerSecond),
                median(runs, RunMetrics::errorPercent));
    }

    private static double median(List<RunMetrics> runs, ToDoubleFunction<RunMetrics> field) {
        double[] sorted = runs.stream().mapToDouble(field).sorted().toArray();
        int n = sorted.length;
        return n % 2 == 1 ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2;
    }
}
