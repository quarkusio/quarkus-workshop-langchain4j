package com.carmanagement.agentic.agents;

import com.carmanagement.agentic.tools.CarWashTool;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.agentic.Agent;
import io.quarkiverse.langchain4j.ToolBox;

// --8<-- [start:carWashAgent]
/**
 * Agent that determines what car wash services to request.
 */
public interface CarWashAgent {

    @SystemMessage("""
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
    @ToolBox(CarWashTool.class)
    String processCarWash(
            @V("carMake") String carMake,
            @V("carModel") String carModel,
            @V("carYear") Integer carYear,
            @V("carNumber") Integer carNumber,
            @V("rentalFeedback") String rentalFeedback,
            @V("carWashFeedback") String carWashFeedback);
}
// --8<-- [end:carWashAgent]

