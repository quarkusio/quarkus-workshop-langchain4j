package com.tripplanner.guardrails;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.agentic.tools.RentalPricingTool;
import io.quarkiverse.langchain4j.guardrails.ToolInputGuardrail;
import io.quarkiverse.langchain4j.guardrails.ToolInputGuardrailRequest;
import io.quarkiverse.langchain4j.guardrails.ToolInputGuardrailResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RentalEstimateInputGuardrail implements ToolInputGuardrail {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    GuardrailAuditLog auditLog;

    @Override
    public ToolInputGuardrailResult validate(ToolInputGuardrailRequest request) {
        JsonNode arguments;
        try {
            arguments = objectMapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(request.arguments());
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return reject("Supply a JSON object with category and days.");
        }
        if (arguments == null || !arguments.isObject()) {
            return reject("Supply a JSON object with category and days.");
        }

        JsonNode category = arguments.path("category");
        if (!category.isTextual() || !RentalPricingTool.DAILY_RATES.containsKey(category.textValue())) {
            return reject("Choose category compact, estate, suv, or mpv (lowercase).");
        }

        JsonNode days = arguments.path("days");
        if (!days.isIntegralNumber() || !days.canConvertToInt() || days.intValue() < 1 || days.intValue() > 30) {
            return reject("Set days to a whole number from 1 to 30.");
        }

        auditLog.log("RentalEstimateInputGuardrail", "PASS", "Rental arguments accepted");
        return ToolInputGuardrailResult.success();
    }

    private ToolInputGuardrailResult reject(String reason) {
        auditLog.log("RentalEstimateInputGuardrail", "REJECT", reason);
        return ToolInputGuardrailResult.failure(reason);
    }
}
