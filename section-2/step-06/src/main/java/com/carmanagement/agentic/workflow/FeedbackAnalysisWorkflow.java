package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.FeedbackAnalysisAgent;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.FeedbackAnalysisResults;
import com.carmanagement.model.FeedbackContext;
import com.carmanagement.model.FeedbackTask;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.ParallelMapperAgent;
import dev.langchain4j.agentic.scope.AgenticScope;

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
            outputKey = "feedbackAnalysisResults",
            subAgent = FeedbackAnalysisAgent.class,
            itemsProvider = "tasks")
    // --8<-- [end:parallel-mapper-agent]
    FeedbackAnalysisResults analyzeFeedback(
            List<FeedbackTask> tasks,
            CarInfo carInfo,
            Integer carNumber,
            FeedbackContext feedback);

    /**
     * Output method that transforms the parallel feedback results into a structured object.
     * The feedbackAnalysisResults list contains results in the same order as the input tasks:
     * [0] = cleaning analysis, [1] = maintenance analysis, [2] = disposition analysis
     */
    @Output
    static FeedbackAnalysisResults output(AgenticScope scope, List<String> feedbackAnalysisResults) {
        return new FeedbackAnalysisResults(
                feedbackAnalysisResults.get(0),  // cleaningAnalysis
                feedbackAnalysisResults.get(1),  // maintenanceAnalysis
                feedbackAnalysisResults.get(2)   // dispositionAnalysis
        );
    }
}