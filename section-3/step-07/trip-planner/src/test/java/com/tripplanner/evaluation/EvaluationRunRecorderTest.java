package com.tripplanner.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

/**
 * The recorder keeps every repeated experiment for a sample so a follow-up run
 * can be compared against the first: same inputs, retained output and evidence,
 * and per-case strategy results.
 */
class EvaluationRunRecorderTest {

    @Test
    void aRepeatedExperimentRetainsComparableInputAndEvidence() {
        EvaluationRunRecorder recorder = new EvaluationRunRecorder();
        String output = "Vehicle: Family MPV\nRoute: Coastal route\nDay 1: Arrival\nCost total: 310\n";

        recorder.record(run("rome-family-three-days", output, List.of("weather: sunny", "pois: 3 entries")));
        recorder.record(run("rome-family-three-days", output, List.of("weather: sunny", "pois: 3 entries")));

        EvaluationRunRecorder.SampleHistory history = recorder.history("rome-family-three-days");

        assertNotNull(history);
        assertEquals(2, history.runs().size());
        assertTrue(history.inputsMatch(), "both runs must have been made from the same request");
        // The saved output and evidence are retained across runs for comparison.
        assertSame(history.first().output(), history.last().output());
        assertEquals(history.first().evidence(), history.last().evidence());
    }

    @Test
    void perCaseResultsAreKeptSeparately() {
        EvaluationRunRecorder recorder = new EvaluationRunRecorder();
        recorder.record(run("rome", "plan-rome", List.of("e-rome")));
        recorder.record(run("barcelona", "plan-barcelona", List.of("e-barcelona")));

        assertEquals(2, recorder.count());
        assertEquals("plan-rome", recorder.history("rome").first().output());
        assertEquals("plan-barcelona", recorder.history("barcelona").first().output());
        assertNull(recorder.history("unknown"), "a sample that was never run has no history");
    }

    @Test
    void aMismatchedInputIsDetected() {
        EvaluationRunRecorder recorder = new EvaluationRunRecorder();
        recorder.record(run("rome", "p", List.of(), List.of("Rome", "3 days")));
        recorder.record(run("rome", "p", List.of(), List.of("Rome", "5 days")));

        assertFalse(recorder.history("rome").inputsMatch());
    }

    @Test
    void theAggregateScoreIsTheMeanOfStrategies() {
        EvaluationRun run = new EvaluationRun(
                "rome", List.of("Rome"), "plan", List.of(), "trace-1", "model",
                List.of(
                        new EvaluationRun.StrategyOutcome("invariant", true, 1.0, null, null),
                        new EvaluationRun.StrategyOutcome("judge", false, 0.0, "mismatch", null)),
                Instant.now());

        assertEquals(0.5, run.aggregateScore());
        assertFalse(run.allStrategiesPassed());
    }

    private static EvaluationRun run(String sampleId, String output, List<String> evidence) {
        return run(sampleId, output, evidence, List.of(sampleId, "3 days"));
    }

    private static EvaluationRun run(String sampleId, String output, List<String> evidence, List<String> input) {
        return new EvaluationRun(
                sampleId, input, output, evidence, "trace-" + sampleId, "ScriptedModel", List.of(), Instant.now());
    }
}
