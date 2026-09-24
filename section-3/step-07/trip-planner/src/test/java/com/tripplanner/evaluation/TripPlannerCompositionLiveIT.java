package com.tripplanner.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tripplanner.agentic.workflow.TripPlannerSystem;
import com.tripplanner.model.ItineraryResult;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripPlan.DayItinerary;
import com.tripplanner.model.TripPlan.VehicleRecommendation;
import com.tripplanner.model.VehicleEvaluation;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.quarkiverse.langchain4j.ModelName;
import io.quarkiverse.langchain4j.testing.evaluation.EvaluationSample;
import io.quarkiverse.langchain4j.testing.evaluation.Parameters;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Drives the final planning graph with a scripted model while the two
 * {@code @McpClientAgent} subagents call the real Trip Intelligence MCP server
 * over streamable HTTP at http://localhost:8085/mcp. One fresh run per sample;
 * each saved output is checked by the invariant strategy and retained in the
 * recorder, so a repeated experiment keeps comparable inputs and evidence.
 *
 * <p>Run with {@code ./mvnw verify -Pevals} after starting the MCP server
 * (sunny fixture). No LLM is called (scripted model) and no Langfuse
 * container is started (dead keys under the {@code mcp} profile).
 */
@QuarkusTest
@TestProfile(TripPlannerCompositionLiveIT.ScriptedProfile.class)
class TripPlannerCompositionLiveIT {

    @Inject
    TripPlannerSystem tripPlannerSystem;

    @Inject
    ChatModel chatModel;

    ScriptedProfile.ScriptedModel model;

    final EvaluationRunRecorder recorder = new EvaluationRunRecorder();
    final TripPlanInvariantStrategy invariants = new TripPlanInvariantStrategy();

    @BeforeEach
    void reset() {
        model = (ScriptedProfile.ScriptedModel) chatModel;
        // Skip where no Trip Intelligence server is running the sunny fixture,
        // so the suite stays green in environments without the MCP server.
        org.junit.jupiter.api.Assumptions.assumeTrue(
                probeSunnyServer(),
                "Trip Intelligence MCP server on :8085 is not running the sunny fixture");
    }

    static boolean probeSunnyServer() {
        try {
            var client = java.net.http.HttpClient.newHttpClient();
            var init = client.send(
                    mcpRequest("initialize",
                            "{\"protocolVersion\":\"2025-11-25\",\"capabilities\":{},\"clientInfo\":{\"name\":\"it\",\"version\":\"1\"}}")
                            .build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            String session = init.headers().firstValue("mcp-session-id").orElse(null);
            var call = mcpRequest("tools/call",
                    "{\"name\":\"getWeatherForecast\",\"arguments\":{\"destination\":\"Rome\",\"startDate\":\"2027-07-10\",\"days\":\"3\"}}");
            if (session != null) {
                call.header("mcp-session-id", session);
            }
            String body = client.send(call.build(), java.net.http.HttpResponse.BodyHandlers.ofString()).body();
            return body != null && body.contains("Mostly sunny");
        } catch (Exception e) {
            return false;
        }
    }

    static java.net.http.HttpRequest.Builder mcpRequest(String method, String params) {
        return java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:8085/mcp"))
                .timeout(java.time.Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                        "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\",\"params\":" + params + "}"));
    }

    @Test
    void aRomeFamilyTripPassesItsInvariantsAndIsRecorded() {
        TripPlan plan = tripPlannerSystem.planTrip("Rome", "2027-07-10", "3", "family", "2", "moderate", "coastal towns");
        String output = TripPlanText.render(plan);

        // The real MCP fixture data reached the research agents.
        assertTrue(model.itineraryPrompt().contains("Workshop fixture: 3 days in Rome"),
                "real weather fixture must reach the itinerary planner");
        assertTrue(model.itineraryPrompt().contains("Rome Zoo & Aquarium"),
                "seeded POIs must reach the itinerary planner");

        // The saved output passes the deterministic gate.
        var invariant = invariants.evaluate(sample("rome-family-three-days", output), output);
        assertTrue(invariant.passed(), "saved plan must pass the invariants: " + invariant.explanation());

        // The run is retained for a comparable repeated experiment.
        var run = new EvaluationRun(
                "rome-family-three-days",
                List.of("Rome", "2027-07-10", "3", "family", "2", "moderate", "coastal towns"),
                output,
                List.of(model.evidenceLine()),
                null, "ScriptedModel",
                List.of(new EvaluationRun.StrategyOutcome("invariant",
                        invariant.passed(), invariant.score(), invariant.explanation(), invariant.metadata())),
                Instant.now());
        recorder.record(run);

        assertEquals(1, recorder.count());
        assertTrue(recorder.history("rome-family-three-days").inputsMatch());
    }

