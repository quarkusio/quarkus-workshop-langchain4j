package com.carmanagement.agentic.agents;

import com.carmanagement.model.FeedbackAnalysisResults;
import dev.langchain4j.agentic.declarative.SupervisorAgent;
import dev.langchain4j.agentic.declarative.SupervisorRequest;

/**
 * Supervisor agent that orchestrates the entire car processing workflow.
 * Coordinates action agents based on analysis results of the car condition.
 */
public interface FleetSupervisorAgent {

    /**
     * Main method to coordinate car processing based on feedback.
     * This is the entry point for the supervisor agent.
     */
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
            Integer carNumber,
            String carCondition,
            String rentalFeedback,
            String cleaningFeedback,
            String maintenanceFeedback,
            FeedbackAnalysisResults feedbackAnalysisResults
    );

    /**
     * Generates the supervisor request prompt based on feedback analysis results.
     * This method examines the disposition analysis to determine if the car requires
     * disposition (removal from fleet) and constructs appropriate instructions for
     * the supervisor agent to coordinate the necessary action agents.
     *
     * @param carMake The make of the car
     * @param carModel The model of the car
     * @param carYear The year of the car
     * @param carNumber The car's identification number
     * @param carCondition The current condition description
     * @param feedbackAnalysisResults The results from parallel feedback analysis
     * @param rentalFeedback The original rental feedback
     * @return A formatted prompt instructing the supervisor which agents to invoke
     */
    @SupervisorRequest
    static String request(
            String carMake,
            String carModel,
            Integer carYear,
            Integer carNumber,
            String carCondition,
            FeedbackAnalysisResults feedbackAnalysisResults,
            String rentalFeedback
    ) {
        boolean dispositionRequired = feedbackAnalysisResults.dispositionAnalysis() != null &&
                feedbackAnalysisResults.dispositionAnalysis().toUpperCase().contains("DISPOSITION_REQUIRED");

        String noDispositionMessage = """
               No disposition has been requested.
               
                INSTRUCTIONS:
                - DO NOT invoke PricingAgent
                - DO NOT invoke DispositionAgent
                - Only invoke MaintenanceAgent if maintenance needed
                - Only invoke CleaningAgent if cleaning needed
               """;

        // Disposition required - complex path
        String dispositionMessage = """
            The car has to be disposed.

            STEP 1: Invoke PricingAgent to get car value
            STEP 2: Invoke DispositionAgent to decide disposition action (SCRAP/SELL/DONATE/KEEP)
            STEP 3: If DispositionAgent decides KEEP:
                    - Invoke MaintenanceAgent if maintenance needed
                    - Invoke CleaningAgent if cleaning needed
            
            IMPORTANT: When invoking DispositionAgent:
            - Pass carValue as a STRING with dollar sign (e.g., "$10,710" not 10710)
            - Use the EXACT format from PricingAgent's response
            
            Follow the decision logic in your system message carefully.
            """;

        return String.format("""
            You are a fleet supervisor for a car rental company. You coordinate action agents based on feedback analysis.
            
            The feedback has already been analyzed and you have these inputs:
            - cleaningAnalysis: What cleaning is needed (or "CLEANING_NOT_REQUIRED")
            - maintenanceAnalysis: What maintenance is needed (or "MAINTENANCE_NOT_REQUIRED")
            - dispositionAnalysis: Whether severe damage requires disposition (or "DISPOSITION_NOT_REQUIRED")
            
            Your job is to invoke the appropriate ACTION agents for this car
            
            Car: %d %s %s (#%d)
            Current Condition: %s
            Rental Feedback: %s
            
            Cleaning Analysis: %s
            Maintenance Analysis: %s
            
            In particular, your have to follow these steps
            
            %s
            """,
                carYear, carMake, carModel, carNumber, carCondition, rentalFeedback,
                feedbackAnalysisResults.cleaningAnalysis(),
                feedbackAnalysisResults.maintenanceAnalysis(),
                dispositionRequired ? dispositionMessage : noDispositionMessage);
    }
}
