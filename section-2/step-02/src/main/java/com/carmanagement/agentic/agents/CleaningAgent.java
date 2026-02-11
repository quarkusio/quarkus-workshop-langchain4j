package com.carmanagement.agentic.agents;

import io.quarkiverse.langchain4j.ToolBox;

import com.carmanagement.agentic.tools.CleaningTool;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that determines what cleaning services to request.
 */
public interface CleaningAgent {

    @SystemMessage("""
        You handle intake for the cleaning department of a car rental company.
        """)
    @UserMessage("""
        Taking into account all provided feedback, determine if the car needs a cleaning.
        If the feedback indicates the car is dirty, has stains, or any other cleanliness issues,
        call the provided tool and recommend appropriate cleaning services (exterior wash, interior cleaning, waxing, detailing).
        Be specific about what services are needed.
        If no cleaning is needed based on the feedback, respond with "CLEANING_NOT_REQUIRED".
        
        Car Information:
        Make: {carMake}
        Model: {carModel}
        Year: {carYear}
        Car Number: {carNumber}
        
        Feedback:
        Rental Feedback: {rentalFeedback}
        Cleaning Feedback: {cleaningFeedback}
        """)
    @Agent(outputKey = "cleaningAgentResult",
            description = "Cleaning specialist. Determines what cleaning services are needed.")
    @ToolBox(CleaningTool.class)
    String processCleaning(
            String carMake,
            String carModel,
            Integer carYear,
            Integer carNumber,
            String rentalFeedback,
            String cleaningFeedback);
}


