package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CarAssignmentDecisionAgent;
import com.carmanagement.agentic.agents.CarConditionFeedbackAgent;
import com.carmanagement.model.CarConditions;
import com.carmanagement.model.CarAssignment;
import com.carmanagement.model.CarInfo;
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
            subAgents = { FeedbackWorkflow.class, CarAssignmentDecisionAgent.class, CarAssignmentWorkflow.class, CarConditionFeedbackAgent.class })
    // --8<-- [end:sequence-agent]
    CarConditions processCarReturn(
            CarInfo carInfo,
            Integer carNumber,
            String feedback);

    @Output
    static CarConditions output(String carCondition, CarAssignment carAssignment) {
        return new CarConditions(carCondition, carAssignment);
    }
}


