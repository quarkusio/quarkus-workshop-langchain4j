package com.carmanagement.agentic.agents;

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
        Always provide a concise condition description, even if there's minimal feedback.
        Do not add any headers or prefixes to your response.
        """)
    @UserMessage("""
            Car Information:
            Make: {carMake}
            Model: {carModel}
            Year: {carYear}
            Previous Condition: {carCondition}
            
            Feedback from other agents:
            Car Wash Recommendation: {carWashRequest}
            Maintenance Recommendation: {maintenanceRequest}
            Disposition Recommendation: {dispositionRequest}
            """)
    @Agent(description = "Car condition analyzer. Determines the current condition of a car based on feedback.",
            outputName = "carCondition")
    String analyzeForCondition(
            String carMake,
            String carModel,
            Integer carYear,
            Long carNumber,
            String carCondition,
            String carWashRequest,
            String maintenanceRequest,
            String dispositionRequest);
}


