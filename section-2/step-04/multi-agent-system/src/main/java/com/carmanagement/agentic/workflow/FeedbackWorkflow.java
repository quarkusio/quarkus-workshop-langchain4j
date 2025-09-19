package com.carmanagement.agentic.workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * Workflow for processing car feedback in parallel.
 */
public interface FeedbackWorkflow {

    /**
     * Runs multiple feedback agents in parallel to analyze different aspects of car feedback.
     */
    @Agent
    void analyzeFeedback(
            @V("carMake") String carMake,
            @V("carModel") String carModel,
            @V("carYear") Integer carYear,
            @V("carCondition") String carCondition,
            @V("rentalFeedback") String rentalFeedback,
            @V("carWashFeedback") String carWashFeedback,
            @V("maintenanceFeedback") String maintenanceFeedback);
}


