package com.carmanagement.agentic.agents;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.agentic.Agent;

/**
 * Agent that determines what car wash services to request.
 */
public interface CarWashAgent {

    @SystemMessage("""
        /nothink, Reasoning: low.
        You handle intake for the car wash department of a car rental company.
        It is your job to submit a request to the provided requestCarWash function to take action on the request.
        Be specific about what services are needed based on the car wash request.
        If no specific car wash request is provided, request a standard exterior wash.
        """)
    @UserMessage("""
        Car Information:
        Make: {{carMake}}
        Model: {{carModel}}
        Year: {{carYear}}
        Car Number: {{carNumber}}
        
        Car Wash Request:
        {{carWashRequest}}
        """)
    @Agent(outputName="carWashAgentResult", description="Car wash specialist. Determines what car wash services are needed.")
    String processCarWash(
            @V("carMake") String carMake,
            @V("carModel") String carModel,
            @V("carYear") Integer carYear,
            @V("carNumber") Integer carNumber,
            @V("carWashRequest") String carWashRequest);
}


