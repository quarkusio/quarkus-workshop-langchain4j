package com.carmanagement.agentic.agents;

import com.carmanagement.model.ApprovalProposal;
import com.carmanagement.model.FeedbackContext;
import com.carmanagement.service.ApprovalService;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.HumanInTheLoop;
import io.quarkus.arc.Arc;
import io.quarkus.logging.Log;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface HumanApprovalAgent {

    @Agent(outputKey = "approvalDecision", description = "Coordinates human approval for high-value vehicle dispositions using the requestHumanApproval tool")
    @HumanInTheLoop(outputKey = "approvalDecision", description = "Coordinates human approval for high-value vehicle dispositions using the requestHumanApproval tool")
    static String reviewDispositionProposal(
            String carMake,
            String carModel,
            Integer carYear,
            Integer carNumber,
            String carValue,
            String dispositionProposal,
            String dispositionReason,
            String carCondition,
            FeedbackContext feedback
    ) {

        Log.infof("🛑 HITL Tool: Creating approval proposal for car %d - %s %s %s",
                carNumber, carYear, carMake, carModel);
        Log.info("⏸️  WORKFLOW PAUSED - Waiting for human approval decision via UI");

        ApprovalService approvalService = Arc.container().instance(ApprovalService.class).get();

        try {
            // Create proposal and get CompletableFuture that completes when human decides
            CompletableFuture<ApprovalProposal> approvalFuture =
                    approvalService.createProposalAndWaitForDecision(
                            carNumber, carMake, carModel, carYear, carValue,
                            dispositionProposal, dispositionReason, carCondition, feedback.rentalFeedback()
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
