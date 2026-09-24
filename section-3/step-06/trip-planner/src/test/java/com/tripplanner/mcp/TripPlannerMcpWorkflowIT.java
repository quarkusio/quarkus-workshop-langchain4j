package com.tripplanner.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.agentic.workflow.TripPlannerSystem;
import com.tripplanner.model.ItineraryResult;
import com.tripplanner.model.TripIntelligenceException;
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
import org.junit.jupiter.api.TestInstance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Drives the complete planning graph with a scripted model while the two
 * {@code @McpClientAgent} subagents call the real Trip Intelligence MCP
 * server over streamable HTTP at http://localhost:8085/mcp. Each test
 * assumes the running server is configured with the matching fixture
 * profile (sunny by default, or -Dquarkus.profile=malformed-response or
 * timeout) and skips otherwise, so the suite stays green where no MCP
 * server is running. The fixture data is workshop-controlled, not live
 * weather or verified travel information.
 *
 * The default {@code ./mvnw test} suite never runs this class (its
 * Surefire includes cover only the declaration tests); run it explicitly
 * with {@code ./mvnw verify -DskipITs=false -Dit.test=TripPlannerMcpWorkflowIT}
 * after starting the MCP server.
 */
@QuarkusTest
@TestProfile(TripPlannerMcpWorkflowIT.ScriptedProfile.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TripPlannerMcpWorkflowIT {

    private static final URI MCP_ENDPOINT = URI.create("http://localhost:8085/mcp");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    @Inject
    TripPlannerSystem tripPlannerSystem;

    @Inject
    ChatModel chatModel;

    ScriptedProfile.ScriptedModel model;

    @BeforeEach
    void reset() {
        model = (ScriptedProfile.ScriptedModel) chatModel;
        model.invocations.set(0);
        model.revisions.set(0);
        model.itineraryPrompts.set(null);
        model.vehiclePrompts.set(null);
    }

    @Test
    void sunnyFixtureFlowsRealMcpDataIntoThePlan() {
        assumeSunnyServer();

        TripPlan plan = tripPlannerSystem.planTrip("Rome", "2027-07-10", "3",
                "family", "2", "moderate", "coastal towns");

        assertEquals("MPV", plan.vehicle().type());
        assertEquals("Family MPV", plan.vehicle().model());
        assertEquals(3, plan.itinerary().size());
        for (int day = 1; day <= 3; day++) {
            assertEquals(day, plan.itinerary().get(day - 1).day());
        }
        assertEquals("310", plan.costs().total());

        // The real weather fixture and the seeded POI catalog reached the research agents.
        String itineraryPrompt = model.itineraryPrompts.get();
        assertNotNull(itineraryPrompt, "itinerary planner must have run");
        assertTrue(itineraryPrompt.contains("Workshop fixture: 3 days in Rome"),
                "real weather fixture must reach the itinerary planner");
        assertTrue(itineraryPrompt.contains("Mostly sunny"), "weather conditions must reach the itinerary planner");
        assertTrue(itineraryPrompt.contains("Rome Zoo & Aquarium"), "seeded POIs must reach the itinerary planner");
        assertTrue(model.vehiclePrompts.get().contains("Mostly sunny"), "weather must reach the vehicle advisor");

        // A clean high score exits the loop without a revision and reaches pricing once.
        assertEquals(0, model.revisions.get());
        assertEquals(6, model.invocations.get());

        // A second, different request carries its own destination through the MCP tools.
        reset();
        TripPlan second = tripPlannerSystem.planTrip("Barcelona", "2027-07-12", "3",
                "family", "2", "moderate", "beaches");
        assertEquals("Family MPV", second.vehicle().model());
        assertTrue(model.itineraryPrompts.get().contains("Barcelona Zoo & Aquarium"),
                "the second request's destination must reach the POI tool");
        assertTrue(model.itineraryPrompts.get().contains("Workshop fixture: 3 days in Barcelona"),
                "the second request's destination must reach the weather tool");
    }

    @Test
    void malformedFixtureStopsResearchBeforeAnyModelCall() {
        assumeMalformedServer();

        // The @Output resolver runs inside the agentic invocation, so the
        // TripIntelligenceException surfaces wrapped; the contract (see TripError.from)
        // is that it is present in the cause chain.
        Throwable failure = assertThrows(RuntimeException.class, () -> tripPlannerSystem.planTrip(
                "Rome", "2027-07-10", "3", "family", "2", "moderate", "coastal towns"));
        assertCarries(failure, TripIntelligenceException.class);
        assertEquals(0, model.invocations.get(), "no LLM call may happen when evidence is unusable");
    }

    @Test
    void timedOutFixtureStopsResearchBeforeAnyModelCall() {
        assumeTimeoutServer();

        long start = System.nanoTime();
        Throwable failure = assertThrows(RuntimeException.class, () -> tripPlannerSystem.planTrip(
                "Rome", "2027-07-10", "3", "family", "2", "moderate", "coastal towns"));
        assertCarries(failure, TripIntelligenceException.class);
        // The client wait is bounded by the 5s tool timeout, not the 15s fixture.
        assertTrue(Duration.ofNanos(System.nanoTime() - start).toSeconds() < 15,
                "the planning wait must be bounded by the client tool timeout");
        assertEquals(0, model.invocations.get(), "no LLM call may happen when evidence is unusable");
    }

    private static void assertCarries(Throwable failure, Class<? extends Throwable> type) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause)) {
                return;
            }
        }
        fail("expected " + type.getSimpleName() + " in the cause chain of: " + failure);
    }

    private void assumeSunnyServer() {
        String body = probeWeather();
        assumeTrue(body != null && body.contains("Mostly sunny"),
                "Trip Intelligence MCP server on :8085 is not running the sunny fixture");
    }

    private void assumeMalformedServer() {
        String body = probeWeather();
        // The tool result is a JSON string nested in the MCP text content, so the
        // quotes arrive backslash-escaped in the raw body.
        assumeTrue(body != null && body.contains("\\\"summary\\\":\\\"\\\""),
                "Trip Intelligence MCP server on :8085 is not running the malformed-response fixture");
    }

    private void assumeTimeoutServer() {
        assumeTrue(probeWeatherTimesOut(),
                "Trip Intelligence MCP server on :8085 is not running the timeout fixture");
    }

    private String probeWeather() {
        try {
            String session = initialize();
            HttpResponse<String> response = HTTP.send(call("getWeatherForecast",
                    "{\"destination\":\"Rome\",\"startDate\":\"2027-07-10\",\"days\":\"3\"}", session),
                    HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean probeWeatherTimesOut() {
        try {
            String session = initialize();
            HTTP.send(call("getWeatherForecast",
                    "{\"destination\":\"Rome\",\"startDate\":\"2027-07-10\",\"days\":\"3\"}", session),
                    HttpResponse.BodyHandlers.ofString());
            return false;
        } catch (HttpTimeoutException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String initialize() throws Exception {
        HttpResponse<String> init = HTTP.send(HttpRequest.newBuilder(MCP_ENDPOINT)
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"workflow-it","version":"1"}}}"""))
                .build(), HttpResponse.BodyHandlers.ofString());
        return init.headers().firstValue("mcp-session-id").orElse(null);
    }

    private HttpRequest call(String tool, String arguments, String session) {
        HttpRequest.Builder request = HttpRequest.newBuilder(MCP_ENDPOINT)
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"%s","arguments":%s}}"""
                        .formatted(tool, arguments)));
        if (session != null) {
            request.header("mcp-session-id", session);
        }
        return request.build();
    }

    public static class ScriptedProfile implements QuarkusTestProfile {
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
            ObjectMapper mapper;

            final AtomicInteger invocations = new AtomicInteger();
            final AtomicInteger revisions = new AtomicInteger();
            final AtomicReference<String> itineraryPrompts = new AtomicReference<>();
            final AtomicReference<String> vehiclePrompts = new AtomicReference<>();

            @Override
            public ChatResponse doChat(ChatRequest request) {
                invocations.incrementAndGet();
                String prompt = request.messages().reversed().stream().filter(UserMessage.class::isInstance)
                        .map(UserMessage.class::cast).map(UserMessage::singleText)
                        .filter(s -> s.contains("- Destination:") || s.contains("Vehicle:")
                                || s.contains("Current recommendation:"))
                        .findFirst().orElseThrow();
                Object result;
                if (prompt.contains("vehicle specialist")) {
                    vehiclePrompts.set(prompt);
                    result = new TripPlan.VehicleRecommendation("MPV", "Family MPV", "Room for two travelers and luggage");
                } else if (prompt.contains("itinerary planner")) {
                    itineraryPrompts.set(prompt);
                    result = new ItineraryResult("Coastal route through the region", List.of(
                            new TripPlan.DayItinerary(1, "Arrival", "Settle in and explore the old town on foot", "Hotel"),
                            new TripPlan.DayItinerary(2, "Family day", "Visit the zoo and the science museum", "Hotel"),
                            new TripPlan.DayItinerary(3, "Departure", "Morning in the park, then the drive home", "Hotel")));
                } else if (prompt.contains("evaluator for")) {
                    result = new VehicleEvaluation(8.5, "Keep the MPV");
                } else if (prompt.contains("recommendation specialist")) {
                    revisions.incrementAndGet();
                    throw new AssertionError("The sunny initial-pass script must not revise the vehicle");
                } else if (prompt.contains("cost estimation")) {
                    assertTrue(prompt.contains("Family MPV"), "cost estimation must receive the accepted vehicle");
                    assertTrue(prompt.contains("Coastal route through the region"), "cost estimation must receive the route");
                    result = new TripPlan.CostEstimate("90", "10", "0", "100", "50", "20", "310");
                } else {
                    throw new AssertionError("Unexpected agent prompt");
                }
                return ChatResponse.builder().aiMessage(AiMessage.from(mapper.valueToTree(result).toString())).build();
            }
        }
    }
}
