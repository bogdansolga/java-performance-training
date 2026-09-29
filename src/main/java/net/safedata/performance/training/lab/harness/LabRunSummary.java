package net.safedata.performance.training.lab.harness;

import java.util.List;
import java.util.Map;

/** Everything recorded about one lab run (one phase, one or more Gatling runs, plus the counters). */
public record LabRunSummary(String lab, String phase, String branch, String timestamp, String javaVersion,
                            String jvmOptions, List<RunMetrics> runs, Map<String, Long> counters) {

    public RunMetrics median() {
        return RunMetrics.median(runs);
    }
}
