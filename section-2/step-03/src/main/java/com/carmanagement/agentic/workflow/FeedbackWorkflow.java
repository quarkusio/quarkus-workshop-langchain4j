package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CleaningFeedbackAgent;
import com.carmanagement.agentic.agents.MaintenanceFeedbackAgent;
import dev.langchain4j.agentic.declarative.ParallelAgent;

/**
 * Workflow for processing car feedback in parallel.
 */
public interface FeedbackWorkflow {

    /**
     * Runs multiple feedback agents in parallel to analyze different aspects of car feedback.
     */
    @ParallelAgent(outputKey = "feedbackResult",
            subAgents = { CleaningFeedbackAgent.class, MaintenanceFeedbackAgent.class })
    String analyzeFeedback(
            String carMake,
            String carModel,
            Integer carYear,
            Long carNumber,
            String carCondition,
            String rentalFeedback,
            String cleaningFeedback,
            String maintenanceFeedback);
}


