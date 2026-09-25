package com.tripplanner.agentic.agents;

import com.tripplanner.model.TripPlan;
import com.tripplanner.guardrails.TripAppropriatenessGuardrail;
import dev.langchain4j.service.guardrail.OutputGuardrails;
import com.tripplanner.model.VehicleEvaluation;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.ChatModelSupplier;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.agentic.runtime.CdiBean;

public interface VehicleReviser {

    @UserMessage("""
            You are a vehicle recommendation specialist.
            Revise the current vehicle recommendation based on the evaluation feedback.
            Keep the same format but improve the choice to address the evaluators' suggestions.

            Current recommendation: {vehicle}
            Evaluation score: {evaluation}
            Trip type: {tripType}
            Number of travelers: {travelers}
            Budget: {budget}
            Destination: {destination}
            Additional preferences: {preferences}
            Preserve the original traveler, budget, and preference constraints.
            """)
    @OutputGuardrails(value = TripAppropriatenessGuardrail.class, maxRetries = 3)
    @Agent(description = "Revises the vehicle recommendation based on evaluation feedback",
           outputKey = "vehicle")
    TripPlan.VehicleRecommendation revise(TripPlan.VehicleRecommendation vehicle,
                                          VehicleEvaluation evaluation,
                                          String tripType,
                                          String travelers,
                                          String budget,
                                          String destination,
                                          String preferences);

    @ChatModelSupplier
    static ChatModel chatModel(@CdiBean DynamicModelSelector modelSelector,
                               VehicleEvaluation evaluation) {
        return modelSelector.select(evaluation);
    }
}
