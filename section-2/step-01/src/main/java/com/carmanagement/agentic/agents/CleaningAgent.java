package com.carmanagement.agentic.agents;

import io.quarkiverse.langchain4j.ToolBox;

import com.carmanagement.agentic.tools.CleaningTool;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.FeedbackContext;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

// --8<-- [start:cleaningAgent]
/**
 * Agent that determines what cleaning services to request.
 */
public interface CleaningAgent {

    @SystemMessage("""
        You handle intake for the cleaning department of a car rental company.
        It is your job to submit a request to the provided requestCleaning function to take action based on the provided feedback.
        Be specific about what services are needed.
        If no cleaning is needed based on the feedback, respond with "CLEANING_NOT_REQUIRED".
        """)
    @UserMessage("""
        Car Information:
        Make: {carInfo.make}
        Model: {carInfo.model}
        Year: {carInfo.year}
        Car Number: {carNumber}
        
        Feedback:
        Rental Feedback: {feedback.rentalFeedback}
        Cleaning Feedback: {feedback.cleaningFeedback}
        """)
    @Agent("Cleaning specialist. Determines what cleaning services are needed.")
    @ToolBox(CleaningTool.class)
    String processCleaning(
            CarInfo carInfo,
            Integer carNumber,
            FeedbackContext feedback);
}
// --8<-- [end:cleaningAgent]

