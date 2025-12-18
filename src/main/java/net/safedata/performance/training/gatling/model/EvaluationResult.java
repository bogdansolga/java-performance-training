package net.safedata.performance.training.gatling.model;

import java.util.List;

public record EvaluationResult(List<EndpointEvaluation> endpointEvaluations) {

    public Result overallResult() {
        return endpointEvaluations.stream()
            .map(EndpointEvaluation::result)
            .reduce(Result.PASS, Result::worseOf);
    }

    public long failureCount() {
        return endpointEvaluations.stream()
            .filter(e -> e.result() == Result.FAIL)
            .count();
    }

    public long warningCount() {
        return endpointEvaluations.stream()
            .filter(e -> e.result() == Result.WARN)
            .count();
    }
}
