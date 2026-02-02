package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.declarative.SupervisorAgent;
import dev.langchain4j.agentic.declarative.SupervisorRequest;

/**
 * Supervisor agent that orchestrates the entire car processing workflow.
 * Coordinates feedback analysis agents and action agents based on car condition.
 */
public interface FleetSupervisorAgent {

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

        boolean dispositionRequired = dispositionRequest != null &&
                dispositionRequest.toUpperCase().contains("DISPOSITION_REQUIRED");

        String noDispositionMessage = """
               No disposition has been requested.
               
                INSTRUCTIONS:
                - DO NOT invoke PricingAgent
                - DO NOT invoke DispositionAgent
                - Only invoke MaintenanceAgent if maintenance needed
                - Only invoke CleaningAgent if cleaning needed
               """;

        // Disposition required - complex path
        String dispositionMessage = """
            The car has to be disposed.

            STEP 1: Invoke PricingAgent to get car value
            STEP 2: Invoke DispositionAgent to decide disposition action (SCRAP/SELL/DONATE/KEEP)
            STEP 3: If DispositionAgent decides KEEP:
                    - Invoke MaintenanceAgent if maintenance needed
                    - Invoke CleaningAgent if cleaning needed
            
            IMPORTANT: When invoking DispositionAgent:
            - Pass carValue as a STRING with dollar sign (e.g., "$10,710" not 10710)
            - Use the EXACT format from PricingAgent's response
            
            Follow the decision logic in your system message carefully.
            """;

        return String.format("""
            You are a fleet supervisor for a car rental company. You coordinate action agents based on feedback analysis.
            
            The feedback has already been analyzed and you have these inputs:
            - cleaningRequest: What cleaning is needed (or "CLEANING_NOT_REQUIRED")
            - maintenanceRequest: What maintenance is needed (or "MAINTENANCE_NOT_REQUIRED")
            - dispositionRequest: Whether severe damage requires disposition (or "DISPOSITION_NOT_REQUIRED")
            
            Your job is to invoke the appropriate ACTION agents for this car
            
            Car: %d %s %s (#%d)
            Current Condition: %s
            Rental Feedback: %s
            
            Cleaning Request: %s
            Maintenance Request: %s
            
            In particular, your have to follow these steps
            
            %s
            """,
            carYear, carMake, carModel, carNumber, carCondition, rentalFeedback,
            cleaningRequest, maintenanceRequest, dispositionRequired ? dispositionMessage : noDispositionMessage);
    }
}

