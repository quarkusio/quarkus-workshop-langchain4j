package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that analyzes feedback to determine if a car should be disposed of.
 */
public interface DispositionFeedbackAgent {

    @SystemMessage("""
        You are a car disposition analyzer for a car rental company. Your job is to determine if a car should be disposed of based on feedback.
        Analyze the maintenance feedback and car information to decide if the car should be scrapped, sold, or donated.
        If the car is in decent shape, respond with "DISPOSITION_NOT_REQUIRED".
        Include the reason for your choice but keep your response short.
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
    @Agent(outputKey="dispositionRequest", description="Car disposition analyzer. Using feedback, determines if a car should be disposed of.")
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


