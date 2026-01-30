package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that creates disposition proposals for vehicles requiring disposition.
 * This agent analyzes the vehicle and creates a proposal that will be reviewed
 * by the HumanApprovalAgent if the vehicle value exceeds the threshold.
 */
public interface DispositionProposalAgent {

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
        - Damage/Feedback: {rentalFeedback}
        
        Provide your disposition proposal with clear reasoning.
        """)
    @Agent(outputKey = "dispositionProposal", description = "Creates disposition proposals for vehicles requiring disposition")
    String createDispositionProposal(
            String carMake,
            String carModel,
            Integer carYear,
            Long carNumber,
            String carCondition,
            String carValue,
            String rentalFeedback);
}


