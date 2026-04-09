package com.carmanagement.agentic.agents;

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
        Make: {carMake}
        Model: {carModel}
        Year: {carYear}
        Previous Condition: {carCondition}
        
        Feedback:
        Rental Feedback: {rentalFeedback}
        Cleaning Feedback: {cleaningFeedback}
        Maintenance Feedback: {maintenanceFeedback}
        """)
    @Agent(description = "Feedback analyzer. Using feedback, determines if action is needed based on task type.",
            outputKey = "{task.outputKey}")
    String analyzeFeedback(
            FeedbackTask task,
            String carMake,
            String carModel,
            Integer carYear,
            Integer carNumber,
            String carCondition,
            String rentalFeedback,
            String cleaningFeedback,
            String maintenanceFeedback);
}