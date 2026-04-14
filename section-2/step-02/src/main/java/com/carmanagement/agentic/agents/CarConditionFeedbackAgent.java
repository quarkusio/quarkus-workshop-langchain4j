package com.carmanagement.agentic.agents;

import com.carmanagement.model.CarInfo;
import com.carmanagement.model.FeedbackContext;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that analyzes feedback to update the car condition.
 */
public interface CarConditionFeedbackAgent {

    @SystemMessage("""
        You are a car condition analyzer for a car rental company. Your job is to determine the current condition of a car based on feedback.
        Analyze all feedback and the previous car condition to provide an updated condition description.
        Always provide a concise condition description, even if there's minimal feedback.
        Do not add any headers or prefixes to your response.
        """)
    @UserMessage("""
            Car Information:
            Make: {carInfo.make}
            Model: {carInfo.model}
            Year: {carInfo.year}
            Previous Condition: {carInfo.condition}
            
            Rental Feedback: {feedback.rentalFeedback}
            Cleaning Feedback: {feedback.cleaningFeedback}
            """)
    @Agent(outputKey = "carCondition",
            description = "Car condition analyzer. Determines the current condition of a car based on feedback.")
    String analyzeForCondition(
            CarInfo carInfo,
            Integer carNumber,
            FeedbackContext feedback);
}
