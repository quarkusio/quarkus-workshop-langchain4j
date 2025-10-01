package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that analyzes feedback to determine if maintenance is needed.
 */
public interface MaintenanceFeedbackAgent {

    @SystemMessage("""
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
        Make: {carMake}
        Model: {carModel}
        Year: {carYear}
        Previous Condition: {carCondition}
        
        Feedback:
        Rental Feedback: {rentalFeedback}
        Car Wash Feedback: {carWashFeedback}
        Maintenance Feedback: {maintenanceFeedback}
        """)
    @Agent(description = "Car maintenance analyzer. Using feedback, determines if a car needs maintenance.", outputName = "maintenanceRequest")
    String analyzeForMaintenance(
            String carMake,
            String carModel,
            Integer carYear,
            Long carNumber,
            String carCondition,
            String rentalFeedback,
            String carWashFeedback,
            String maintenanceFeedback);
}


