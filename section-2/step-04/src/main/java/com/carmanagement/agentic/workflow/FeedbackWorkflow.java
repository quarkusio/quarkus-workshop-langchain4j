package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.FeedbackAnalysisAgent;
import com.carmanagement.model.FeedbackTask;
import dev.langchain4j.agentic.declarative.ParallelMapperAgent;

import java.util.List;

/**
 * Workflow for processing car feedback in parallel.
 * Analyzes feedback for cleaning, maintenance, and disposition needs using a unified agent.
 */
public interface FeedbackWorkflow {

    /**
     * Runs the feedback analysis agent in parallel for multiple tasks.
     * Uses @ParallelMapperAgent to execute the same agent with different task configurations.
     * Returns a list of results that will be mapped to individual output keys.
     */
    // --8<-- [start:parallel-mapper-agent]
    @ParallelMapperAgent(
            description = "Analyzes car feedback in parallel for cleaning, maintenance, and disposition needs",
            outputKey = "feedbackResult",
            subAgent = FeedbackAnalysisAgent.class,
            itemsProvider = "tasks")
    // --8<-- [end:parallel-mapper-agent]
    List<String> analyzeFeedback(
            List<FeedbackTask> tasks,
            String carMake,
            String carModel,
            Integer carYear,
            Integer carNumber,
            String carCondition,
            String rentalFeedback,
            String cleaningFeedback,
            String maintenanceFeedback);
}
