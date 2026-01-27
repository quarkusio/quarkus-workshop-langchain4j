package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.declarative.SupervisorAgent;
import dev.langchain4j.agentic.declarative.SupervisorRequest;
import dev.langchain4j.service.SystemMessage;

/**
 * Supervisor agent that orchestrates the entire car processing workflow.
 * Coordinates feedback analysis agents and action agents based on car condition.
 */
public interface FleetSupervisorAgent {

    @SystemMessage("""
        You are a fleet supervisor for a car rental company. You coordinate action agents based on feedback analysis.
        
        The feedback has already been analyzed and you have these inputs:
        - cleaningRequest: What cleaning is needed (or "CLEANING_NOT_REQUIRED")
        - maintenanceRequest: What maintenance is needed (or "MAINTENANCE_NOT_REQUIRED")
        - dispositionRequest: Whether severe damage requires disposition (or "DISPOSITION_NOT_REQUIRED")
        
        Your job is to invoke the appropriate ACTION agents:
        
        DECISION LOGIC:
        
        1. If dispositionRequest contains "DISPOSITION_REQUIRED":
           - ALWAYS invoke PricingAgent first to estimate car value
           - Then invoke DispositionAgent to decide: SCRAP/SELL/DONATE/KEEP
           - If DispositionAgent says KEEP, then invoke MaintenanceAgent or CleaningAgent as needed
        
        2. If dispositionRequest is "DISPOSITION_NOT_REQUIRED":
           - If maintenanceRequest is NOT "MAINTENANCE_NOT_REQUIRED": Invoke MaintenanceAgent
           - If cleaningRequest is NOT "CLEANING_NOT_REQUIRED": Invoke CleaningAgent
           - You can invoke both if both are needed
        
        Explain your decision-making clearly, including which agents you invoked and why.
        """)
    @SupervisorAgent(
        outputKey = "supervisorDecision",
        subAgents = {
            PricingAgent.class,
            DispositionAgent.class,
            MaintenanceAgent.class,
            CleaningAgent.class
        }
    )
    String superviseCarProcessing(
        String carMake,
        String carModel,
        Integer carYear,
        Long carNumber,
        String carCondition,
        String rentalFeedback,
        String cleaningFeedback,
        String maintenanceFeedback,
        String cleaningRequest,
        String maintenanceRequest,
        String dispositionRequest
    );

    @SupervisorRequest
    static String request(
        String carMake,
        String carModel,
        Integer carYear,
        Long carNumber,
        String carCondition,
        String cleaningRequest,
        String maintenanceRequest,
        String dispositionRequest,
        String rentalFeedback
    ) {
        // Determine if disposition is required
        boolean dispositionRequired = dispositionRequest != null &&
                                     dispositionRequest.toUpperCase().contains("DISPOSITION_REQUIRED");
        
        if (!dispositionRequired) {
            // No disposition needed - simple path
            return String.format("""
                ═══════════════════════════════════════════════════════════════════════════
                ✅ NO DISPOSITION REQUIRED
                ═══════════════════════════════════════════════════════════════════════════
                
                Car: %d %s %s (#%d)
                Current Condition: %s
                
                Disposition Request: %s
                Cleaning Request: %s
                Maintenance Request: %s
                
                INSTRUCTIONS:
                - DO NOT invoke PricingAgent
                - DO NOT invoke DispositionAgent
                - Only invoke MaintenanceAgent if maintenance needed
                - Only invoke CleaningAgent if cleaning needed
                """,
                carYear, carMake, carModel, carNumber, carCondition,
                dispositionRequest, cleaningRequest, maintenanceRequest
            );
        }
        
        // Disposition required - complex path
        return String.format("""
            ═══════════════════════════════════════════════════════════════════════════
            ⚠️  DISPOSITION REQUIRED - FOLLOW WORKFLOW
            ═══════════════════════════════════════════════════════════════════════════
            
            Car: %d %s %s (#%d)
            Current Condition: %s
            Rental Feedback: %s
            
            Disposition Request: %s
            Cleaning Request: %s
            Maintenance Request: %s
            
            STEP 1: Invoke PricingAgent to get car value
            STEP 2: Invoke DispositionAgent to decide disposition action (SCRAP/SELL/DONATE/KEEP)
            STEP 3: If DispositionAgent decides KEEP:
                    - Invoke MaintenanceAgent if maintenance needed
                    - Invoke CleaningAgent if cleaning needed
            
            IMPORTANT: When invoking DispositionAgent:
            - Pass carValue as a STRING with dollar sign (e.g., "$10,710" not 10710)
            - Use the EXACT format from PricingAgent's response
            
            Follow the decision logic in your system message carefully.
            """,
            carYear, carMake, carModel, carNumber, carCondition, rentalFeedback,
            dispositionRequest, cleaningRequest, maintenanceRequest
        );
    }
}

