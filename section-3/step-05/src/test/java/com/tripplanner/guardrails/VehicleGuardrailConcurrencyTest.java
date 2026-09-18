package com.tripplanner.guardrails;

import com.tripplanner.agentic.workflow.TripPlannerSystem;
import com.tripplanner.model.TripPlan;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(VehicleGuardrailConcurrencyTest.ConcurrentProfile.class)
class VehicleGuardrailConcurrencyTest {
    @Inject
    TripPlannerSystem planner;
    @Inject
    ChatModel chatModel;

    @Test
    void overlappingPlansKeepTheirOwnBudgetAndTravelerDetailsAcrossReprompts() throws Exception {
        // Hold every first vehicle response until all four plans are inside the model.
        // This also exercises each plan's parallel vehicle/itinerary research phase.
        try (var executor = Executors.newFixedThreadPool(4)) {
            var family = executor.submit(() -> plan("Family", "family", 5, "economy"));
            var adventure = executor.submit(() -> plan("Adventure", "adventure", 4, "economy"));
            var business = executor.submit(() -> plan("Business", "business", 4, "premium"));
            var couple = executor.submit(() -> plan("Couple", "family", 2, "premium"));

            assertEquals("MPV", family.get(20, SECONDS).vehicle().type());
            assertEquals("SUV", adventure.get(20, SECONDS).vehicle().type());
            assertEquals("Estate", business.get(20, SECONDS).vehicle().type());
            assertEquals("Ferrari Roma", couple.get(20, SECONDS).vehicle().model());

            var model = assertInstanceOf(ConcurrentProfile.ScriptedModel.class, chatModel);
            assertEquals(2, model.calls.get("Family").get());
            assertEquals(2, model.calls.get("Adventure").get());
            assertEquals(1, model.calls.get("Business").get());
            assertEquals(1, model.calls.get("Couple").get());
        }
    }

    private TripPlan plan(String destination, String tripType, int travelers, String budget) {
        return planner.planTrip(destination, "2027-07-10", "5", tripType, String.valueOf(travelers), budget, "");
    }

    public static class ConcurrentProfile implements QuarkusTestProfile {
        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(ScriptedModel.class);
        }

        @Alternative
        @Singleton
        public static class ScriptedModel implements ChatModel {
            final Map<String, AtomicInteger> calls = new ConcurrentHashMap<>();
            final CyclicBarrier firstResponses = new CyclicBarrier(4);

            @Override
            public ChatResponse doChat(ChatRequest request) {
                String prompt = request.messages().reversed().stream().filter(UserMessage.class::isInstance)
                        .map(UserMessage.class::cast).map(UserMessage::singleText)
                        .filter(text -> text.contains("- Destination:") || text.contains("Vehicle:"))
                        .findFirst().orElseThrow();
                String json;
                if (prompt.contains("vehicle specialist")) {
                    String destination = prompt.lines().filter(line -> line.startsWith("- Destination:"))
                            .findFirst().orElseThrow().substring("- Destination:".length()).trim();
                    int attempt = calls.computeIfAbsent(destination, ignored -> new AtomicInteger()).incrementAndGet();
                    if (attempt == 1) {
                        try {
                            firstResponses.await(10, SECONDS);
                        } catch (Exception failure) {
                            throw new AssertionError("All four planning calls must overlap", failure);
                        }
                    } else {
                        assertEquals(2, attempt, "Only economy trips need one reprompt");
                        assertTrue(request.messages().stream().filter(UserMessage.class::isInstance)
                                .map(UserMessage.class::cast).map(UserMessage::singleText)
                                .anyMatch(text -> text.contains("MUST recommend only budget-friendly")));
                    }
                    json = "{\"type\":\"Sports car\",\"model\":\""
                            + (attempt == 1 ? "Ferrari Roma" : "Mazda MX-5") + "\",\"reasoning\":\"Test recommendation\"}";
                } else if (prompt.contains("itinerary planner")) {
                    json = """
                            {"routeOverview":"Local route","itinerary":[
                             {"day":1,"title":"Arrival","description":"Explore the town","overnightStop":"Genoa"}]}
                            """;
                } else if (prompt.contains("cost estimation")) {
                    json = """
                            {"vehiclePerDay":"90","fuel":"0","tolls":"0","accommodation":"0",
                             "food":"0","activities":"0","total":"450"}
                            """;
                } else {
                    throw new AssertionError("Unexpected agent prompt");
                }
                return ChatResponse.builder().aiMessage(AiMessage.from(json)).build();
            }
        }
    }
}
