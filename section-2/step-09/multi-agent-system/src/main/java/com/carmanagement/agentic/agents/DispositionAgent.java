package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that determines how to dispose of a car based on value, condition, and damage.
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

        Your response must include:
        1. Final Workflow Action with unique marker: __DISPOSE_CAR__ or __KEEP_CAR__
        2. Disposition Recommendation with unique marker: __SCRAP__ or __SELL__ or __DONATE__ or __KEEP__
        3. Reasoning: Clear explanation of your recommendation

        Use __DISPOSE_CAR__ when the recommendation is __SCRAP__, __SELL__, or __DONATE__.
        Use __KEEP_CAR__ when the recommendation is __KEEP__.

        Format your response as:
        Final Workflow Action: __[DISPOSE_CAR/KEEP_CAR]__
        Disposition Recommendation: __[SCRAP/SELL/DONATE/KEEP]__
        Reasoning: [Your detailed explanation]

        CRITICAL: Use double underscores around both markers.
        """)
    @UserMessage("""
        Determine the disposition for this vehicle:
        - Make: {carMake}
        - Model: {carModel}
        - Year: {carYear}
        - Car Number: {carNumber}
        - Current Condition: {carCondition}
        - Estimated Value: {carValue}
        - Damage/Feedback: {feedback}

        Provide your final workflow action, disposition recommendation, and explanation.
        """)
    @Agent(outputKey = "dispositionAction", description = "Car disposition specialist. Determines how to dispose of a car based on value and condition.")
    String processDisposition(
            String carMake,
            String carModel,
            Integer carYear,
            Integer carNumber,
            String carCondition,
            String carValue,
            String feedback);
}
