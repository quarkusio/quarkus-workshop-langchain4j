package com.carmanagement.agentic.agents;

import com.carmanagement.model.CarConditions;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that analyzes feedback to determine the final car condition and assignment.
 * This is the final decision-maker that interprets all previous agent outputs.
 */
public interface CarConditionFeedbackAgent {

    @SystemMessage("""
        Analyze car processing results and output a JSON summary.
        
        Output format:
        {
          "generalCondition": "concise description (max 200 chars)",
          "carAssignment": "DISPOSITION|MAINTENANCE|CLEANING|NONE"
        }
        
        Rules:
        - carAssignment: Check the ACTUAL DispositionAgent decision in supervisorDecision, not just the request
        - If supervisorDecision mentions SCRAP/SELL/DONATE (but NOT KEEP) → DISPOSITION
        - Else if maintenanceRequest ≠ "MAINTENANCE_NOT_REQUIRED" → MAINTENANCE
        - Else if cleaningRequest ≠ "CLEANING_NOT_REQUIRED" → CLEANING
        - Else → NONE
        - IMPORTANT: If DispositionAgent decided KEEP, do NOT assign DISPOSITION - check maintenance/cleaning instead
        - generalCondition: Summarize the action and reason
        """)
    @UserMessage("""
            Car: {carYear} {carMake} {carModel} (#{carNumber})
            
            Supervisor Decision: {supervisorDecision}
            
            Requests:
            - Disposition: {dispositionRequest}
            - Maintenance: {maintenanceRequest}
            - Cleaning: {cleaningRequest}
            """)
    @Agent(description = "Final car condition analyzer. Determines the car's condition and assignment based on all feedback.",
            outputKey = "carConditions")
    CarConditions analyzeForCondition(
            String carMake,
            String carModel,
            Integer carYear,
            Integer carNumber,
            String carCondition,
            String dispositionRequest,
            String cleaningRequest,
            String maintenanceRequest,
            String supervisorDecision);
}


