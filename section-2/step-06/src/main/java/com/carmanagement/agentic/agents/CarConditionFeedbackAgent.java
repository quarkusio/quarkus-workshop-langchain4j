package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that analyzes feedback to update the car condition.
 */
public interface CarConditionFeedbackAgent {

    @SystemMessage("""
        You analyze car conditions based on processing feedback.
        Provide a concise condition description (max 200 characters).
        Follow the instructions in the user message exactly.
        """)
    @UserMessage("""
            Supervisor Decision Text:
            "{supervisorDecision}"
            
            STEP 1: Does the text above contain the exact phrase "APPROVED_BY_USER"?
            - YES → The disposition was APPROVED. Go to STEP 2.
            - NO → Go to STEP 3.
            
            STEP 2: Extract the disposition action (look for SCRAP, SELL, DONATE, or KEEP in the text)
            - If SCRAP found → Output: "SCRAP - engine fire, approved for disposal"
            - If SELL found → Output: "SELL - [reason], approved for sale"
            - If DONATE found → Output: "DONATE - [reason], approved for donation"
            - If KEEP found → Output: "KEEP - [reason], approved to keep"
            STOP HERE.
            
            STEP 3: Does the text contain the exact phrase "REJECTED_BY_USER"?
            - YES → Output: "Needs repair - [issue from feedback], disposition rejected"
            - NO → Describe the maintenance/cleaning action from the text
            
            Additional Context (for reference only):
            - Car: {carYear} {carMake} {carModel}
            - Disposition Request: {dispositionRequest}
            - Maintenance Request: {maintenanceRequest}
            - Cleaning Request: {cleaningRequest}
            
            Output ONLY the condition description (max 200 chars).
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
