package com.tripplanner.guardrails;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.agentic.agents.CostEstimatorAgent;
import com.tripplanner.model.ItineraryResult;
import com.tripplanner.model.TripPlan;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
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

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(RentalPricingAgentGuardrailTest.PricingProfile.class)
class RentalPricingAgentGuardrailTest {

    @Inject
    CostEstimatorAgent agent;

    @Inject
    ChatModel chatModel;

    @Test
    void productionAgentDeliversGuardrailErrorAndCorrectedRentalResultToScriptedModel() {
        var scriptedModel = assertInstanceOf(PricingProfile.ScriptedModel.class, chatModel);
        scriptedModel.calls = 0;
        var vehicle = new TripPlan.VehicleRecommendation("SUV", "Workshop SUV", "Room for the travelers");
        var itinerary = new ItineraryResult("Five-day local trip", List.of());

        // Use the CDI agent producer that applies @ToolBox in the production graph.
        var costs = agent.estimateCosts(vehicle, itinerary, "5", "4", "economy");

        assertNotNull(costs);
        assertAll(
                () -> assertEquals("80", costs.vehiclePerDay()),
                () -> assertEquals("0", costs.fuel()),
                () -> assertEquals("0", costs.tolls()),
                () -> assertEquals("0", costs.accommodation()),
                () -> assertEquals("0", costs.food()),
                () -> assertEquals("0", costs.activities()),
                () -> assertEquals("400", costs.total()),
                () -> assertEquals(3, scriptedModel.calls));
    }

    public static class PricingProfile implements QuarkusTestProfile {

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(ScriptedModel.class);
        }

        // Profile-nested beans are ignored outside this profile; no global @Priority override.
        @Alternative
        @Singleton
        public static class ScriptedModel implements ChatModel {

            @Inject
            ObjectMapper objectMapper;

            private int calls;

            @Override
            public ChatResponse doChat(ChatRequest request) {
                // This script checks the tool loop wiring, not a real model's ability to recover.
                AiMessage response = switch (++calls) {
                    case 1 -> {
                        assertTrue(request.messages().stream()
                                .filter(UserMessage.class::isInstance)
                                .map(UserMessage.class::cast)
                                .flatMap(message -> message.singleText().lines())
                                .anyMatch(line -> line.equals("Rental duration in days: 5")),
                                "The production prompt must include the requested rental duration");
                        assertTrue(request.toolSpecifications().stream()
                                .anyMatch(tool -> tool.name().equals("estimateRental")),
                                "The production @ToolBox must expose estimateRental");
                        yield AiMessage.from(ToolExecutionRequest.builder()
                                .id("invalid-rental").name("estimateRental")
                                .arguments("{\"category\":\"suv\",\"days\":0}").build());
                    }
                    case 2 -> {
                        var result = assertInstanceOf(ToolExecutionResultMessage.class,
                                request.messages().getLast());
                        assertEquals("invalid-rental", result.id());
                        assertEquals("estimateRental", result.toolName());
                        assertTrue(result.text().startsWith("Input validation failed:"));
                        assertTrue(result.text().contains("Set days to a whole number from 1 to 30."));
                        yield AiMessage.from(ToolExecutionRequest.builder()
                                .id("corrected-rental").name("estimateRental")
                                .arguments("{\"category\":\"suv\",\"days\":5}").build());
                    }
                    case 3 -> {
                        var result = assertInstanceOf(ToolExecutionResultMessage.class,
                                request.messages().getLast());
                        assertEquals("corrected-rental", result.id());
                        assertEquals("estimateRental", result.toolName());
                        var estimate = assertDoesNotThrow(() -> objectMapper.readTree(result.text()));
                        assertEquals("suv", estimate.path("category").asText());
                        assertEquals(5, estimate.path("days").asInt());
                        assertEquals("EUR", estimate.path("currency").asText());
                        assertEquals(80, estimate.path("dailyRate").asInt());
                        assertEquals(400, estimate.path("rentalTotal").asInt());
                        yield AiMessage.from("""
                                {"vehiclePerDay":"80","fuel":"0","tolls":"0",
                                 "accommodation":"0","food":"0","activities":"0","total":"400"}
                                """);
                    }
                    default -> throw new AssertionError("Unexpected model call: " + calls);
                };
                return ChatResponse.builder().aiMessage(response).build();
            }
        }
    }
}
