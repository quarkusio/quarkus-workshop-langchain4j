package com.carmanagement.agentic.agents;

import com.carmanagement.agentic.tools.CarWashTool;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.agentic.Agent;
import io.quarkiverse.langchain4j.ToolBox;

/**
 * Agent that determines what car wash services to request.
 */
public interface CarWashAgent {

    @SystemMessage("""
        You handle intake for the car wash department of a car rental company.
        It is your job to submit a request to the provided requestCarWash function to take action on the request.
        Be specific about what services are needed based on the car wash request.
        If no specific car wash request is provided, request a standard exterior wash.
        """)
    @UserMessage("""
        Car Information:
        Make: {carMake}
        Model: {carModel}
        Year: {carYear}
        Car Number: {carNumber}
        
        Car Wash Request:
        {carWashRequest}
        """)
    @Agent(description = "Car wash specialist. Determines what car wash services are needed.",
            outputName = "carWashAgentResult")
    @ToolBox(CarWashTool.class)
    String processCarWash(
            String carMake,
            String carModel,
            Integer carYear,
            Integer carNumber,
            String carWashRequest);
}


