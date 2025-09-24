package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CarConditionFeedbackAgent;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.declarative.SubAgent;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.service.V;

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
    ResultWithAgenticScope<String> processCarReturn(
            @V("carMake") String carMake,
            @V("carModel") String carModel,
            @V("carYear") Integer carYear,
            @V("carNumber") Integer carNumber,
            @V("carCondition") String carCondition,
            @V("rentalFeedback") String rentalFeedback,
            @V("carWashFeedback") String carWashFeedback,
            @V("maintenanceFeedback") String maintenanceFeedback);
}


