package com.tripplanner.evaluation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.langfuse.api.model.NumericScoreV31;
import com.langfuse.api.model.ScoreV3;
import io.quarkiverse.langfuse.api.LangfuseOperations;
import io.quarkiverse.langfuse.api.ScoreFieldGroup;
import io.quarkiverse.langfuse.api.ScoreFilter;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Runs one sample from the Langfuse dataset as an experiment item and waits
 * for the Langfuse evaluator to score it.
 *
 * <p>Start dev mode first: it seeds the {@code trip-plan-samples} dataset and
 * creates the evaluator. Then run {@code ./mvnw verify -Pevals
 * -Dquarkus.langfuse.base-url=<dev mode Langfuse URL>} with a real LLM key and
 * the MCP server running on :8085.
 */
@QuarkusTest
@TestProfile(TripPlanQualityEvaluationLiveIT.LiveProfile.class)
@EnabledIfSystemProperty(named = "quarkus.langfuse.base-url", matches = ".+")
class TripPlanQualityEvaluationLiveIT {

    static final String DATASET = "trip-plan-samples";
    static final String SAMPLE = "rome-family-three-days";
    static final double THRESHOLD = 0.7;

    @Inject
    LangfuseOperations langfuse;

    @Inject
    TripPlanExperimentRunner runner;

    @Test
    void langfuseScoresTheExperimentItem() {
        assertTrue(TripPlannerCompositionLiveIT.probeSunnyServer(),
                "Trip Intelligence MCP server on :8085 is not running the sunny fixture");
        var loader = new LangfuseDatasetSampleLoader(langfuse);
        assertTrue(loader.datasetId(DATASET).isPresent(),
                "dataset " + DATASET + " not found in Langfuse: start dev mode first");

        var item = loader.load(SAMPLE);
        var result = runner.run("step-07-" + Instant.now(), item);
        assertFalse(result.traceId().matches("0+"), "the planning run must have a valid trace id");

        // Deterministic gate, checked locally before waiting for the judge.
        var invariant = new TripPlanInvariantStrategy().evaluate(item.sample(), result.output());
        assertTrue(invariant.passed(), "plan must pass the invariants: " + invariant.explanation());

        // Langfuse scores the experiment item asynchronously.
        NumericScoreV31 score = Awaitility.await()
                .atMost(Duration.ofSeconds(90))
                .pollInterval(Duration.ofSeconds(3))
                .until(() -> planQualityScore(result.traceId()), Optional::isPresent)
                .orElseThrow();
        assertTrue(score.getValue() >= THRESHOLD,
                "plan-quality " + score.getValue() + " is below " + THRESHOLD + ": " + score.getComment());
        assertFalse(score.getComment() == null || score.getComment().isBlank(),
                "the judge must explain its score");
    }

    Optional<NumericScoreV31> planQualityScore(String traceId) {
        var filter = ScoreFilter.builder()
                .traceId(traceId)
                .name("plan-quality")
                .fields(ScoreFieldGroup.DETAILS)
                .build();
        return langfuse.scores().matching(filter).findAll().stream()
                .map(ScoreV3::getActualInstance)
                .filter(NumericScoreV31.class::isInstance)
                .map(NumericScoreV31.class::cast)
                .findFirst();
    }

    public static class LiveProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "evals";
        }
    }
}
