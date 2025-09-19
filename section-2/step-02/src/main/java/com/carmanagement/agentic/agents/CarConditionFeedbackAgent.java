package com.carmanagement.agentic.agents;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.agentic.Agent;

// --8<-- [start:carConditionFeedbackAgent]
/**
 * Agent that analyzes feedback to update the car condition.
 */
public interface CarConditionFeedbackAgent {

    @SystemMessage("""
        /nothink, Reasoning: low.
        You are a car condition analyzer for a car rental company. Your job is to determine the current condition of a car based on feedback.
        Analyze all feedback and the previous car condition to provide an updated condition description.
        Always provide a concise condition description, even if there's minimal feedback.
        Do not add any headers or prefixes to your response.
        """)
    @UserMessage("""
            Car Information:
            Make: {{carMake}}
            Model: {{carModel}}
            Year: {{carYear}}
            Previous Condition: {{carCondition}}
            
            Rental Feedback: {{rentalFeedback}}
            Car Wash Feedback: {{carWashFeedback}}
            """)
    @Agent("Car condition analyzer. Determines the current condition of a car based on feedback.")
    String analyzeForCondition(
            @V("carMake") String carMake,
            @V("carModel") String carModel,
            @V("carYear") Integer carYear,
            @V("carNumber") Integer carNumber,
            @V("carCondition") String carCondition,
            @V("rentalFeedback") String rentalFeedback,
            @V("carWashFeedback") String carWashFeedback);
}
// --8<-- [end:carConditionFeedbackAgent]


