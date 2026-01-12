package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CarWashAgent;
import com.carmanagement.agentic.agents.DispositionAgent;
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
            subAgents = { MaintenanceAgent.class, CarWashAgent.class, DispositionAgent.class })
    String processAction(
            String carMake,
            String carModel,
            Integer carYear,
            Long carNumber,
            String carCondition,
            String carWashRequest,
            String maintenanceRequest,
            String dispositionRequest);

    @ActivationCondition(MaintenanceAgent.class)
    static boolean activateMaintenance(String maintenanceRequest) {
        return isRequired(maintenanceRequest);
    }

    @ActivationCondition(CarWashAgent.class)
    static boolean activateCarWash(String carWashRequest) {
        return isRequired(carWashRequest);
    }

    @ActivationCondition(DispositionAgent.class)
    static boolean activateDisposition(String dispositionRequest) {
        return isRequired(dispositionRequest);
    }

    private static boolean isRequired(String value) {
        return value != null && !value.isEmpty() && !value.toUpperCase().contains("NOT_REQUIRED");
    }
}


