package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CleaningAgent;
import com.carmanagement.agentic.agents.MaintenanceAgent;
import dev.langchain4j.agentic.declarative.ActivationCondition;
import dev.langchain4j.agentic.declarative.ConditionalAgent;

/**
 * Workflow for processing car actions conditionally.
 */
public interface ActionWorkflow {

    /**
     * Runs the appropriate action agent based on the feedback analysis.
     */
    @ConditionalAgent(outputKey = "actionResult",
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
    static boolean activateMaintenance(String maintenanceRequest) {
        return isRequired(maintenanceRequest);
    }

    @ActivationCondition(CleaningAgent.class)
    static boolean activateCleaning(String cleaningRequest) {
        return isRequired(cleaningRequest);
    }

    private static boolean isRequired(String value) {
        return value != null && !value.isEmpty() && !value.toUpperCase().contains("NOT_REQUIRED");
    }
}


