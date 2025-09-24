package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CarConditionFeedbackAgent;
import com.carmanagement.model.CarConditions;
import com.carmanagement.model.RequiredAction;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.declarative.SubAgent;

/**
 * Workflow for processing car returns using a sequence of agents.
 */
public interface CarProcessingWorkflow {

    /**
     * Processes a car return by running feedback analysis and then appropriate actions.
     */
    @SequenceAgent(outputName = "carProcessingAgentResult", subAgents = {
            @SubAgent(type = FeedbackWorkflow.class, outputName = "carProcessingAgentResult"),
            @SubAgent(type = ActionWorkflow.class, outputName = "carProcessingAgentResult"),
            @SubAgent(type = CarConditionFeedbackAgent.class, outputName = "carProcessingAgentResult")
    })
    CarConditions processCarReturn(
            String carMake,
            String carModel,
            Integer carYear,
            Integer carNumber,
            String carCondition,
            String rentalFeedback,
            String carWashFeedback,
            String maintenanceFeedback);

    @Output
    static CarConditions output(String carCondition, String maintenanceRequest, String carWashRequest) {
        RequiredAction requiredAction;
        // Check maintenance first (higher priority)
        if (isRequired(maintenanceRequest)) {
            requiredAction = RequiredAction.MAINTENANCE;
        } else if (isRequired(carWashRequest)) {
            requiredAction = RequiredAction.CAR_WASH;
        } else {
            requiredAction = RequiredAction.NONE;
        }
        return new CarConditions(carCondition, requiredAction);
    }

    private static boolean isRequired(String value) {
        return value != null && !value.isEmpty() && !value.toUpperCase().contains("NOT_REQUIRED");
    }
}


