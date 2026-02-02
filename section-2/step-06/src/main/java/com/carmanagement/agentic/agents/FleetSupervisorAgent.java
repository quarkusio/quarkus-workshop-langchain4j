package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.declarative.SupervisorAgent;
import dev.langchain4j.agentic.declarative.SupervisorRequest;

/**
 * Supervisor agent that orchestrates the entire car processing workflow.
 * Coordinates feedback analysis agents and action agents based on car condition.
 * Implements TRUE human-in-the-loop pattern for high-value vehicle dispositions.
 */
public interface FleetSupervisorAgent {

    @SupervisorAgent(
        outputKey = "supervisorDecision",
        subAgents = {
            PricingAgent.class,
            DispositionProposalAgent.class,
            HumanApprovalAgent.class,
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
                Disposition: NOT REQUIRED
                → Include "APPROVAL_NOT_REQUIRED" in response
                """;

    String dispositionMessage = """
           Disposition: REQUIRED
           
           Follow this steps:
           
           1. Get value from PricingAgent (keep $ format)
           2. IF value > $15,000 (HIGH-VALUE):
              - Invoke DispositionProposalAgent → HumanApprovalAgent (workflow pauses)
              - APPROVED: Use AI recommendation → KEEP→"KEEP_CAR", DISPOSE→"DISPOSE_CAR"
              - REJECTED: Opposite of AI → KEEP→"DISPOSE_CAR", DISPOSE→"KEEP_CAR"
           3. IF value ≤ $15,000 (LOW-VALUE):
              - Invoke DispositionAgent directly
              - KEEP→"KEEP_CAR", SCRAP/SELL/DONATE→"DISPOSE_CAR"
           4. IF "KEEP_CAR": Invoke MaintenanceAgent/CleaningAgent as needed otherwise invoke DispositionAgent
           
           CRITICAL: End with KEEP_CAR or DISPOSE_CAR
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
            
            %s
            """,
                carYear, carMake, carModel, carNumber, carCondition, rentalFeedback,
                cleaningRequest, maintenanceRequest, dispositionRequired ? dispositionMessage : noDispositionMessage);
    }
}


