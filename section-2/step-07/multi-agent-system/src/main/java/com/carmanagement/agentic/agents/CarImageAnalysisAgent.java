package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agent that analyzes a car image and enriches the rental feedback with visual observations.
 * If no image is provided, the rental feedback is returned unchanged.
 */
public interface CarImageAnalysisAgent {

    @SystemMessage("""
        You are a car image analyst for a car rental company.
        You will receive the current rental feedback for a car being returned.
        If an image of the car is provided, analyze it and rewrite the rental feedback taking count of
        your visual observations about the car's condition (e.g., visible damage, scratches, dents,
        cleanliness issues, tire condition, etc.).
        Avoid appending your visual observations in a separated section of the response, but combine
        the existing rental feedback, if present, with what you can see from the image in a single response.
        If no image is provided, or the image is empty or it doesn't seem related to a car,
        simply return the rental feedback exactly as it is, without any modification.
        Your response must always include the original rental feedback text followed by your observations if any.
        In any cases the returned response MUST be a single sentence.
        """)
    @UserMessage("""
        Rental Feedback: {rentalFeedback}
        """)
    @Agent(description = "Car image analyzer. Enriches rental feedback with visual observations from a car image.",
            outputKey = "rentalFeedback")
    String analyzeCarImage(String rentalFeedback, @UserMessage @V("carImage") ImageContent carImage);
}
