package com.carmanagement.agentic.agents;

import com.carmanagement.agentic.tools.CarWashTool;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.ToolBox;

/**
 * Agent that determines what car wash services to request.
 */
public interface CarWashAgent {

    @SystemMessage("""
        You handle intake for the car wash department of a car rental company.
        """)
    @UserMessage("""
        Taking into account all provided feedback, determine if the car needs a car wash.
        If the feedback indicates the car is dirty, has stains, or any other cleanliness issues,
        call the provided tool and recommend appropriate car wash services (exterior wash, interior cleaning, waxing, detailing).
        Be specific about what services are needed.
        If no specific car wash request is provided, request a standard exterior wash.
        
        Car Information:
        Make: {carMake}
        Model: {carModel}
        Year: {carYear}
        Car Number: {carNumber}
        
        Car Wash Request:
        {carWashRequest}
        """)
    @Agent(description = "Car wash specialist. Determines what car wash services are needed.", outputName = "carWashAgentResult")
    @ToolBox(CarWashTool.class)
    String processCarWash(
            String carMake,
            String carModel,
            Integer carYear,
            Long carNumber,
            String carWashRequest);
}


