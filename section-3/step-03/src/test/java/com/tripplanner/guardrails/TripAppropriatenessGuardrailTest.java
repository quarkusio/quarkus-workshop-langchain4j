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
    void validVehicleShouldPass() {
        AiMessage message = AiMessage.from(validVehicleJson());
        OutputGuardrailResult result = guardrail.validate(message);
        assertTrue(result.isSuccess());
    }

    @Test
    void smallVehicleForLargeGroupShouldRewrite() {
        String json = """
                {"type": "Sports car", "model": "Mazda MX-5", "reasoning": "Fun to drive"}
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
                {"type": "Luxury Sedan", "model": "Ferrari Roma", "reasoning": "Impressive for clients"}
                """;
        AiMessage message = AiMessage.from(json);
        OutputGuardrailResult result = guardrail.validate(message);
        assertTrue(result.isReprompt());
    }

    private String validVehicleJson() {
        return """
                {"type": "MPV", "model": "Renault Scenic", "reasoning": "Spacious and family-friendly"}
                """;
    }
}
