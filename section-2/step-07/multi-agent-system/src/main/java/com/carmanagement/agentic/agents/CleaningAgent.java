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
        If no specific cleaning request is provided, request a standard exterior wash.
        
        Car Information:
        Make: {carMake}
        Model: {carModel}
        Year: {carYear}
        Car Number: {carNumber}
        
        Cleaning Request:
        {cleaningRequest}
        """)
    @Agent(description = "Cleaning specialist. Determines what cleaning services are needed.",
            outputKey = "analysisResult")
    @ToolBox(CleaningTool.class)
    String processCleaning(
            String carMake,
            String carModel,
            Integer carYear,
            Integer carNumber,
            String cleaningRequest);
}


