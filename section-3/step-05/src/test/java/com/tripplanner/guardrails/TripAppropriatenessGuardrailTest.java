package com.tripplanner.guardrails;

import com.tripplanner.model.TripRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import dev.langchain4j.guardrail.OutputGuardrailRequest;
import dev.langchain4j.guardrail.ChatExecutor;
import dev.langchain4j.guardrail.GuardrailRequestParams;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.Map;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TripAppropriatenessGuardrailTest {

    @Inject
    TripAppropriatenessGuardrail guardrail;

    TripRequest tripRequest;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    GuardrailAuditLog auditLog;

    @BeforeEach
    void setUp() {
        tripRequest = new TripRequest("Italian Riviera", "2026-07-10", 5, "family", 4, "moderate (€1000-€2500)", "coastal towns");
    }

    @Test
    void validVehicleShouldPass() {
        AiMessage message = AiMessage.from(validVehicleJson());
        OutputGuardrailResult result = validate(message);
        assertTrue(result.isSuccess());
        assertFalse(result.hasRewrittenResult());
        assertEquals("PASS", auditLog.getRecentEntries().getLast().decision());
    }

    @Test
    void smallVehicleForLargeGroupShouldRewrite() throws Exception {
        String json = """
                {"type": "Sports car", "model": "Mazda MX-5", "reasoning": "Fun to drive"}
                """;
        AiMessage message = AiMessage.from(json);
        OutputGuardrailResult result = validate(message);
        assertTrue(result.isSuccess(), "Rewritten result should be a success");
        assertTrue(result.hasRewrittenResult());
        var replacement = objectMapper.readTree(result.successfulText());
        assertEquals("MPV", replacement.path("type").asText());
        assertEquals("Family MPV; specific model subject to availability.", replacement.path("model").asText());
        assertEquals("Vehicle corrected by guardrail: original recommendation was too small for 4 travelers. "
                + "Suggested category: MPV. Confirm seating, luggage capacity, price, and availability with the rental provider.",
                replacement.path("reasoning").asText());
        assertFalse(result.successfulText().contains("Mazda"));
        assertEquals("REWRITE", auditLog.getRecentEntries().getLast().decision());
    }

    @Test
    void luxuryVehicleOnEconomyBudgetShouldRetry() {
        tripRequest = new TripRequest("Milan", "2026-09-20", 3, "business", 2, "economy (€500-€1000)", "meetings");
        String json = """
                {"type": "Luxury Sedan", "model": "Ferrari Roma", "reasoning": "Impressive for clients"}
                """;
        AiMessage message = AiMessage.from(json);
        OutputGuardrailResult result = validate(message);
        assertTrue(result.isReprompt());
    }

    @Test
    void smallLuxuryVehicleOnEconomyBudgetMustRepromptBeforeRewriting() {
        tripRequest = new TripRequest("Milan", "2026-09-20", 3, "family", 4, "economy", "");
        var result = validate(AiMessage.from("""
                {"type":"Sports car","model":"Ferrari Roma","reasoning":"Fun to drive"}
                """));
        assertTrue(result.isReprompt());
        assertFalse(result.isSuccess());
        assertFalse(result.hasRewrittenResult());
        assertTrue(result.getReprompt().orElseThrow().contains("budget-friendly"));
        assertEquals("REPROMPT", auditLog.getRecentEntries().getLast().decision());

        var corrected = validate(AiMessage.from("""
                {"type":"Sports car","model":"Mazda MX-5","reasoning":"Affordable"}
                """));
        assertTrue(corrected.isSuccess());
        assertTrue(corrected.hasRewrittenResult());
        assertEquals("REWRITE", auditLog.getRecentEntries().getLast().decision());
    }

    @ParameterizedTest
    @ValueSource(strings = {"adventure", "business"})
    void genericRewriteMatchesTripCategory(String tripType) throws Exception {
        tripRequest = new TripRequest("Milan", "2026-09-20", 3, tripType, 4, "moderate", "");
        var result = validate(AiMessage.from("""
                {"type":"Convertible","model":"Mazda MX-5","reasoning":"Fun"}
                """));
        var replacement = objectMapper.readTree(result.successfulText());
        var category = tripType.equals("adventure") ? "SUV" : "Estate";
        assertEquals(category, replacement.path("type").asText());
        assertEquals(category + "; specific model subject to availability.", replacement.path("model").asText());
        assertTrue(replacement.path("reasoning").asText().contains("Suggested category: " + category));
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "not json", "null", "[]", "{}", "{\"type\":\"SUV\"}"})
    void unvalidatedTextIsSkippedNotPassed(String text) {
        assertTrue(validate(AiMessage.from(text)).isSuccess());
        assertEquals("SKIP", auditLog.getRecentEntries().getLast().decision());
    }

    @Test
    void toolCallContentIsSkippedNotPassed() {
        var message = AiMessage.from(ToolExecutionRequest.builder().name("vehicle").arguments("{}").build());
        assertTrue(validate(message).isSuccess());
        assertEquals("SKIP", auditLog.getRecentEntries().getLast().decision());
    }

    @Test
    void missingContextIsSkippedNotPassed() {
        tripRequest = null;
        assertTrue(validate(AiMessage.from(validVehicleJson())).isSuccess());
        assertEquals("SKIP", auditLog.getRecentEntries().getLast().decision());
    }

    private OutputGuardrailResult validate(AiMessage message) {
        Map<String, Object> variables = tripRequest == null ? Map.of() : Map.of(
                "travelers", String.valueOf(tripRequest.travelers()),
                "budget", tripRequest.budget(), "tripType", tripRequest.tripType());
        return guardrail.validate(OutputGuardrailRequest.builder()
                .responseFromLLM(ChatResponse.builder().aiMessage(message).build())
                .chatExecutor(new ChatExecutor() {
                    @Override
                    public ChatResponse execute() { throw new AssertionError("Unit validation must not call the model"); }
                    @Override
                    public ChatResponse execute(java.util.List<dev.langchain4j.data.message.ChatMessage> messages) {
                        return execute();
                    }
                })
                .requestParams(GuardrailRequestParams.builder()
                        .userMessageTemplate("Trip details").variables(variables).build())
                .build());
    }

    private String validVehicleJson() {
        return """
                {"type": "MPV", "model": "Renault Scenic", "reasoning": "Spacious and family-friendly"}
                """;
    }
}
