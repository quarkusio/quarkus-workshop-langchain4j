package com.tripplanner.evaluation;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory store of {@link EvaluationRun}s, keyed by sample id, that keeps
 * every repeated experiment so a follow-up run can be compared against the
 * first. This is what makes an evaluation repeatable: the same sample run
 * again retains comparable inputs, output, and evidence.
 *
 * <p>Not a Quarkus bean on purpose: it holds the results of a test, not a
 * runtime concern, and keeping it plain makes it easy to assert against.
 */
public class EvaluationRunRecorder {

    /** All runs for one sample, in submission order. */
    public record SampleHistory(String sampleId, List<EvaluationRun> runs) {
        public EvaluationRun first() {
            return runs.get(0);
        }

        public EvaluationRun last() {
            return runs.get(runs.size() - 1);
        }

        /** True when a later run's input matches the first run's (comparable experiment). */
        public boolean inputsMatch() {
            EvaluationRun first = first();
            return runs.stream().allMatch(r -> r.input().equals(first.input()));
        }
    }

    private final List<EvaluationRun> runs = new ArrayList<>();

    public void record(EvaluationRun run) {
        runs.add(run);
    }

    public List<EvaluationRun> all() {
        return List.copyOf(runs);
    }

    public SampleHistory history(String sampleId) {
        List<EvaluationRun> forSample = runs.stream()
                .filter(r -> r.sampleId().equals(sampleId))
                .toList();
        if (forSample.isEmpty()) {
            return null;
        }
        return new SampleHistory(sampleId, List.copyOf(forSample));
    }

    public int count() {
        return runs.size();
    }
}
