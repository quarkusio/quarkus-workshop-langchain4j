package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.declarative.SupervisorAgent;
import dev.langchain4j.agentic.declarative.SupervisorRequest;
import dev.langchain4j.service.SystemMessage;

/**
 * Supervisor agent that orchestrates the entire car processing workflow.
 * Coordinates feedback analysis agents and action agents based on car condition.
 */
public interface FleetSupervisorAgent {

    @SystemMessage("""
        You are a fleet supervisor for a car rental company. You coordinate action agents based on feedback analysis.
        
        You will receive car information and feedback analysis results as parameters.
        Based on these inputs, you must invoke the appropriate ACTION agents.
        
        DECISION LOGIC:
        
        1. If dispositionRequest contains "DISPOSITION_REQUIRED":
           - ALWAYS invoke PricingAgent first with: carMake, carModel, carYear, carCondition
           - Then invoke DispositionAgent with: carMake, carModel, carYear, carNumber, carCondition, carValue (from PricingAgent), rentalFeedback
           - The DispositionAgent will decide: SCRAP/SELL/DONATE/KEEP
           - If DispositionAgent says KEEP, then invoke MaintenanceAgent or CleaningAgent as needed
        
        2. If dispositionRequest is "DISPOSITION_NOT_REQUIRED":
           - If maintenanceRequest is NOT "MAINTENANCE_NOT_REQUIRED": Invoke MaintenanceAgent
           - If cleaningRequest is NOT "CLEANING_NOT_REQUIRED": Invoke CleaningAgent
           - You can invoke both if both are needed
        
        IMPORTANT: You MUST invoke agents when the conditions are met. Do not skip agent invocations.
        
        Explain your decision-making clearly, including which agents you invoked and why.
        """)
    @SupervisorAgent(
        outputKey = "supervisorDecision",
        subAgents = {
            PricingAgent.class,
            DispositionAgent.class,
            MaintenanceAgent.class,
            CleaningAgent.class
        }
    )
    String superviseCarProcessing(
        String carMake,
        String carModel,
        Integer carYear,
        Long carNumber,
        String carCondition,
        String rentalFeedback,
        String cleaningFeedback,
        String maintenanceFeedback,
        String cleaningRequest,
        String maintenanceRequest,
        String dispositionRequest
    );

    @SupervisorRequest
    static String request(
        String carMake,
        String carModel,
        Integer carYear,
        Long carNumber,
        String carCondition,
        String cleaningRequest,
        String maintenanceRequest,
        String dispositionRequest,
        String rentalFeedback
    ) {
        return String.format("""
            Process this car based on the feedback analysis results:
            
            Car: %d %s %s (#%d)
            Current Condition: %s
            
            Feedback Analysis Results:
            - Cleaning Request: %s
            - Maintenance Request: %s
            - Disposition Request: %s
            
            Additional Context:
            - Rental Feedback: %s
            
            Based on the decision logic in your system message, invoke the appropriate action agents.
            """,
            carYear, carMake, carModel, carNumber, carCondition,
            cleaningRequest, maintenanceRequest, dispositionRequest,
            rentalFeedback
        );
    }
}

