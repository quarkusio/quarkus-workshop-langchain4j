package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CarConditionFeedbackAgent;
import com.carmanagement.agentic.agents.FleetSupervisorAgent;
import com.carmanagement.model.CarConditions;
import com.carmanagement.model.CarAssignment;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;

/**
 * Workflow for processing car returns using a supervisor agent for complete orchestration.
 * The supervisor coordinates both feedback analysis and action agents.
 */
public interface CarProcessingWorkflow {

    /**
     * Processes a car return by first analyzing feedback, then using supervisor to coordinate actions.
     * FeedbackWorkflow produces cleaningRequest, maintenanceRequest, and dispositionRequest.
     * FleetSupervisorAgent then uses these to coordinate action agents.
     */
    // --8<-- [start:sequence-agent]
    @SequenceAgent(outputKey = "carProcessingAgentResult",
            subAgents = { FeedbackWorkflow.class, FleetSupervisorAgent.class, CarConditionFeedbackAgent.class })
    // --8<-- [end:sequence-agent]
    CarConditions processCarReturn(
            String carMake,
            String carModel,
            Integer carYear,
            Long carNumber,
            String carCondition,
            String rentalFeedback,
            String cleaningFeedback,
            String maintenanceFeedback);

    @Output
    static CarConditions output(String carCondition, String dispositionRequest, String maintenanceRequest,
                                String cleaningRequest, String supervisorDecision) {
        CarAssignment carAssignment;
        
        // Check disposition first (highest priority)
        // DispositionFeedbackAgent outputs "DISPOSITION_REQUIRED" if severe damage detected
        if (dispositionRequest != null && dispositionRequest.toUpperCase().contains("DISPOSITION_REQUIRED")) {
            carAssignment = CarAssignment.DISPOSITION;
        }
        // Also check supervisor's decision for disposition keywords
        else if (supervisorDecision != null &&
                 (supervisorDecision.toUpperCase().contains("SCRAP") ||
                  supervisorDecision.toUpperCase().contains("SELL") ||
                  supervisorDecision.toUpperCase().contains("DONATE"))) {
            carAssignment = CarAssignment.DISPOSITION;
        }
        // Check maintenance (high priority)
        else if (maintenanceRequest != null && !maintenanceRequest.toUpperCase().contains("MAINTENANCE_NOT_REQUIRED")) {
            carAssignment = CarAssignment.MAINTENANCE;
        }
        // Check cleaning (medium priority)
        else if (cleaningRequest != null && !cleaningRequest.toUpperCase().contains("CLEANING_NOT_REQUIRED")) {
            carAssignment = CarAssignment.CLEANING;
        }
        // No action needed
        else {
            carAssignment = CarAssignment.NONE;
        }
        return new CarConditions(carCondition, carAssignment);
    }
}


