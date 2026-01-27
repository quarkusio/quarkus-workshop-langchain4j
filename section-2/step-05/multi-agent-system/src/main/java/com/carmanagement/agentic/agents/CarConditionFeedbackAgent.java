package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that analyzes feedback to update the car condition.
 */
public interface CarConditionFeedbackAgent {

    @SystemMessage("""
        You are a car condition analyzer for a car rental company. Your job is to determine the current condition of a car based on all processing feedback.
        
        Analyze all feedback and provide an updated condition description.
        
        IMPORTANT: If disposition was required and a disposition decision was made (SCRAP/SELL/DONATE),
        include that decision and brief reasoning (e.g., "SCRAP - severe damage, low value").
        
        CRITICAL: Your response MUST be 200 characters or less. Be extremely concise.
        Do not add any headers or prefixes to your response.
        """)
    @UserMessage("""
            Car Information:
            Make: {carMake}
            Model: {carModel}
            Year: {carYear}
            Previous Condition: {carCondition}
            
            Feedback from agents:
            Disposition Analysis: {dispositionRequest}
            Cleaning Recommendation: {cleaningRequest}
            Maintenance Recommendation: {maintenanceRequest}
            
            Supervisor Decision: {supervisorDecision}
            """)
    @Agent(description = "Car condition analyzer. Determines the current condition of a car based on all feedback including disposition decisions.",
            outputKey = "carCondition")
    String analyzeForCondition(
            String carMake,
            String carModel,
            Integer carYear,
            Long carNumber,
            String carCondition,
            String dispositionRequest,
            String cleaningRequest,
            String maintenanceRequest,
            String supervisorDecision);
}
