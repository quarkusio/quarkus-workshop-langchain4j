package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CarConditionFeedbackAgent;
import com.carmanagement.agentic.agents.FleetSupervisorAgent;
import com.carmanagement.model.CarConditions;
import com.carmanagement.model.CarAssignment;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;

/**
 * Workflow for processing car returns using a supervisor agent for complete orchestration.
 * The supervisor coordinates both feedback analysis and action agents.
 */
public interface CarProcessingWorkflow {

    /**
     * Processes a car return by first analyzing feedback, then using supervisor to coordinate actions.
     * FeedbackWorkflow produces cleaningRequest, maintenanceRequest, and dispositionRequest.
     * FleetSupervisorAgent then uses these to coordinate action agents.
     */
    // --8<-- [start:sequence-agent]
    @SequenceAgent(outputKey = "carProcessingAgentResult",
            subAgents = { FeedbackWorkflow.class, FleetSupervisorAgent.class, CarConditionFeedbackAgent.class })
    // --8<-- [end:sequence-agent]
    CarConditions processCarReturn(
            String carMake,
            String carModel,
            Integer carYear,
            Long carNumber,
            String carCondition,
            String rentalFeedback,
            String cleaningFeedback,
            String maintenanceFeedback);

    @Output
    static CarConditions output(String carCondition, String dispositionRequest, String maintenanceRequest,
                                String cleaningRequest, String supervisorDecision) {

        // Extract approval status from supervisor decision
        String approvalStatus = extractApprovalStatus(supervisorDecision);
        
        // Check if disposition was required
        boolean dispositionRequired = dispositionRequest != null &&
                                     dispositionRequest.toUpperCase().contains("DISPOSITION_REQUIRED");
        
        // Determine car assignment based on priority
        CarAssignment carAssignment = determineCarAssignment(
            approvalStatus, dispositionRequired, supervisorDecision, maintenanceRequest, cleaningRequest
        );
        
        return new CarConditions(carCondition, carAssignment, approvalStatus, null);
    }
    
    /**
     * Extracts approval status from supervisor decision text.
     */
    private static String extractApprovalStatus(String supervisorDecision) {
        if (supervisorDecision == null) {
            return "NOT_REQUIRED";
        }
        
        String upperDecision = supervisorDecision.toUpperCase();
        
        // Check for specific keywords first
        if (upperDecision.contains("REJECTED_BY_USER")) {
            return "REJECTED";
        }
        if (upperDecision.contains("APPROVED_BY_USER")) {
            return "APPROVED";
        }
        if (upperDecision.contains("APPROVAL_NOT_REQUIRED")) {
            return "NOT_REQUIRED";
        }
        
        // Fallback to generic phrases
        if (upperDecision.contains("REJECTED") || upperDecision.contains("REJECTION")) {
            return "REJECTED";
        }
        if (upperDecision.contains("APPROVED") || upperDecision.contains("APPROVAL")) {
            return "APPROVED";
        }
        
        return "NOT_REQUIRED";
    }
    
    /**
     * Determines car assignment based on approval status and requests.
     */
    private static CarAssignment determineCarAssignment(String approvalStatus, boolean dispositionRequired,
                                                        String supervisorDecision, String maintenanceRequest,
                                                        String cleaningRequest) {
        // Priority 1: Human rejected disposition → send to maintenance
        if ("REJECTED".equals(approvalStatus) && dispositionRequired) {
            return CarAssignment.MAINTENANCE;
        }
        
        // Priority 2: Human approved disposition
        if ("APPROVED".equals(approvalStatus) && dispositionRequired) {
            // Check if the approved action was KEEP
            if (supervisorDecision != null && supervisorDecision.toUpperCase().contains("KEEP")) {
                // KEEP means car stays in fleet - check if maintenance/cleaning needed
                if (maintenanceRequest != null && !maintenanceRequest.toUpperCase().contains("MAINTENANCE_NOT_REQUIRED")) {
                    return CarAssignment.MAINTENANCE;
                }
                if (cleaningRequest != null && !cleaningRequest.toUpperCase().contains("CLEANING_NOT_REQUIRED")) {
                    return CarAssignment.CLEANING;
                }
                return CarAssignment.NONE; // Ready to rent
            }
            // Other dispositions (SCRAP/SELL/DONATE)
            return CarAssignment.DISPOSITION;
        }
        
        // Priority 3: Disposition required but no human approval (low-value car)
        if (dispositionRequired) {
            // Check if DispositionAgent decided KEEP
            if (supervisorDecision != null && supervisorDecision.toUpperCase().contains("KEEP")) {
                // KEEP means car stays in fleet - check if maintenance/cleaning needed
                if (maintenanceRequest != null && !maintenanceRequest.toUpperCase().contains("MAINTENANCE_NOT_REQUIRED")) {
                    return CarAssignment.MAINTENANCE;
                }
                if (cleaningRequest != null && !cleaningRequest.toUpperCase().contains("CLEANING_NOT_REQUIRED")) {
                    return CarAssignment.CLEANING;
                }
                return CarAssignment.NONE; // Ready to rent
            }
            // Other dispositions (SCRAP/SELL/DONATE)
            return CarAssignment.DISPOSITION;
        }
        
        // Priority 4: Maintenance needed
        if (maintenanceRequest != null && !maintenanceRequest.toUpperCase().contains("MAINTENANCE_NOT_REQUIRED")) {
            return CarAssignment.MAINTENANCE;
        }
        
        // Priority 5: Cleaning needed
        if (cleaningRequest != null && !cleaningRequest.toUpperCase().contains("CLEANING_NOT_REQUIRED")) {
            return CarAssignment.CLEANING;
        }
        
        // Priority 6: No action needed
        return CarAssignment.NONE;
    }
}


