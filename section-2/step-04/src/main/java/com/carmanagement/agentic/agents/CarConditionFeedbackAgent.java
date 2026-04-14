package com.carmanagement.agentic.agents;

import com.carmanagement.model.CarConditions;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.FeedbackAnalysisResults;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that analyzes feedback to determine the final car condition and assignment.
 * This is the final decision-maker that interprets all previous agent outputs.
 */
public interface CarConditionFeedbackAgent {

    @SystemMessage("""
        Analyze car processing results and output a JSON summary.
        
        Output format:
        {
          "generalCondition": "concise description (max 200 chars)",
          "carAssignment": "DISPOSITION|MAINTENANCE|CLEANING|NONE"
        }
        
        Rules:
        - carAssignment: Check the ACTUAL DispositionAgent decision in supervisorDecision, not just the analysis
        - If supervisorDecision mentions SCRAP/SELL/DONATE (but NOT KEEP) → DISPOSITION
        - Else if maintenanceAnalysis ≠ "MAINTENANCE_NOT_REQUIRED" → MAINTENANCE
        - Else if cleaningAnalysis ≠ "CLEANING_NOT_REQUIRED" → CLEANING
        - Else → NONE
        - IMPORTANT: If DispositionAgent decided KEEP, do NOT assign DISPOSITION - check maintenance/cleaning instead
        - generalCondition: Summarize the action and reason
        """)
    @UserMessage("""
            Car: {carInfo.year} {carInfo.make} {carInfo.model} (#{carNumber})
            
            Supervisor Decision: {supervisorDecision}
            
            Feedback Analysis Results:
            - Disposition: {feedbackAnalysisResults.dispositionAnalysis}
            - Maintenance: {feedbackAnalysisResults.maintenanceAnalysis}
            - Cleaning: {feedbackAnalysisResults.cleaningAnalysis}
            """)
    @Agent(description = "Final car condition analyzer. Determines the car's condition and assignment based on all feedback.",
            outputKey = "carConditions")
    CarConditions analyzeForCondition(
            CarInfo carInfo,
            Integer carNumber,
            FeedbackAnalysisResults feedbackAnalysisResults,
            String supervisorDecision);
}
