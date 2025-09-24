package com.carmanagement.agentic.agents;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.agentic.Agent;

/**
 * Agent that analyzes feedback to determine if a car should be disposed of.
 */
public interface DispositionFeedbackAgent {

    @SystemMessage("""
        /nothink, Reasoning: low.
        You are a car disposition analyzer for a car rental company. Your job is to determine if a car should be disposed of based on feedback.
        Analyze the maintenance feedback and car information to decide if the car should be scrapped, sold, or donated.
        If the car is in decent shape, respond with "DISPOSITION_NOT_REQUIRED".
        Include the reason for your choice but keep your response short.
        """)
    @UserMessage("""
        Car Information:
        Make: {{carMake}}
        Model: {{carModel}}
        Year: {{carYear}}
        Previous Condition: {{carCondition}}
        
        Feedback:
        Rental Feedback: {{rentalFeedback}}
        Car Wash Feedback: {{carWashFeedback}}
        Maintenance Feedback: {{maintenanceFeedback}}
        """)
    @Agent(outputName="dispositionRequest", description="Car disposition analyzer. Using feedback, determines if a car should be disposed of.")
    String analyzeForDisposition(
            @V("carMake") String carMake,
            @V("carModel") String carModel,
            @V("carYear") Integer carYear,
            @V("carNumber") Integer carNumber,
            @V("carCondition") String carCondition,
            @V("rentalFeedback") String rentalFeedback,
            @V("carWashFeedback") String carWashFeedback,
            @V("maintenanceFeedback") String maintenanceFeedback);
}


