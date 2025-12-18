package net.safedata.performance.training.gatling.analyzer;

import net.safedata.performance.training.gatling.model.EndpointStats;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class PercentileInterpolator {

    public enum Accuracy {
        MEASURED,
        INTERPOLATED,
        EXTRAPOLATED
    }

    public record InterpolatedValue(double value, Accuracy accuracy) {}

    public InterpolatedValue getPercentile(EndpointStats stats, double targetPercentile) {
        Map<Integer, Integer> percentiles = stats.percentiles();

        // Check if we have the exact percentile
        int targetInt = (int) targetPercentile;
        if (targetPercentile == targetInt && percentiles.containsKey(targetInt)) {
            return new InterpolatedValue(percentiles.get(targetInt), Accuracy.MEASURED);
        }

        NavigableMap<Integer, Integer> sorted = new TreeMap<>(percentiles);

        Integer lowerKey = sorted.floorKey((int) targetPercentile);
        Integer upperKey = sorted.ceilingKey((int) Math.ceil(targetPercentile));

        // Handle extrapolation beyond known range
        if (upperKey == null || targetPercentile > sorted.lastKey()) {
            return extrapolateBeyond(sorted, targetPercentile);
        }

        if (lowerKey == null) {
            return extrapolateBelow(sorted, targetPercentile);
        }

        // Interpolate between two known points
        double lowerValue = sorted.get(lowerKey);
        double upperValue = sorted.get(upperKey);

        double ratio = (targetPercentile - lowerKey) / (upperKey - lowerKey);
        double interpolated = lowerValue + (upperValue - lowerValue) * ratio;

        return new InterpolatedValue(interpolated, Accuracy.INTERPOLATED);
    }

    private InterpolatedValue extrapolateBeyond(NavigableMap<Integer, Integer> sorted, double targetPercentile) {
        // Use log-linear extrapolation from the last two known percentiles
        Integer p95Key = sorted.floorKey(95);
        Integer p99Key = sorted.floorKey(99);

        if (p95Key == null || p99Key == null || p95Key.equals(p99Key)) {
            // Fall back to linear extrapolation from last two points
            var entries = sorted.descendingMap().entrySet().iterator();
            var last = entries.next();
            var secondLast = entries.next();

            double slope = (double)(last.getValue() - secondLast.getValue()) /
                          (last.getKey() - secondLast.getKey());
            double extrapolated = last.getValue() + slope * (targetPercentile - last.getKey());
            return new InterpolatedValue(extrapolated, Accuracy.EXTRAPOLATED);
        }

        double p95Value = sorted.get(p95Key);
        double p99Value = sorted.get(p99Key);

        // Log-linear extrapolation: p_target = p99 * (p99/p95)^((target - 99) / (99 - 95))
        double ratio = p99Value / p95Value;
        double exponent = (targetPercentile - p99Key) / (p99Key - p95Key);
        double extrapolated = p99Value * Math.pow(ratio, exponent);

        return new InterpolatedValue(extrapolated, Accuracy.EXTRAPOLATED);
    }

    private InterpolatedValue extrapolateBelow(NavigableMap<Integer, Integer> sorted, double targetPercentile) {
        var entries = sorted.entrySet().iterator();
        var first = entries.next();
        var second = entries.next();

        double slope = (double)(second.getValue() - first.getValue()) /
                      (second.getKey() - first.getKey());
        double extrapolated = first.getValue() - slope * (first.getKey() - targetPercentile);

        return new InterpolatedValue(Math.max(0, extrapolated), Accuracy.EXTRAPOLATED);
    }
}
