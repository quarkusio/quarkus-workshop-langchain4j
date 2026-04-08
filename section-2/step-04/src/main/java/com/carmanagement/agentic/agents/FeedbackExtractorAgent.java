package com.carmanagement.agentic.agents;

import com.carmanagement.model.FeedbackTask;
import dev.langchain4j.agentic.Agent;
import io.quarkiverse.langchain4j.CreatedAware;
import io.quarkus.logging.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Non-AI agent that extracts individual feedback results from the parallel analysis
 * and returns them as a Map.
 */
@CreatedAware
public class FeedbackExtractorAgent {

    @Agent(description = "Extracts individual feedback requests from the parallel analysis results", outputKey = "feedbackRequests")
    public static Map<String, String> extractFeedback(List<String> feedbackResult, List<FeedbackTask> tasks) {
        Log.info("FeedbackExtractorAgent extracting feedback from results...");
        Map<String, String> requests = new HashMap<>();
        requests.put("cleaningRequest", feedbackResult.get(0));
        requests.put("maintenanceRequest", feedbackResult.get(1));
        requests.put("dispositionRequest", feedbackResult.get(2));
        Log.info("FeedbackExtractorAgent extracted: " + requests);
        return requests;
    }
}