    @Test
    void requestIsolationMeansASecondDestinationIsIndependent() {
        TripPlan first = tripPlannerSystem.planTrip("Rome", "2027-07-10", "3", "family", "2", "moderate", "coastal towns");
        TripPlan second = tripPlannerSystem.planTrip("Barcelona", "2027-07-12", "3", "family", "2", "moderate", "beaches");

        // Each request carried its own destination through the MCP tools.
        assertTrue(model.itineraryPrompt().contains("Barcelona"),
                "the second request's destination must reach the planner");
        assertEquals(3, first.itinerary().size());
        assertEquals(3, second.itinerary().size());
        assertNotNull(first.vehicle());
        assertNotNull(second.vehicle());
        assertFalse(TripPlanText.render(first).contains("Barcelona"),
                "the first plan must not leak the second request's destination");
    }

    static EvaluationSample<String> sample(String name, String output) {
        return EvaluationSample.<String>builder()
                .withName(name)
                .withParameters(Parameters.of("Rome", "2027-07-10", "3", "family", "2", "moderate", "coastal towns"))
                .withExpectedOutput(output)
                .build();
    }

    public static class ScriptedProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "mcp";
        }

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(ScriptedModel.class, EnhancedModel.class);
        }

        @Alternative
        @Singleton
        @ModelName("enhancedModel")
        public static class EnhancedModel implements ChatModel {
            @Inject
            ChatModel delegate;

            @Override
            public ChatResponse doChat(ChatRequest request) {
                return delegate.doChat(request);
            }
        }

        @Alternative
        @Singleton
        public static class ScriptedModel implements ChatModel {
            @Inject
            com.fasterxml.jackson.databind.ObjectMapper mapper;

            final AtomicReference<String> itineraryPrompt = new AtomicReference<>();
            final AtomicReference<String> lastEvidence = new AtomicReference<>();

            String itineraryPrompt() {
                return itineraryPrompt.get();
            }

            String evidenceLine() {
                return lastEvidence.get();
            }

            @Override
            public ChatResponse doChat(ChatRequest request) {
                String prompt = request.messages().reversed().stream().filter(UserMessage.class::isInstance)
                        .map(UserMessage.class::cast).map(UserMessage::singleText)
                        .filter(s -> s.contains("- Destination:") || s.contains("Vehicle:")
                                || s.contains("Current recommendation:"))
                        .findFirst().orElseThrow();
                Object result;
                if (prompt.contains("vehicle specialist")) {
                    result = new VehicleRecommendation("MPV", "Family MPV", "Room for two travelers and luggage");
                } else if (prompt.contains("itinerary planner")) {
                    itineraryPrompt.set(prompt);
                    result = new ItineraryResult("Coastal route through the region", List.of(
                            new DayItinerary(1, "Arrival", "Settle in and explore the old town on foot", "Hotel"),
                            new DayItinerary(2, "Family day", "Visit the zoo and the science museum", "Hotel"),
                            new DayItinerary(3, "Departure", "Morning in the park, then the drive home", "Hotel")));
                } else if (prompt.contains("evaluator for")) {
                    lastEvidence.set("evaluation: 8.5");
                    result = new VehicleEvaluation(8.5, "Keep the MPV");
                } else if (prompt.contains("recommendation specialist")) {
                    throw new AssertionError("The sunny initial-pass script must not revise the vehicle");
                } else if (prompt.contains("cost estimation")) {
                    result = new TripPlan.CostEstimate("90", "10", "0", "100", "50", "20", "310");
                } else {
                    throw new AssertionError("Unexpected agent prompt");
                }
                return ChatResponse.builder().aiMessage(AiMessage.from(mapper.valueToTree(result).toString())).build();
            }
        }
    }
}
