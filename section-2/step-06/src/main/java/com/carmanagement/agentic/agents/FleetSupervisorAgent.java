package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.declarative.SupervisorAgent;
import dev.langchain4j.agentic.declarative.SupervisorRequest;
import dev.langchain4j.service.SystemMessage;

/**
 * Supervisor agent that orchestrates the entire car processing workflow.
 * Coordinates feedback analysis agents and action agents based on car condition.
 * Implements TRUE human-in-the-loop pattern for high-value vehicle dispositions.
 */
public interface FleetSupervisorAgent {

    @SystemMessage("""
        Fleet supervisor coordinating car processing. Your response MUST end with KEEP_CAR or DISPOSE_CAR.
        
        WORKFLOW:
        
        IF dispositionRequest = "DISPOSITION_NOT_REQUIRED":
           → Handle cleaning/maintenance only → End with "KEEP_CAR"
        
        IF dispositionRequest = "DISPOSITION_REQUIRED":
           1. Get value from PricingAgent (keep $ format)
           2. IF value > $15,000 (HIGH-VALUE):
              - Invoke DispositionProposalAgent → HumanApprovalAgent (workflow pauses)
              - APPROVED: Use AI recommendation → KEEP→"KEEP_CAR", DISPOSE→"DISPOSE_CAR"
              - REJECTED: Opposite of AI → KEEP→"DISPOSE_CAR", DISPOSE→"KEEP_CAR"
           3. IF value ≤ $15,000 (LOW-VALUE):
              - Invoke DispositionAgent directly
              - KEEP→"KEEP_CAR", SCRAP/SELL/DONATE→"DISPOSE_CAR"
           4. IF "KEEP_CAR": Invoke MaintenanceAgent/CleaningAgent as needed
        
        CRITICAL: Always end with KEEP_CAR or DISPOSE_CAR marker.
        """)
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
        
        if (!dispositionRequired) {
            return String.format("""
                Car: %d %s %s (#%d)
                Disposition: NOT REQUIRED
                Cleaning: %s | Maintenance: %s
                → Include "APPROVAL_NOT_REQUIRED" in response
                """,
                carYear, carMake, carModel, carNumber,
                cleaningRequest, maintenanceRequest
            );
        }
        
        return String.format("""
            Car: %d %s %s (#%d) | Damage: %s
            Disposition: REQUIRED | Cleaning: %s | Maintenance: %s
            
            1. Get value from PricingAgent (keep $ format)
            2. Value > $15K: DispositionProposalAgent → HumanApprovalAgent
            3. Value ≤ $15K: DispositionAgent (pass carValue with $)
            
            CRITICAL: End with KEEP_CAR or DISPOSE_CAR
            """,
            carYear, carMake, carModel, carNumber, rentalFeedback,
            cleaningRequest, maintenanceRequest
        );
    }
}


