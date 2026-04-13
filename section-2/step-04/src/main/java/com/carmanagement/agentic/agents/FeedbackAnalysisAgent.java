package com.carmanagement.agentic.agents;

import com.carmanagement.model.CarInfo;
import com.carmanagement.model.FeedbackTask;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Unified agent that analyzes feedback based on the provided task configuration.
 * This agent is parameterized to handle cleaning, maintenance, and disposition analysis.
 */
public interface FeedbackAnalysisAgent {

    @SystemMessage("{task.systemInstructions}")
    @UserMessage("""
        Car Information:
        Make: {carInfo.make}
        Model: {carInfo.model}
        Year: {carInfo.year}
        Previous Condition: {carInfo.condition}
        
        Feedback: {feedback}
        """)
    @Agent(description = "Feedback analyzer. Using feedback, determines if action is needed based on task type.",
            outputKey = "feedbackAnalysis")
    String analyzeFeedback(
            FeedbackTask task,
            CarInfo carInfo,
            Integer carNumber,
            String feedback);
}

