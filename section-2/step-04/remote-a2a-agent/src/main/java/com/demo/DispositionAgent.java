package com.demo;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Agent that determines how to dispose of a car.
 */
@RegisterAiService
@ApplicationScoped
public interface DispositionAgent {

    @SystemMessage("""
        /nothink, Reasoning: low.
        You handle intake for the car disposition department.
        It is your job to submit a request to the provided DispositionTool function to take action on the request (SCRAP, SELL, or DONATE).
        Be specific about what disposition option is most appropriate based on the car's condition.
        """)
    @UserMessage("""
        Car Information:
        Make: {{carMake}}
        Model: {{carModel}}
        Year: {{carYear}}
        Car Number: {{carNumber}}
        
        Previous Car Condition:
        {{carCondition}}
        
        Disposition Request:
        {{dispositionRequest}}
        """)
    @ToolBox(DispositionTool.class)
    String processDisposition(
            @V("carMake") String carMake,
            @V("carModel") String carModel,
            @V("carYear") Integer carYear,
            @V("carNumber") Integer carNumber,
            @V("carCondition") String carCondition,
            @V("dispositionRequest") String dispositionRequest);
}


