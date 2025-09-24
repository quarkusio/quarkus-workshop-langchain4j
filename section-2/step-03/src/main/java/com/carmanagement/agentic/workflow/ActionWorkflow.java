package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CarWashAgent;
import com.carmanagement.agentic.agents.MaintenanceAgent;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.ActivationCondition;
import dev.langchain4j.agentic.declarative.ConditionalAgent;
import dev.langchain4j.agentic.declarative.SubAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.service.V;

/**
 * Workflow for processing car actions conditionally.
 */
public interface ActionWorkflow {

    /**
     * Runs the appropriate action agent based on the feedback analysis.
     */
    @ConditionalAgent(outputName = "actionResult", subAgents = {
            @SubAgent(type = MaintenanceAgent.class, outputName = "actionResult"),
            @SubAgent(type = CarWashAgent.class, outputName = "actionResult")
    })
    String processAction(
            @V("carMake") String carMake,
            @V("carModel") String carModel,
            @V("carYear") Integer carYear,
            @V("carNumber") Integer carNumber,
            @V("carCondition") String carCondition,
            @V("carWashRequest") String carWashRequest,
            @V("maintenanceRequest") String maintenanceRequest);

    @ActivationCondition(MaintenanceAgent.class)
    static boolean activateMaintenance(AgenticScope agenticScope) {
        return isRequired(agenticScope, "maintenanceRequest");
    }

    @ActivationCondition(CarWashAgent.class)
    static boolean activateCarWash(AgenticScope agenticScope) {
        return isRequired(agenticScope, "carWashRequest");
    }

    private static boolean isRequired(AgenticScope agenticScope, String key) {
        String value = (String)agenticScope.readState(key);
        return value != null && !value.isEmpty() && !value.toUpperCase().contains("NOT_REQUIRED");
    }

}


