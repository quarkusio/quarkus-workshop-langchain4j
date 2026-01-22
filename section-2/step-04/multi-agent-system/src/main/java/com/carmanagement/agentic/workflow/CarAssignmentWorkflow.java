package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CleaningAgent;
import com.carmanagement.agentic.agents.DispositionAgent;
import com.carmanagement.agentic.agents.MaintenanceAgent;
import dev.langchain4j.agentic.declarative.ActivationCondition;
import dev.langchain4j.agentic.declarative.ConditionalAgent;

/**
 * Workflow for assigning cars to appropriate teams based on feedback analysis.
 */
public interface CarAssignmentWorkflow {

    /**
     * Assigns the car to the appropriate team based on the feedback analysis.
     */
    @ConditionalAgent(outputKey = "analysisResult",
            subAgents = { DispositionAgent.class, MaintenanceAgent.class, CleaningAgent.class })
    String processAction(
            String carMake,
            String carModel,
            Integer carYear,
            Long carNumber,
            String carCondition,
            String cleaningRequest,
            String maintenanceRequest,
            String dispositionRequest);

    @ActivationCondition(MaintenanceAgent.class)
    static boolean assignToMaintenance(String maintenanceRequest) {
        return isRequired(maintenanceRequest);
    }

    @ActivationCondition(CleaningAgent.class)
    static boolean assignToCleaning(String cleaningRequest) {
        return isRequired(cleaningRequest);
    }

    @ActivationCondition(DispositionAgent.class)
    static boolean assignToDisposition(String dispositionRequest) {
        return isRequired(dispositionRequest);
    }

    private static boolean isRequired(String value) {
        return value != null && !value.isEmpty() && !value.toUpperCase().contains("NOT_REQUIRED");
    }
}


