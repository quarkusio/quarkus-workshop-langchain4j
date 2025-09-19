package com.carmanagement.agentic.agents;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.agentic.Agent;

/**
 * Agent that analyzes feedback to determine if maintenance is needed.
 */
public interface MaintenanceFeedbackAgent {

    @SystemMessage("""
        /nothink, Reasoning: low.
        You are a car maintenance analyzer for a car rental company. Your job is to determine if a car needs maintenance based on feedback.
        Analyze the feedback and car information to decide if maintenance is needed.
        If the feedback mentions mechanical issues, strange noises, performance problems, or anything that suggests
        the car needs maintenance, recommend appropriate maintenance.
        Be specific about what type of maintenance is needed (oil change, tire rotation, brake service, engine service, transmission service).
        If no service of any kind, repairs or maintenance are needed, respond with "MAINTENANCE_NOT_REQUIRED".
        Include the reason for your choice but keep your response short.
        """)
    @UserMessage("""
        Car Information:
        Make: {{carMake}}
        Model: {{carModel}}
        Year: {{carYear}}
        Previous Condition: {{carCondition}}
        
        Feedback:
        Rental Feedback: {{rentalFeedback}}
        Car Wash Feedback: {{carWashFeedback}}
        Maintenance Feedback: {{maintenanceFeedback}}
        """)
    @Agent("Car maintenance analyzer. Using feedback, determines if a car needs maintenance.")
    String analyzeForMaintenance(
            @V("carMake") String carMake,
            @V("carModel") String carModel,
            @V("carYear") Integer carYear,
            @V("carNumber") Integer carNumber,
            @V("carCondition") String carCondition,
            @V("rentalFeedback") String rentalFeedback,
            @V("carWashFeedback") String carWashFeedback,
            @V("maintenanceFeedback") String maintenanceFeedback);
}


