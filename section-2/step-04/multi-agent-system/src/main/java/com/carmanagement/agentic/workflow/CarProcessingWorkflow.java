package com.carmanagement.agentic.workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.service.V;

/**
 * Workflow for processing car returns using a sequence of agents.
 */
public interface CarProcessingWorkflow {

    /**
     * Processes a car return by running feedback analysis and then appropriate actions.
     */
    @Agent(outputName="carProcessingAgentResult")
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


