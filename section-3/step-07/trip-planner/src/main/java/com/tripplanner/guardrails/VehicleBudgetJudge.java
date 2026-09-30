package com.tripplanner.guardrails;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * LLM-as-judge that evaluates whether a vehicle recommendation is appropriate
 * for the given budget tier. Used by {@link TripAppropriatenessGuardrail} in
 * place of a hardcoded brand allowlist.
 *
 * <p>Runs on a fast, cheap model (configured as {@code judgeModel}) to keep
 * latency low. Returns a structured {@link BudgetVerdict} with a boolean verdict
 * and a short explanation suitable for logging.
 */
@ApplicationScoped
@RegisterAiService(modelName = "judgeModel")
public interface VehicleBudgetJudge {

    @SystemMessage("""
            You are a vehicle budget compliance judge.
            Your only job is to decide whether a vehicle recommendation is appropriate for a given budget tier.
            Economy budgets (roughly €500–€1000 total or €30–€50/day) require affordable, mass-market vehicles.
            Premium or luxury vehicles — regardless of brand — are not appropriate for economy budgets.
            Be strict: if there is any doubt, mark the vehicle as not appropriate.
            Respond only with a JSON object matching the BudgetVerdict schema.
            """)
    @UserMessage("""
            Budget tier: {budget}
            Vehicle type: {vehicleType}
            Vehicle model: {vehicleModel}
            Vehicle reasoning: {reasoning}

            Is this vehicle appropriate for the stated budget tier?
            """)
    BudgetVerdict evaluate(String budget, String vehicleType, String vehicleModel, String reasoning);
}
