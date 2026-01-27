package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that analyzes feedback to determine if a car should be considered for disposition.
 */
public interface DispositionFeedbackAgent {

    @SystemMessage("""
        You are a disposition analyzer for a car rental company. Your job is to determine if a car should be considered for disposition (removal from fleet).
        
        Analyze the feedback for SEVERE issues that would make the car uneconomical to keep:
        - Major accidents: "wrecked", "totaled", "destroyed", "crashed", "collision"
        - Severe damage: "frame damage", "structural damage", "major damage"
        - Safety concerns: "unsafe", "not drivable", "inoperable", "dangerous"
        - Catastrophic mechanical failure: "engine blown", "transmission failed", "major mechanical failure"
        
        If you detect ANY of these severe issues, respond with:
        "DISPOSITION_REQUIRED: [brief description of the severe issue]"
        
        If the car has only minor or moderate issues that can be repaired, respond with:
        "DISPOSITION_NOT_REQUIRED"
        
        Keep your response concise.
        """)
    @UserMessage("""
        Car Information:
        Make: {carMake}
        Model: {carModel}
        Year: {carYear}
        Previous Condition: {carCondition}
        
        Feedback:
        Rental Feedback: {rentalFeedback}
        Cleaning Feedback: {cleaningFeedback}
        Maintenance Feedback: {maintenanceFeedback}
        """)
    @Agent(description = "Disposition analyzer. Determines if a car has severe damage requiring disposition evaluation.",
            outputKey = "dispositionRequest")
    String analyzeForDisposition(
            String carMake,
            String carModel,
            Integer carYear,
            Long carNumber,
            String carCondition,
            String rentalFeedback,
            String cleaningFeedback,
            String maintenanceFeedback);
}

