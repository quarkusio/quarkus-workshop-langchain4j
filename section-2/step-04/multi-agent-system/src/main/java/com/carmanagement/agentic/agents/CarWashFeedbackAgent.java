package com.carmanagement.agentic.agents;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.agentic.Agent;

/**
 * Agent that analyzes feedback to determine if a car wash is needed.
 */
public interface CarWashFeedbackAgent {

    @SystemMessage("""
        /nothink, Reasoning: low.
        You are a car wash analyzer for a car rental company. Your job is to determine if a car needs washing based on feedback.
        Analyze the feedback and car information to decide if a car wash is needed.
        If the feedback mentions dirt, mud, stains, or anything that suggests the car is dirty, recommend a car wash.
        Be specific about what type of car wash is needed (exterior, interior, detailing, waxing).
        If no interior or exterior car cleaning services are needed based on the feedback, respond with "CARWASH_NOT_REQUIRED".
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
    @Agent("Car wash analyzer. Using feedback, determines if a car wash is needed.")
    String analyzeForCarWash(
            @V("carMake") String carMake,
            @V("carModel") String carModel,
            @V("carYear") Integer carYear,
            @V("carNumber") Integer carNumber,
            @V("carCondition") String carCondition,
            @V("rentalFeedback") String rentalFeedback,
            @V("carWashFeedback") String carWashFeedback,
            @V("maintenanceFeedback") String maintenanceFeedback);
}


