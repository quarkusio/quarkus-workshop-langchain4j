package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CleaningAgent;
import com.carmanagement.agentic.agents.MaintenanceAgent;
import com.carmanagement.model.CarAssignment;
import com.carmanagement.model.CarInfo;
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
            CarInfo carInfo,
            Integer carNumber,
            String cleaningRequest,
            String maintenanceRequest);

    @ActivationCondition(MaintenanceAgent.class)
    static boolean assignToMaintenance(CarAssignment carAssignment) {
        return carAssignment == CarAssignment.MAINTENANCE;
    }

    @ActivationCondition(CleaningAgent.class)
    static boolean assignToCleaning(CarAssignment carAssignment) {
        return carAssignment == CarAssignment.CLEANING;
    }
    // --8<-- [end:conditional-agent]
}


