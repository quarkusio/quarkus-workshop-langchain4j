package com.tripplanner.guardrails;

import com.tripplanner.model.TripRequest;
import com.tripplanner.model.TripRequestContext;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TripAppropriatenessGuardrailTest {

    @Inject
    TripAppropriatenessGuardrail guardrail;

    @Inject
    TripRequestContext tripRequestContext;

    @BeforeEach
    void setUp() {
        tripRequestContext.set(new TripRequest("Italian Riviera", 5, "family", 4, "moderate (€1000-€2500)", "coastal towns"));
    }

    @Test
    void validFamilyPlanShouldPass() {
        AiMessage message = AiMessage.from(validFamilyPlanJson());
        OutputGuardrailResult result = guardrail.validate(message);
        assertTrue(result.isSuccess());
    }

    @Test
    void smallVehicleForLargeGroupShouldRewrite() {
        String json = """
                {
                    "vehicle": {"type": "Sports car", "model": "Mazda MX-5", "reasoning": "Fun to drive"},
                    "routeOverview": "Coastal drive",
                    "itinerary": [{"day": 1, "title": "Day 1", "description": "Drive along the coast", "overnightStop": "Nice"}],
                    "costs": {"total": "€2000"},
                    "tips": ["Enjoy the ride"]
                }
                """;
        AiMessage message = AiMessage.from(json);
        OutputGuardrailResult result = guardrail.validate(message);
        assertTrue(result.isSuccess(), "Rewritten result should be a success");
        assertTrue(result.hasRewrittenResult());
    }

    @Test
    void luxuryVehicleOnEconomyBudgetShouldRetry() {
        tripRequestContext.set(new TripRequest("Milan", 3, "business", 2, "economy (€500-€1000)", "meetings"));
        String json = """
                {
                    "vehicle": {"type": "Luxury Sedan", "model": "Ferrari Roma", "reasoning": "Impressive for clients"},
                    "routeOverview": "City to city",
                    "itinerary": [{"day": 1, "title": "Day 1", "description": "Drive to meeting", "overnightStop": "Milan"}],
                    "costs": {"total": "€900"},
                    "tips": ["Book parking in advance"]
                }
                """;
        AiMessage message = AiMessage.from(json);
        OutputGuardrailResult result = guardrail.validate(message);
        assertTrue(result.isRetry());
    }

    @Test
    void familyTripWithInappropriateTipsShouldRewrite() {
        String json = """
                {
                    "vehicle": {"type": "MPV", "model": "Renault Scenic", "reasoning": "Spacious for families"},
                    "routeOverview": "Family-friendly coastal route",
                    "itinerary": [{"day": 1, "title": "Day 1", "description": "Visit the beach", "overnightStop": "Portofino"}],
                    "costs": {"total": "€1500"},
                    "tips": ["Great nightclub scene in the old town", "Pack sunscreen", "Try the local gelato"]
                }
                """;
        AiMessage message = AiMessage.from(json);
        OutputGuardrailResult result = guardrail.validate(message);
        assertTrue(result.isSuccess(), "Rewritten result should be a success");
        assertTrue(result.hasRewrittenResult());
        assertFalse(result.successfulText().contains("nightclub"));
    }

    @Test
    void familyTripWithInappropriateItineraryShouldRewrite() {
        String json = """
                {
                    "vehicle": {"type": "MPV", "model": "Renault Scenic", "reasoning": "Spacious for families"},
                    "routeOverview": "Coastal route",
                    "itinerary": [{"day": 1, "title": "Day 1", "description": "Enjoy the bar hopping district at night", "overnightStop": "Nice"}],
                    "costs": {"total": "€1500"},
                    "tips": ["Pack comfortable shoes"]
                }
                """;
        AiMessage message = AiMessage.from(json);
        OutputGuardrailResult result = guardrail.validate(message);
        assertTrue(result.isSuccess(), "Rewritten result should be a success");
        assertTrue(result.hasRewrittenResult());
        assertFalse(result.successfulText().contains("bar hopping"));
    }

    private String validFamilyPlanJson() {
        return """
                {
                    "vehicle": {"type": "MPV", "model": "Renault Scenic", "reasoning": "Spacious and family-friendly"},
                    "routeOverview": "A scenic coastal route along the Italian Riviera",
                    "itinerary": [
                        {"day": 1, "title": "Genoa to Camogli", "description": "Explore the village", "overnightStop": "Camogli"},
                        {"day": 2, "title": "Cinque Terre", "description": "Visit the five villages", "overnightStop": "Monterosso"}
                    ],
                    "costs": {"total": "€1800"},
                    "tips": ["Pack sunscreen", "Book train passes early"]
                }
                """;
    }
}
