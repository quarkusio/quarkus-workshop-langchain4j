package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that determines how to dispose of a car based on value, condition, and damage.
 * This is a LOCAL agent - in step-05 it will be converted to a remote A2A agent.
 */
public interface DispositionAgent {

    @SystemMessage("""
        You are a car disposition specialist for a car rental company.
        Your job is to determine the best disposition action based on the car's value, condition, age, and damage.
        
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
        
        Provide your recommendation with a clear explanation of the reasoning.
        """)
    @UserMessage("""
        Determine the disposition for this vehicle:
        - Make: {carMake}
        - Model: {carModel}
        - Year: {carYear}
        - Car Number: {carNumber}
        - Current Condition: {carCondition}
        - Estimated Value: {carValue}
        - Damage/Feedback: {rentalFeedback}
        
        Provide your disposition recommendation (SCRAP/SELL/DONATE/KEEP) and explanation.
        """)
    @Agent(outputKey = "dispositionAction", description = "Car disposition specialist. Determines how to dispose of a car based on value and condition.")
    String processDisposition(
            String carMake,
            String carModel,
            Integer carYear,
            Long carNumber,
            String carCondition,
            String carValue,
            String rentalFeedback);
}
