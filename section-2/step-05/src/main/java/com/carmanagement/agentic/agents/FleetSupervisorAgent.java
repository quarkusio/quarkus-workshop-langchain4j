package com.carmanagement.agentic.agents;

import com.carmanagement.model.FeedbackAnalysisResults;
import dev.langchain4j.agentic.declarative.SupervisorAgent;
import dev.langchain4j.agentic.declarative.SupervisorRequest;

/**
 * Supervisor agent that orchestrates the entire car processing workflow.
 * Coordinates feedback analysis agents and action agents based on car condition.
 * Implements human-in-the-loop pattern for high-value vehicle dispositions.
 */
public interface FleetSupervisorAgent {

    @SupervisorAgent(
        outputKey = "supervisorDecision",
        subAgents = {
            PricingAgent.class,
            DispositionProposalAgent.class,
            HumanApprovalAgent.class,
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

    @SupervisorRequest()
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
            Disposition is not required. 
            Proceed with normal maintenance and cleaning workflow. 
            If cleaning or maintenance is required, invoke the appropriate agents.
                """;

        String dispositionMessage = """
           DISPOSITION_REQUIRED
           
           Follow these steps:
           
           1. Get value from PricingAgent (keep $ format)
           2. IF value > $15,000 (HIGH-VALUE):
              - Invoke DispositionProposalAgent → HumanApprovalAgent (workflow pauses)
              - APPROVED: Use AI recommendation → KEEP→"KEEP_CAR", DISPOSE→"DISPOSE_CAR"
              - REJECTED: Opposite of AI → KEEP→"DISPOSE_CAR", DISPOSE→"KEEP_CAR"
           3. IF value ≤ $15,000 (LOW-VALUE):
              - Invoke DispositionAgent directly
              - KEEP→"KEEP_CAR", SCRAP/SELL/DONATE→"DISPOSE_CAR"
           4. IF "KEEP_CAR": Invoke MaintenanceAgent/CleaningAgent as needed
           
           CRITICAL: End with KEEP_CAR or DISPOSE_CAR
           """;

        return """
            You are a fleet supervisor for a car rental company. You coordinate action agents based on feedback analysis.
            
            The feedback has already been analyzed and you have these inputs:
            - cleaningAnalysis: What cleaning is needed (or "CLEANING_NOT_REQUIRED")
            - maintenanceAnalysis: What maintenance is needed (or "MAINTENANCE_NOT_REQUIRED")
            - dispositionAnalysis: Whether severe damage requires disposition (or "DISPOSITION_NOT_REQUIRED")
            
            Your job is to invoke the appropriate ACTION agents for this car
            
            Car: """ + carYear + " " + carMake + " " + carModel + " (#" + carNumber + ")" + """
            
            Current Condition: """ + carCondition + """
            
            Rental Feedback: """ + rentalFeedback + """
            
            Cleaning Analysis: """ + feedbackAnalysisResults.cleaningAnalysis() + """
            
            Maintenance Analysis: """ + feedbackAnalysisResults.maintenanceAnalysis() + """
            
            Disposition Analysis: """ + (dispositionRequired ? dispositionMessage : noDispositionMessage);
    }
}


