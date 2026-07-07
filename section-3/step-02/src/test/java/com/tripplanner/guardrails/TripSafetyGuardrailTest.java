package com.tripplanner.guardrails;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TripSafetyGuardrailTest {

    @Inject
    TripSafetyGuardrail guardrail;

    @Inject
    GuardrailAuditLog auditLog;

    @Test
    void validPlanShouldPass() {
        AiMessage message = AiMessage.from(validTripPlanJson());
        OutputGuardrailResult result = guardrail.validate(message);
        assertTrue(result.isSuccess());
    }

    @Test
    void invalidJsonShouldRetry() {
        AiMessage message = AiMessage.from("this is not json");
        OutputGuardrailResult result = guardrail.validate(message);
        assertTrue(result.isRetry());
    }

    @Test
    void emptyItineraryShouldRetry() {
        String json = """
                {
                    "vehicle": {"type": "SUV", "model": "Toyota RAV4", "reasoning": "Good for families"},
                    "routeOverview": "A scenic coastal route",
                    "itinerary": [],
                    "costs": {"total": "€2000"},
                    "tips": ["Pack sunscreen"]
                }
                """;
        AiMessage message = AiMessage.from(json);
        OutputGuardrailResult result = guardrail.validate(message);
        assertTrue(result.isRetry());
    }

    @Test
    void missingTotalCostShouldRetry() {
        String json = """
                {
                    "vehicle": {"type": "SUV", "model": "Toyota RAV4", "reasoning": "Good for families"},
                    "routeOverview": "A scenic coastal route",
                    "itinerary": [{"day": 1, "title": "Day 1", "description": "Explore the coast", "overnightStop": "Nice"}],
                    "costs": {"fuel": "€100"},
                    "tips": ["Pack sunscreen"]
                }
                """;
        AiMessage message = AiMessage.from(json);
        OutputGuardrailResult result = guardrail.validate(message);
        assertTrue(result.isRetry());
    }

    @Test
    void dangerousRouteKeywordShouldRetry() {
        String json = """
                {
                    "vehicle": {"type": "SUV", "model": "Toyota RAV4", "reasoning": "Good for off-road"},
                    "routeOverview": "Drive through a conflict area near the border",
                    "itinerary": [{"day": 1, "title": "Day 1", "description": "Travel through the region", "overnightStop": "Hotel"}],
                    "costs": {"total": "€2000"},
                    "tips": ["Stay alert"]
                }
                """;
        AiMessage message = AiMessage.from(json);
        OutputGuardrailResult result = guardrail.validate(message);
        assertTrue(result.isRetry());
    }

    @Test
    void dangerousKeywordInItineraryShouldRetry() {
        String json = """
                {
                    "vehicle": {"type": "SUV", "model": "Toyota RAV4", "reasoning": "Good for families"},
                    "routeOverview": "A beautiful coastal drive",
                    "itinerary": [{"day": 1, "title": "Day 1", "description": "Pass near a war zone checkpoint", "overnightStop": "Hotel"}],
                    "costs": {"total": "€2000"},
                    "tips": ["Pack sunscreen"]
                }
                """;
        AiMessage message = AiMessage.from(json);
        OutputGuardrailResult result = guardrail.validate(message);
        assertTrue(result.isRetry());
    }

    private String validTripPlanJson() {
        return """
                {
                    "vehicle": {"type": "SUV", "model": "Toyota RAV4", "reasoning": "Spacious and reliable"},
                    "routeOverview": "A scenic drive along the Italian Riviera coastline",
                    "itinerary": [
                        {"day": 1, "title": "Genoa to Camogli", "description": "Explore the colorful village of Camogli", "overnightStop": "Camogli"},
                        {"day": 2, "title": "Cinque Terre", "description": "Visit the five villages of Cinque Terre", "overnightStop": "Monterosso"}
                    ],
                    "costs": {
                        "vehiclePerDay": "€50", "fuel": "€80", "tolls": "€30",
                        "accommodation": "€400", "food": "€200", "activities": "€100", "total": "€860"
                    },
                    "tips": ["Pack comfortable walking shoes", "Book Cinque Terre train passes in advance"]
                }
                """;
    }
}
