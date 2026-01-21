package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CleaningAgent;
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
    // --8<-- [start:conditional-agent]
    @ConditionalAgent(outputKey = "analysisResult",
            subAgents = { MaintenanceAgent.class,  CleaningAgent.class })
    String processAction(
            String carMake,
            String carModel,
            Integer carYear,
            Long carNumber,
            String carCondition,
            String cleaningRequest,
            String maintenanceRequest);

    @ActivationCondition(MaintenanceAgent.class)
    static boolean assignToMaintenance(String maintenanceRequest) {
        return isRequired(maintenanceRequest);
    }

    @ActivationCondition(CleaningAgent.class)
    static boolean assignToCleaning(String cleaningRequest) {
        return isRequired(cleaningRequest);
    }

    private static boolean isRequired(String value) {
        return value != null && !value.isEmpty() && !value.toUpperCase().contains("NOT_REQUIRED");
    }
    // --8<-- [end:conditional-agent]
}


