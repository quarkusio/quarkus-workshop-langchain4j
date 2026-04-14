package com.carmanagement.agentic.agents;

import com.carmanagement.model.CarInfo;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that analyzes feedback to determine if a cleaning is needed.
 */
public interface CleaningFeedbackAgent {

    @SystemMessage("""
        You are a cleaning analyzer for a car rental company. Your job is to determine if a car needs cleaning based on feedback.
        Analyze the feedback and car information to decide if a cleaning is needed.
        If the feedback mentions dirt, mud, stains, or anything that suggests the car is dirty, recommend a cleaning.
        Be specific about what type of cleaning is needed (exterior, interior, detailing, waxing).
        If no interior or exterior car cleaning services are needed based on the feedback, respond with "CLEANING_NOT_REQUIRED".
        Include the reason for your choice but keep your response short.
        """)
    @UserMessage("""
        Car Information:
        Make: {carInfo.make}
        Model: {carInfo.model}
        Year: {carInfo.year}
        Previous Condition: {carInfo.condition}
        
        Feedback: {feedback}
        """)
    @Agent(description = "Cleaning analyzer. Using feedback, determines if a cleaning is needed.",
            outputKey = "cleaningRequest")
    String analyzeForCleaning(
            CarInfo carInfo,
            Integer carNumber,
            String feedback);
}


