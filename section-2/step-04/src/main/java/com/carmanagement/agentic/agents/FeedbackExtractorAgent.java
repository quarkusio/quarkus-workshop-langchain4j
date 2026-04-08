package com.carmanagement.agentic.agents;

import com.carmanagement.model.FeedbackTask;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.AgenticScope;
import io.quarkus.logging.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Non-AI agent that extracts individual feedback results from the parallel analysis
 * and returns them as a Map.
 */
public class FeedbackExtractorAgent {

    @Agent(description = "Extracts individual feedback requests from the parallel analysis results", outputKey = "feedbackRequests")
    public static Map<String, String> extractFeedback(List<String> feedbackResult, List<FeedbackTask> tasks, AgenticScope scope) {
        Log.info("FeedbackExtractorAgent extracting feedback from results...");
        Map<String, String> feedbackRequests = new HashMap<>();
        feedbackRequests.put("cleaningRequest", feedbackResult.get(0));
        feedbackRequests.put("maintenanceRequest", feedbackResult.get(1));
        feedbackRequests.put("dispositionRequest", feedbackResult.get(2));
        Log.info("FeedbackExtractorAgent extracted: " + feedbackRequests);
        // Write directly to scope as workaround for framework bug where
        // non-interface agent return values are discarded by AgenticScopeAction wrapper
        scope.writeState("feedbackRequests", feedbackRequests);
        return feedbackRequests;
    }
}