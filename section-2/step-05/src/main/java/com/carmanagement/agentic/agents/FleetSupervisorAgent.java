package com.carmanagement.agentic.agents;

import com.carmanagement.model.CarInfo;
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
        CarInfo carInfo,
        Integer carNumber,
        String feedback,
        FeedbackAnalysisResults feedbackAnalysisResults
    );

    @SupervisorRequest()
    static String request(
        CarInfo carInfo,
        Integer carNumber,
        String feedback,
        FeedbackAnalysisResults feedbackAnalysisResults
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
              - Invoke DispositionProposalAgent once
              - Read its Final Workflow Action marker: __KEEP_CAR__ or __DISPOSE_CAR__
              - Invoke HumanApprovalAgent once with the proposal (workflow pauses)
              - APPROVED: Return the proposal's Final Workflow Action
              - REJECTED: Return the opposite action
              - Do not invoke DispositionAgent for high-value cars
           3. IF value ≤ $15,000 (LOW-VALUE):
              - Invoke DispositionAgent once
              - Read its Final Workflow Action marker: __KEEP_CAR__ or __DISPOSE_CAR__
              - Return that Final Workflow Action
              - Do not invoke DispositionAgent more than once for the same car
           4. IF "KEEP_CAR": Invoke MaintenanceAgent/CleaningAgent as needed
           5. IF "DISPOSE_CAR": Finish immediately

           CRITICAL: End with KEEP_CAR or DISPOSE_CAR
           """;

        return """
            You are a fleet supervisor for a car rental company. You coordinate action agents based on feedback analysis.

            The feedback has already been analyzed and you have these inputs:
            - cleaningAnalysis: What cleaning is needed (or "CLEANING_NOT_REQUIRED")
            - maintenanceAnalysis: What maintenance is needed (or "MAINTENANCE_NOT_REQUIRED")
            - dispositionAnalysis: Whether severe damage requires disposition (or "DISPOSITION_NOT_REQUIRED")

            Your job is to invoke the appropriate ACTION agents for this car

            Car: """ + carInfo.year + " " + carInfo.make + " " + carInfo.model + " (#" + carNumber + ")" + """

            Current Condition: """ + carInfo.condition + """

            Feedback: """ + feedback + """

            Cleaning Analysis: """ + feedbackAnalysisResults.cleaningAnalysis() + """

            Maintenance Analysis: """ + feedbackAnalysisResults.maintenanceAnalysis() + """

            Disposition Analysis: """ + (dispositionRequired ? dispositionMessage : noDispositionMessage);
    }
}
