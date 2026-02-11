package com.carmanagement.agentic.tools;

import com.carmanagement.model.ApprovalProposal;
import com.carmanagement.service.ApprovalService;
import dev.langchain4j.agent.tool.Tool;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Tool that enables TRUE Human-in-the-Loop by blocking workflow execution
 * until a human makes an approval decision through the UI.
 */
@ApplicationScoped
public class HumanApprovalTool {

    @Inject
    ApprovalService approvalService;

    @Tool("Request human approval for a high-value vehicle disposition proposal. This will PAUSE the workflow until a human approves or rejects the proposal via the UI.")
    public String requestHumanApproval(
            Integer carNumber,
            String carMake,
            String carModel,
            Integer carYear,
            String carValue,
            String proposedDisposition,
            String dispositionReason,
            String carCondition,
            String rentalFeedback) {
        
        Log.infof("🛑 HITL Tool: Creating approval proposal for car %d - %s %s %s", 
                carNumber, carYear, carMake, carModel);
        Log.info("⏸️  WORKFLOW PAUSED - Waiting for human approval decision via UI");
        
        try {
            // Create proposal and get CompletableFuture that completes when human decides
            CompletableFuture<ApprovalProposal> approvalFuture =
                approvalService.createProposalAndWaitForDecision(
                    carNumber, carMake, carModel, carYear, carValue,
                    proposedDisposition, dispositionReason, carCondition, rentalFeedback
                );
            
            // BLOCK HERE until human makes decision (with 5 minute timeout)
            ApprovalProposal result = approvalFuture.get(5, TimeUnit.MINUTES);
            
            Log.infof("▶️  WORKFLOW RESUMED - Human decision received: %s", result.decision);
            
            // Format response for the agent
            return String.format("""
                Human Decision: %s
                Reason: %s
                Approved By: %s
                Decision Time: %s
                """,
                result.decision,
                result.approvalReason != null ? result.approvalReason : "No reason provided",
                result.approvedBy != null ? result.approvedBy : "Unknown",
                result.decidedAt != null ? result.decidedAt.toString() : "Unknown"
            );
            
        } catch (TimeoutException e) {
            Log.error("⏱️  TIMEOUT: No human decision received within 5 minutes, defaulting to REJECTED");
            return """
                Human Decision: REJECTED
                Reason: Timeout - No human decision received within 5 minutes. Defaulting to rejection for safety.
                Approved By: System (Timeout)
                """;
        } catch (Exception e) {
            Log.errorf(e, "❌ ERROR: Failed to get human approval for car %d", carNumber);
            return String.format("""
                Human Decision: REJECTED
                Reason: Error occurred while waiting for human approval: %s
                Approved By: System (Error)
                """, e.getMessage());
        }
    }
}

