package com.tripplanner.evaluation;

import java.time.Instant;
import java.util.List;

/**
 * One evaluated run of the trip planner for a single sample.
 *
 * @param sampleId    the sample name (stable across repeated experiments)
 * @param input       the request parameters the planner was run with
 * @param output      the saved plan text (TripPlanText rendering), produced once
 * @param evidence    supporting evidence captured during the run (e.g. tool results)
 * @param traceId     the 32-hex OpenTelemetry trace id of the planning run, captured
 *                    while the root span was current; used to attach scores in Langfuse
 * @param model       the chat model used for the run (class name), for comparability
 * @param strategies  per-strategy results for the saved output
 * @param startedAt   when the run started
 */
public record EvaluationRun(
        String sampleId,
        List<String> input,
        String output,
        List<String> evidence,
        String traceId,
        String model,
        List<StrategyOutcome> strategies,
        Instant startedAt) {

    /**
     * The outcome of applying one evaluation strategy to the saved output.
     */
    public record StrategyOutcome(
            String strategy,
            boolean passed,
            double score,
            String reason,
            java.util.Map<String, Object> metadata) {
    }

    /**
     * Mean strategy score, clamped to 0..1; 1.0 when no strategy ran.
     */
    public double aggregateScore() {
        if (strategies.isEmpty()) {
            return 1.0;
        }
        double sum = 0.0;
        for (StrategyOutcome s : strategies) {
            sum += Math.max(0.0, Math.min(1.0, s.score()));
        }
        return sum / strategies.size();
    }

    public boolean allStrategiesPassed() {
        return strategies.stream().allMatch(StrategyOutcome::passed);
    }
}
