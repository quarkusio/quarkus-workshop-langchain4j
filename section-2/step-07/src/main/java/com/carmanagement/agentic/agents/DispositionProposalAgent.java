package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.ChatModelSupplier;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.ModelName;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;

/**
 * Agent that creates disposition proposals for vehicles requiring disposition.
 * This agent analyzes the vehicle and creates a proposal that will be reviewed
 * by the HumanApprovalAgent if the vehicle value exceeds the threshold.
 */
public class DispositionProposalAgent {

    private static final int HIGH_VALUE_THRESHOLD = 30000;

    @Inject
    ChatModel baseModel;

    @Inject
    @ModelName("advancedModel")
    ChatModel advancedModel;

    @SystemMessage("""
        You are a car disposition specialist for a car rental company.
        Your job is to create a disposition proposal based on the car's value, condition, age, and damage.
        
        Disposition Options:
        - SCRAP: Car is beyond economical repair or has severe safety concerns
        - SELL: Car has value but is aging out of the fleet or has moderate damage
        - DONATE: Car has minimal value but could serve a charitable purpose
        - KEEP: Car is worth keeping in the fleet
        
        Decision Criteria:
        - If estimated repair cost > 50% of car value: Consider SCRAP or SELL
        - If car is over 5 years old with significant damage: SCRAP
        - If car is 3-5 years old in fair condition: SELL
        - If car has low value (<$5,000) but functional: DONATE
        - If car is valuable and damage is minor: KEEP
        
        Your response must include:
        1. Proposed Action with unique marker: __SCRAP__ or __SELL__ or __DONATE__ or __KEEP__
        2. Reasoning: Clear explanation of your recommendation
        
        Format your response as:
        Proposed Action: __[SCRAP/SELL/DONATE/KEEP]__
        Reasoning: [Your detailed explanation]
        
        CRITICAL: Use double underscores around the action (e.g., __KEEP__ not KEEP)
        """)
    @UserMessage("""
        Create a disposition proposal for this vehicle:
        - Make: {carMake}
        - Model: {carModel}
        - Year: {carYear}
        - Car Number: {carNumber}
        - Current Condition: {carCondition}
        - Estimated Value: {carValue}
        - Damage/Feedback: {feedback}

        Provide your disposition proposal with clear reasoning.
        """)
    @Agent(outputKey = "dispositionProposal", description = "Creates disposition proposals for vehicles requiring disposition")
    public native String createDispositionProposal(
            String carMake,
            String carModel,
            Integer carYear,
            Integer carNumber,
            String carCondition,
            String carValue,
            String feedback);

    @ChatModelSupplier
    ChatModel chatModel(String carValue) {
        if (parseValue(carValue) > HIGH_VALUE_THRESHOLD) {
            Log.info("Selecting advanced model for high-value car estimated " + carValue);
            return advancedModel;
        }
        return baseModel;
    }

    private static int parseValue(String carValue) {
        if (carValue == null) {
            return 0;
        }
        String digits = carValue.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(digits);
    }
}


