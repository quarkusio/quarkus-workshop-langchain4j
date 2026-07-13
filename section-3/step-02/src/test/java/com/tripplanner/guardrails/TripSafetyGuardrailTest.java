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
    void validItineraryShouldPass() {
        AiMessage message = AiMessage.from(validItineraryResultJson());
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
                    "routeOverview": "A scenic coastal route",
                    "itinerary": []
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
                    "routeOverview": "Drive through a conflict area near the border",
                    "itinerary": [{"day": 1, "title": "Day 1", "description": "Travel through the region", "overnightStop": "Hotel"}]
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
                    "routeOverview": "A beautiful coastal drive",
                    "itinerary": [{"day": 1, "title": "Day 1", "description": "Pass near a war zone checkpoint", "overnightStop": "Hotel"}]
                }
                """;
        AiMessage message = AiMessage.from(json);
        OutputGuardrailResult result = guardrail.validate(message);
        assertTrue(result.isRetry());
    }

    private String validItineraryResultJson() {
        return """
                {
                    "routeOverview": "A scenic drive along the Italian Riviera coastline",
                    "itinerary": [
                        {"day": 1, "title": "Genoa to Camogli", "description": "Explore the colorful village of Camogli", "overnightStop": "Camogli"},
                        {"day": 2, "title": "Cinque Terre", "description": "Visit the five villages of Cinque Terre", "overnightStop": "Monterosso"}
                    ]
                }
                """;
    }
}
