package com.carmanagement.agentic.agents;

import com.carmanagement.model.CarInfo;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that analyzes feedback to update the car condition.
 */
public interface CarConditionFeedbackAgent {

    @SystemMessage("""
        You are a car condition analyzer for a car rental company. Your job is to determine the current condition of a car based on feedback.
        Analyze all feedback and the previous car condition to provide an updated condition description.
        Always provide a very short (no more than 200 characters) condition description, even if there's minimal feedback.
        Do not add any headers or prefixes to your response.
        """)
    @UserMessage("""
            Car Information:
            Make: {carInfo.make}
            Model: {carInfo.model}
            Year: {carInfo.year}
            Previous Condition: {carInfo.condition}
            
            Feedback from other agents:
            Cleaning Recommendation: {cleaningRequest}
            Maintenance Recommendation: {maintenanceRequest}
            """)
    @Agent(description = "Car condition analyzer. Determines the current condition of a car based on feedback.",
            outputKey = "carCondition")
    String analyzeForCondition(
            CarInfo carInfo,
            Integer carNumber,
            String cleaningRequest,
            String maintenanceRequest);
}


