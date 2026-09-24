package com.tripplanner.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tripplanner.agentic.workflow.TripPlannerSystem;
import com.tripplanner.model.TripPlan;
import dev.langchain4j.model.chat.ChatModel;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.quarkiverse.langfuse.api.LangfuseOperations;
import io.quarkiverse.langchain4j.testing.evaluation.EvaluationResult;
import io.quarkiverse.langchain4j.testing.evaluation.EvaluationSample;
import io.quarkiverse.langchain4j.testing.evaluation.Parameters;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * The full live evaluation loop: one real planning run (real LLM plus the real
 * Trip Intelligence MCP server on :8085), its trace captured while the root
 * span is current, the saved output checked by the deterministic invariant
 * strategy and by the AI judge, and the resulting score published to Langfuse
 * and verified back through the score API as landing on exactly this trace.
 *
 * <p>Run with {@code ./mvnw verify -Pevals}. Requires a container runtime
 * (Langfuse Dev Services) and a real LLM key. The MCP server must be running.
 */
@QuarkusTest
@TestProfile(TripPlanQualityEvaluationLiveIT.LiveProfile.class)
@org.junit.jupiter.api.condition.EnabledIf("containerRuntimeAvailable")
class TripPlanQualityEvaluationLiveIT {

    static boolean containerRuntimeAvailable() {
        try {
            Process p = new ProcessBuilder("bash", "-c",
                    "command -v podman >/dev/null 2>&1 || command -v docker >/dev/null 2>&1")
                    .redirectErrorStream(true).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static final String SAMPLE = "rome-family-three-days";

    @Inject
    TripPlannerSystem tripPlannerSystem;

    @Inject
    ChatModel chatModel;

    @Inject
    Tracer tracer;

    @Inject
    LangfuseOperations langfuse;

    final EvaluationRunRecorder recorder = new EvaluationRunRecorder();
    final TripPlanInvariantStrategy invariants = new TripPlanInvariantStrategy();

    @Test
    void oneSampleOneTraceOneScoreVerifiedThroughTheScoreApi() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                TripPlannerCompositionLiveIT.probeSunnyServer(),
                "Trip Intelligence MCP server on :8085 is not running the sunny fixture");
        // One real planning run, with the root span current so the agent's
        // internal spans join this trace. Capture the trace id before ending.
        String traceId;
        TripPlan plan;
        Span root = tracer.spanBuilder("trip-plan-evaluation")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("trip.sample.id", SAMPLE)
                .startSpan();
        try (var scope = root.makeCurrent()) {
            traceId = root.getSpanContext().getTraceId();
            plan = tripPlannerSystem.planTrip("Rome", "2027-07-10", "3", "family", "2", "moderate", "coastal towns");
        } finally {
            root.end();
        }
        assertTrue(traceId.matches("[0-9a-f]{32}"), "captured trace id must be 32-hex: " + traceId);

        String output = TripPlanText.render(plan);

        // Deterministic gate on the saved output.
        EvaluationResult invariant = invariants.evaluate(sample(output), output);

        // The AI judge is a separate invocation from the application run, so its
        // usage stays separable from the planner's measurements.
        TripPlanJudge judge = new TripPlanJudge(chatModel);
        EvaluationResult judged = judge.judge(sample(output), output);
        assertNotNull(judged.metadata().get("judge-model"), "judge must record which model it used");

        var run = new EvaluationRun(
                SAMPLE,
                List.of("Rome", "2027-07-10", "3", "family", "2", "moderate", "coastal towns"),
                output,
                List.of("vehicle=" + plan.vehicle().model(), "days=" + plan.itinerary().size()),
                traceId, "openai",
                List.of(
                        new EvaluationRun.StrategyOutcome("invariant", invariant.passed(), invariant.score(), invariant.explanation(), invariant.metadata()),
                        new EvaluationRun.StrategyOutcome("judge", judged.passed(), judged.score(), judged.explanation(), judged.metadata())),
                Instant.now());
        recorder.record(run);

        // Publish the score to the captured trace and verify it lands there.
        LangfuseScorePublisher publisher = new LangfuseScorePublisher(langfuse);
        LangfuseScorePublisher.PublishedScore published =
                publisher.publishScore(run, run.aggregateScore(), "plan-quality",
                        "invariant=" + invariant.passed() + " judge=" + judged.passed());

        // Score ingestion is asynchronous, so retry.
        Awaitility.await().atMost(Duration.ofSeconds(90)).untilAsserted(() ->
                assertTrue(publisher.scoreIsQueryable(published.scoreId(), published.traceId()),
                        "the score must be queryable on the planning trace"));

        // A score must land only on the intended trace, not a foreign one.
        String foreignTrace = "0".repeat(32);
        assertFalse(publisher.scoreIsQueryable(published.scoreId(), foreignTrace),
                "the score must not be queryable on an unrelated trace");
        assertEquals(published.traceId(), run.traceId());
    }

    static EvaluationSample<String> sample(String output) {
        return EvaluationSample.<String>builder()
                .withName(SAMPLE)
                .withParameters(Parameters.of("Rome", "2027-07-10", "3", "family", "2", "moderate", "coastal towns"))
                .withExpectedOutput(output)
                .build();
    }

    public static class LiveProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "evals";
        }

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of();
        }
    }
}
