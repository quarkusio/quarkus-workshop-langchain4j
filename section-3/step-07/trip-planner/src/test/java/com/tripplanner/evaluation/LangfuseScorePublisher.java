package com.tripplanner.evaluation;

import io.quarkiverse.langfuse.api.LangfuseOperations;
import io.quarkiverse.langfuse.api.ScoreFilter;

import com.langfuse.api.model.CreateScoreRequest;
import com.langfuse.api.model.CreateScoreResponse;
import com.langfuse.api.model.CreateScoreSource;
import com.langfuse.api.model.CreateScoreValue;
import com.langfuse.api.model.ScoreDataType;

import java.util.Map;

/**
 * Publishes a run's aggregate score to Langfuse, attached to the trace the run
 * actually produced (captured while the root span was current).
 *
 * <p>Kept as a plain class that receives the injected {@link LangfuseOperations}:
 * the harness must hold the injected reference, not look the bean up at runtime
 * (Arc bean removal drops a bean that is only referenced through
 * {@code CDI.current().select()} in a test lambda).
 */
public class LangfuseScorePublisher {

    public record PublishedScore(String scoreId, String traceId, String name, double value) {
    }

    private final LangfuseOperations langfuse;

    public LangfuseScorePublisher(LangfuseOperations langfuse) {
        this.langfuse = langfuse;
    }

    /**
     * Attaches a numeric score to the given trace and returns the created score
     * id for later verification through the score API.
     */
    public PublishedScore publishScore(EvaluationRun run, double score, String name, String comment) {
        CreateScoreResponse created = langfuse.scores().create(CreateScoreRequest.builder()
                .name(name)
                .value(new CreateScoreValue(score))
                .dataType(ScoreDataType.NUMERIC)
                .traceId(run.traceId())
                .source(CreateScoreSource.API)
                .comment(comment)
                .metadata(Map.of(
                        "sample.id", run.sampleId(),
                        "model", run.model() == null ? "unknown" : run.model()))
                .build());
        return new PublishedScore(created.getId(), run.traceId(), name, score);
    }

    /**
     * Returns true when a previously created score is queryable for the run's
     * trace. Score ingestion is asynchronous, so callers must retry (e.g. with
     * Awaitility).
     */
    public boolean scoreIsQueryable(String scoreId, String traceId) {
        return langfuse.scores()
                .matching(ScoreFilter.builder().traceId(traceId).build())
                .findAll().stream()
                .map(s -> s.getNumericScoreV31() == null ? null : s.getNumericScoreV31().getId())
                .anyMatch(scoreId::equals);
    }
}
