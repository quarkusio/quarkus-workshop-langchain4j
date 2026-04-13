package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.FeedbackAnalysisAgent;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.FeedbackAnalysisResults;
import com.carmanagement.model.FeedbackTask;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.ParallelMapperAgent;

import java.util.List;

/**
 * Workflow for processing car feedback in parallel.
 * Analyzes feedback for cleaning, maintenance, and disposition needs using a unified agent.
 */
public interface FeedbackAnalysisWorkflow {

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
            CarInfo carInfo,
            Integer carNumber,
            String feedback);

    /**
     * Output method that transforms the parallel feedback results into a structured object.
     * The feedbackResult list contains results in the same order as the input tasks:
     * [0] = cleaning analysis, [1] = maintenance analysis, [2] = disposition analysis
     */
    @Output
    static FeedbackAnalysisResults output(List<String> feedbackResult) {
        return new FeedbackAnalysisResults(
                feedbackResult.get(0),  // cleaningAnalysis
                feedbackResult.get(1),  // maintenanceAnalysis
                feedbackResult.get(2)   // dispositionAnalysis
        );
    }
}