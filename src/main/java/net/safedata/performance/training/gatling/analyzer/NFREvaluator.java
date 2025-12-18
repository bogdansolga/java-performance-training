package net.safedata.performance.training.gatling.analyzer;

import net.safedata.performance.training.gatling.model.*;

import java.util.ArrayList;
import java.util.List;

public class NFREvaluator {

    private final PercentileInterpolator interpolator = new PercentileInterpolator();

    public EvaluationResult evaluate(GatlingReport report, NFRConfig nfrConfig) {
        List<EndpointEvaluation> evaluations = new ArrayList<>();

        for (EndpointNFR nfr : nfrConfig.endpoints()) {
            report.getEndpoint(nfr.endpointName())
                .ifPresent(stats -> evaluations.add(evaluateEndpoint(stats, nfr)));
        }

        return new EvaluationResult(evaluations);
    }

    private EndpointEvaluation evaluateEndpoint(EndpointStats stats, EndpointNFR nfr) {
        List<LatencyEvaluation> latencyEvals = evaluateLatencies(stats, nfr);
        ThroughputEvaluation throughputEval = evaluateThroughput(stats, nfr);
        ErrorRateEvaluation errorRateEval = evaluateErrorRate(stats, nfr);

        Result endpointResult = Result.PASS;
        for (LatencyEvaluation eval : latencyEvals) {
            endpointResult = endpointResult.worseOf(eval.result());
        }
        if (throughputEval != null) {
            endpointResult = endpointResult.worseOf(throughputEval.result());
        }
        if (errorRateEval != null) {
            endpointResult = endpointResult.worseOf(errorRateEval.result());
        }

        return new EndpointEvaluation(
            nfr.endpointName(),
            endpointResult,
            latencyEvals,
            throughputEval,
            errorRateEval
        );
    }

    private List<LatencyEvaluation> evaluateLatencies(EndpointStats stats, EndpointNFR nfr) {
        List<LatencyEvaluation> evaluations = new ArrayList<>();

        for (LatencyThreshold threshold : nfr.latencyThresholds()) {
            PercentileInterpolator.InterpolatedValue interpolated =
                interpolator.getPercentile(stats, threshold.percentile());

            Result result = threshold.evaluate(interpolated.value());

            evaluations.add(new LatencyEvaluation(
                threshold.percentile(),
                interpolated.value(),
                interpolated.accuracy(),
                threshold.maxMs(),
                threshold.warnMs().orElse(null),
                result
            ));
        }

        return evaluations;
    }

    private ThroughputEvaluation evaluateThroughput(EndpointStats stats, EndpointNFR nfr) {
        return nfr.throughput().map(threshold -> {
            // For PDF parsing, we only have average TPS, not min TPS
            // Use average as both for now
            Result result = threshold.evaluate(stats.requestsPerSecond(), stats.requestsPerSecond());

            return new ThroughputEvaluation(
                stats.requestsPerSecond(),
                threshold.targetTps(),
                threshold.warnTps().orElse(null),
                result
            );
        }).orElse(null);
    }

    private ErrorRateEvaluation evaluateErrorRate(EndpointStats stats, EndpointNFR nfr) {
        return nfr.errorRate().map(threshold -> {
            Result result = threshold.evaluate(stats.errorPercent());

            return new ErrorRateEvaluation(
                stats.errorPercent(),
                threshold.maxPercent(),
                threshold.warnPercent().orElse(null),
                result
            );
        }).orElse(null);
    }
}
