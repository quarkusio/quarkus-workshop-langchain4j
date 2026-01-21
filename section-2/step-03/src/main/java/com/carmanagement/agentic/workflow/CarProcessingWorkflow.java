package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CarConditionFeedbackAgent;
import com.carmanagement.model.CarConditions;
import com.carmanagement.model.CarAssignment;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;

/**
 * Workflow for processing car returns using a sequence of agents.
 */
public interface CarProcessingWorkflow {

    /**
     * Processes a car return by running feedback analysis and then appropriate actions.
     */
    // --8<-- [start:sequence-agent]
    @SequenceAgent(outputKey = "carProcessingAgentResult",
            subAgents = { FeedbackWorkflow.class, CarAssignmentWorkflow.class, CarConditionFeedbackAgent.class })
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
    static CarConditions output(String carCondition, String maintenanceRequest, String cleaningRequest) {
        CarAssignment carAssignment;
        // Check maintenance first (higher priority)
        if (isRequired(maintenanceRequest)) {
            carAssignment = CarAssignment.MAINTENANCE;
        } else if (isRequired(cleaningRequest)) {
            carAssignment = CarAssignment.CLEANING;
        } else {
            carAssignment = CarAssignment.NONE;
        }
        return new CarConditions(carCondition, carAssignment);
    }

    private static boolean isRequired(String value) {
        return value != null && !value.isEmpty() && !value.toUpperCase().contains("NOT_REQUIRED");
    }
}


