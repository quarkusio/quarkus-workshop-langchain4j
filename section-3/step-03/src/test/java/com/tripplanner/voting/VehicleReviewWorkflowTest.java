package com.tripplanner.voting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.model.ItineraryResult;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.VehicleEvaluation;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.quarkiverse.langchain4j.ModelName;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(VehicleReviewWorkflowTest.ScriptedProfile.class)
class VehicleReviewWorkflowTest {
    @Inject ChatModel chatModel;
    ScriptedProfile.ScriptedModel model;

    @BeforeEach
    void reset() {
        model = (ScriptedProfile.ScriptedModel) chatModel;
        model.revisions.set(0);
        model.evaluations.set(0);
        model.costCalls.set(0);
        model.mode = "initial-pass";
    }

    @Test
    void acceptsInitialCandidateWithoutRevising() {
        plan().statusCode(200).body("vehicle.model", equalTo("Initial estate"));
        assertEquals(0, model.revisions.get());
        assertEquals(3, model.evaluations.get());
        assertEquals(1, model.costCalls.get());
    }

    @Test
    void evaluatesRevisionBeforePassingItToPricing() {
        model.mode = "improve";
        plan().statusCode(200).body("vehicle.model", equalTo("Revised estate 1"));
        assertEquals(1, model.revisions.get());
        assertEquals(6, model.evaluations.get());
    }

    @Test
    void evaluatesAndAcceptsTheLastPermittedRevision() {
        model.mode = "last-pass";
        plan().statusCode(200).body("vehicle.model", equalTo("Revised estate 3"));
        assertEquals(3, model.revisions.get());
        assertEquals(12, model.evaluations.get());
    }

    @Test
    void rejectsExhaustedReviewWithoutCallingPricing() {
        model.mode = "exhausted";
        plan().statusCode(422).body("error", equalTo("quality_not_met"))
                .body("message", containsString("after three revisions"));
        assertEquals(3, model.revisions.get());
        assertEquals(12, model.evaluations.get());
        assertEquals(0, model.costCalls.get());
    }

    @Test
    void guardsTheRevisedVehicleBeforeEvaluationAndPricing() {
        model.mode = "unsafe-revision";
        plan().statusCode(200).body("vehicle.type", equalTo("MPV"))
                .body("vehicle.model", equalTo("Family MPV; specific model subject to availability."));
        assertEquals(1, model.revisions.get());
        assertEquals(6, model.evaluations.get());
    }

    @Test
    void rejectsARevisionWhoseGuardrailAttemptsAreExhausted() {
        model.mode = "luxury-revision";
        plan().statusCode(422).body("error", equalTo("guardrail_violation"));
        assertEquals(3, model.revisions.get());
        assertEquals(3, model.evaluations.get());
        assertEquals(0, model.costCalls.get());
    }

    private io.restassured.response.ValidatableResponse plan() {
        return given().contentType("application/json").body("""
                {"destination":"Italian Riviera","startDate":"2027-07-10","days":1,
                 "tripType":"family","travelers":4,"budget":"economy",
                 "preferences":"keep room for our luggage"}
                """).post("/trip/plan").then();
    }

    public static class ScriptedProfile implements QuarkusTestProfile {
        @Override public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(ScriptedModel.class, EnhancedModel.class);
        }

        @Alternative @Singleton @ModelName("enhancedModel")
        public static class EnhancedModel implements ChatModel {
            @Inject ChatModel delegate;
            @Override public ChatResponse doChat(ChatRequest request) { return delegate.doChat(request); }
        }

        @Alternative @Singleton
        public static class ScriptedModel implements ChatModel {
            @Inject ObjectMapper mapper;
            final AtomicInteger revisions = new AtomicInteger();
            final AtomicInteger evaluations = new AtomicInteger();
            final AtomicInteger costCalls = new AtomicInteger();
            volatile String mode;

            @Override public ChatResponse doChat(ChatRequest request) {
                String prompt = request.messages().reversed().stream().filter(UserMessage.class::isInstance)
                        .map(UserMessage.class::cast).map(UserMessage::singleText)
                        .filter(s -> s.contains("- Destination:") || s.contains("Vehicle:")
                                || s.contains("Current recommendation:")).findFirst().orElseThrow();
                Object result;
                int revision = revisions.get();
                if (prompt.contains("vehicle specialist")) {
                    result = vehicle("Estate", "Initial estate");
                } else if (prompt.contains("itinerary planner")) {
                    result = new ItineraryResult("Coast", List.of(new TripPlan.DayItinerary(1, "Coast", "Walk", "Genoa")));
                } else if (prompt.contains("evaluator for")) {
                    evaluations.incrementAndGet();
                    if (revision > 0) assertTrue(prompt.contains(expectedModel()), "Evaluators must see the current guarded candidate");
                    double score = switch (mode) {
                        case "initial-pass" -> 8;
                        case "last-pass" -> revision == 3 ? 8 : 6;
                        case "exhausted", "luxury-revision" -> 6;
                        default -> revision == 0 ? 6 : 8;
                    };
                    result = new VehicleEvaluation(score, "Keep the original constraints");
                } else if (prompt.contains("recommendation specialist")) {
                    assertTrue(prompt.contains("keep room for our luggage"), "Revision must retain preferences");
                    int attempt = revisions.incrementAndGet();
                    result = switch (mode) {
                        case "unsafe-revision" -> vehicle("sports car", "Small roadster");
                        case "luxury-revision" -> vehicle("sports car", "Ferrari");
                        default -> vehicle("Estate", "Revised estate " + attempt);
                    };
                } else if (prompt.contains("cost estimation")) {
                    costCalls.incrementAndGet();
                    assertTrue(prompt.contains(expectedModel()), "Pricing must see the accepted candidate");
                    result = new TripPlan.CostEstimate("65", "10", "0", "100", "50", "20", "245");
                } else throw new AssertionError("Unexpected prompt: " + prompt);
                return ChatResponse.builder().aiMessage(AiMessage.from(mapper.valueToTree(result).toString())).build();
            }

            private String expectedModel() {
                if (mode.equals("unsafe-revision")) return "Family MPV; specific model subject to availability.";
                return revisions.get() == 0 ? "Initial estate" : "Revised estate " + revisions.get();
            }
            private TripPlan.VehicleRecommendation vehicle(String type, String name) {
                return new TripPlan.VehicleRecommendation(type, name, "Room for four and luggage");
            }
        }
    }
}
