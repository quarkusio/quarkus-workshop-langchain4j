package com.tripplanner.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.quarkiverse.langchain4j.testing.evaluation.EvaluationResult;
import io.quarkiverse.langchain4j.testing.evaluation.EvaluationSample;
import io.quarkiverse.langchain4j.testing.evaluation.Parameters;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * The judge contract, verified offline with a deterministic fake model and no
 * app boot: a rubric that asks for a literal true/false verdict passes on a
 * matching plan and fails otherwise, and a non-boolean or JSON verdict can
 * never register as a pass (it parses as false).
 */
class TripPlanJudgeContractTest {

    private static final String EXPECTED =
            "Vehicle: Family MPV (Room for two travelers and luggage)\n"
            + "Route: Coastal route through the region\n"
            + "Day 1: Arrival\nDay 2: Family day\nDay 3: Departure\n"
            + "Cost total: 310\n";

    @Test
    void aMatchingPlanPassesTheJudge() {
        TripPlanJudge judge = new TripPlanJudge(new JudgeModel(s -> "true"), rubric());

        EvaluationResult result = judge.judge(sample(), EXPECTED);

        assertTrue(result.passed());
        assertEquals(1.0, result.score());
        assertEquals("true", result.explanation());
        assertEquals("JudgeModel", result.metadata().get("judge-model"));
    }

    @Test
    void aMismatchingPlanFailsTheJudge() {
        TripPlanJudge judge = new TripPlanJudge(new JudgeModel(s -> "false"), rubric());

        EvaluationResult result = judge.judge(sample(), "A completely different itinerary.");

        assertFalse(result.passed());
        assertEquals(0.0, result.score());
    }

    @Test
    void aJsonVerdictIsNeverParsedAsAPass() {
        // A model that answers JSON (a common mistake) must not pass:
        // Boolean.parseBoolean of any non-"true" text is false.
        TripPlanJudge judge = new TripPlanJudge(new JudgeModel(s -> "{\"score\": 9, \"explanation\": \"strong match\"}"), rubric());

        EvaluationResult result = judge.judge(sample(), EXPECTED);

        assertFalse(result.passed());
        assertEquals(0.0, result.score());
        assertTrue(result.explanation().startsWith("{"));
    }

    @Test
    void proseOrMissingVerdictIsTreatedAsFalse() {
        TripPlanJudge judge = new TripPlanJudge(new JudgeModel(s -> "This looks like a good plan overall."), rubric());

        EvaluationResult result = judge.judge(sample(), EXPECTED);

        assertFalse(result.passed(), "a verdict that is not the word 'true' cannot pass the gate");
    }

    /**
     * A deterministic judge model: ignores the prompt and always returns the
     * configured verdict. Proves the parsing contract without any network.
     */
    static class JudgeModel implements ChatModel {
        private final java.util.function.Function<String, String> verdict;

        JudgeModel(java.util.function.Function<String, String> verdict) {
            this.verdict = verdict;
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            String user = request.messages().stream()
                    .filter(UserMessage.class::isInstance)
                    .map(UserMessage.class::cast)
                    .map(UserMessage::singleText)
                    .reduce("", (a, b) -> a + "\n" + b);
            return ChatResponse.builder().aiMessage(AiMessage.from(verdict.apply(user))).build();
        }

        @Override
        public List<dev.langchain4j.model.chat.listener.ChatModelListener> listeners() {
            return List.of();
        }
    }

    private static String rubric() {
        return "Answer with the single word \"true\" or \"false\".\n\n"
                + "Response to evaluate: {response}\n"
                + "Expected output: {expected_output}\n";
    }

    private static EvaluationSample<String> sample() {
        return EvaluationSample.<String>builder()
                .withName("rome-family-three-days")
                .withParameters(Parameters.of("Rome", "2027-07-10", "3", "family", "2", "moderate", "coastal towns"))
                .withExpectedOutput(EXPECTED)
                .build();
    }
}
