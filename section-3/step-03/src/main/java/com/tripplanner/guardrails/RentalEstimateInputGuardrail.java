package com.tripplanner.guardrails;

import com.tripplanner.agentic.tools.RentalPricingTool;
import io.quarkiverse.langchain4j.guardrails.ToolInputGuardrail;
import io.quarkiverse.langchain4j.guardrails.ToolInputGuardrailRequest;
import io.quarkiverse.langchain4j.guardrails.ToolInputGuardrailResult;
import io.quarkus.logging.Log;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RentalEstimateInputGuardrail implements ToolInputGuardrail {

    @Override
    public ToolInputGuardrailResult validate(ToolInputGuardrailRequest request) {
        JsonObject arguments;
        try {
            arguments = request.argumentsAsJson();
        } catch (RuntimeException e) {
            return reject("Supply a JSON object with category and days.");
        }

        if (!(arguments.getValue("category") instanceof String category)
                || !RentalPricingTool.DAILY_RATES.containsKey(category)) {
            return reject("Choose category compact, estate, suv, or mpv (lowercase).");
        }

        if (!(arguments.getValue("days") instanceof Integer days) || days < 1 || days > 30) {
            return reject("Set days to a whole number from 1 to 30.");
        }

        Log.info("🛡️ [RentalEstimateInputGuardrail] PASS — Rental arguments accepted");
        return ToolInputGuardrailResult.success();
    }

    private ToolInputGuardrailResult reject(String reason) {
        Log.infof("🛡️ [RentalEstimateInputGuardrail] REJECT — %s", reason);
        return ToolInputGuardrailResult.failure(reason);
    }
}
