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
          "carAssignment": "DISPOSITION|MAINTENANCE|CLEANING|NONE",
          "dispositionStatus": "DISPOSITION_APPROVED|DISPOSITION_REJECTED|DISPOSITION_NOT_REQUIRED",
          "dispositionReason": "reason or null"
        }
        
        Rules:
        - carAssignment: DISPOSE_CAR→DISPOSITION, KEEP_CAR+maintenance→MAINTENANCE, KEEP_CAR+cleaning→CLEANING, KEEP_CAR+none→NONE
        - dispositionStatus: APPROVED_BY_USER→DISPOSITION_APPROVED, REJECTED_BY_USER→DISPOSITION_REJECTED, else→DISPOSITION_NOT_REQUIRED
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
    @Agent(description = "Final car condition analyzer. Determines the car's condition, assignment, and approval status based on all feedback.",
            outputKey = "carConditions")
    CarConditions analyzeForCondition(
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
