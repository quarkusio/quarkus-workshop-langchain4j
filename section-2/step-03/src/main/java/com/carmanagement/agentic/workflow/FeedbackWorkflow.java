package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CarWashFeedbackAgent;
import com.carmanagement.agentic.agents.MaintenanceFeedbackAgent;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.ParallelAgent;
import dev.langchain4j.agentic.declarative.SubAgent;
import dev.langchain4j.service.V;

/**
 * Workflow for processing car feedback in parallel.
 */
public interface FeedbackWorkflow {

    /**
     * Runs multiple feedback agents in parallel to analyze different aspects of car feedback.
     */
    @Agent(outputName = "feedbackResult")
    @ParallelAgent(outputName = "feedbackResult", subAgents = {
            @SubAgent(type = CarWashFeedbackAgent.class, outputName = "carWashRequest"),
            @SubAgent(type = MaintenanceFeedbackAgent.class, outputName = "maintenanceRequest")
    })
    String analyzeFeedback(
            @V("carMake") String carMake,
            @V("carModel") String carModel,
            @V("carYear") Integer carYear,
            @V("carCondition") String carCondition,
            @V("rentalFeedback") String rentalFeedback,
            @V("carWashFeedback") String carWashFeedback,
            @V("maintenanceFeedback") String maintenanceFeedback);
}


