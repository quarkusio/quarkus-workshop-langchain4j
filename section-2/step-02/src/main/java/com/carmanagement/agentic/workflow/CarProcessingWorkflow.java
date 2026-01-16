package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CarConditionFeedbackAgent;
import com.carmanagement.agentic.agents.CarWashAgent;
import com.carmanagement.model.CarConditions;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;

/**
 * Workflow for processing car returns using a sequence of agents.
 */
public interface CarProcessingWorkflow {

    /**
     * Processes a car return by running feedback analysis and then appropriate actions.
     */
    @SequenceAgent(
            outputKey = "carConditions",
            subAgents = { CarWashAgent.class, CarConditionFeedbackAgent.class })
    CarConditions processCarReturn(
            String carMake,
            String carModel,
            Integer carYear,
            Long carNumber,
            String carCondition,
            String rentalFeedback,
            String carWashFeedback);

    @Output
    static CarConditions output(String carCondition, String carWashAgentResult) {
        boolean carWashRequired = !carWashAgentResult.toUpperCase().contains("NOT_REQUIRED");
        return new CarConditions(carCondition, carWashRequired);
    }
}
