package com.carmanagement.agentic.agents;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.agentic.Agent;

// --8<-- [start:carWashAgent]
/**
 * Agent that determines what car wash services to request.
 */
public interface CarWashAgent {

    @SystemMessage("""
        /nothink, Reasoning: low.
        You handle intake for the car wash department of a car rental company.
        It is your job to submit a request to the provided requestCarWash function to take action based on the provided feedback.
        Be specific about what services are needed.
        If no car wash is needed based on the feedback, respond with "CARWASH_NOT_REQUIRED".
        """)
    @UserMessage("""
        Car Information:
        Make: {{carMake}}
        Model: {{carModel}}
        Year: {{carYear}}
        Car Number: {{carNumber}}
        
        Feedback:
        Rental Feedback: {{rentalFeedback}}
        Car Wash Feedback: {{carWashFeedback}}
        """)
    @Agent("Car wash specialist. Determines what car wash services are needed.")
    String processCarWash(
            @V("carMake") String carMake,
            @V("carModel") String carModel,
            @V("carYear") Integer carYear,
            @V("carNumber") Integer carNumber,
            @V("rentalFeedback") String rentalFeedback,
            @V("carWashFeedback") String carWashFeedback);
}
// --8<-- [end:carWashAgent]

