package com.carmanagement.service;

import com.carmanagement.model.ApprovalProposal;
import com.carmanagement.model.ApprovalProposal.ApprovalStatus;
import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import static jakarta.transaction.Transactional.TxType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for managing approval proposals and the Human-in-the-Loop workflow.
 * This service handles the async nature of human approvals - creating proposals,
 * waiting for human decisions, and continuing workflow execution.
 */
@ApplicationScoped
public class ApprovalService {

    @Inject
    EntityManager entityManager;

    /**
     * Map to store CompletableFutures waiting for approval decisions.
     * Key: carNumber, Value: CompletableFuture that completes when decision is made
     */
    private final Map<Integer, CompletableFuture<ApprovalProposal>> pendingApprovals = new ConcurrentHashMap<>();
    
    /**
     * Executor for async proposal creation to ensure transaction commits before blocking
     */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Create a new approval proposal and return a CompletableFuture that will complete
     * when a human makes a decision.
     * 
     * @param carNumber The car number
     * @param carMake Car make
     * @param carModel Car model
     * @param carYear Car year
     * @param carValue Estimated car value
     * @param proposedDisposition Proposed action (SCRAP, SELL, DONATE, KEEP)
     * @param dispositionReason Reasoning for the proposal
     * @param carCondition Current car condition
     * @param rentalFeedback Rental feedback
     * @return CompletableFuture that completes when human approves/rejects
     */
    public CompletableFuture<ApprovalProposal> createProposalAndWaitForDecision(
            Integer carNumber,
            String carMake,
            String carModel,
            Integer carYear,
            String carValue,
            String proposedDisposition,
            String dispositionReason,
            String carCondition,
            String rentalFeedback) {

        // Check if there's already a pending proposal for this car
        ApprovalProposal existing = ApprovalProposal.findPendingByCarNumber(carNumber);
        if (existing != null) {
            Log.warnf("Proposal already exists for car %d, returning existing future", carNumber);
            return pendingApprovals.computeIfAbsent(carNumber, k -> new CompletableFuture<>());
        }

        // Create CompletableFuture first
        CompletableFuture<ApprovalProposal> future = new CompletableFuture<>();
        pendingApprovals.put(carNumber, future);

        // Create proposal in separate thread with its own transaction
        // This ensures the transaction commits BEFORE we return the future
        executor.submit(() -> {
            try {
                createProposalInNewTransaction(carNumber, carMake, carModel, carYear, carValue,
                        proposedDisposition, dispositionReason, carCondition, rentalFeedback);
                Log.info("✅ Proposal creation transaction committed - now visible to queries");
            } catch (Exception e) {
                Log.errorf(e, "Failed to create proposal for car %d", carNumber);
                future.completeExceptionally(e);
                pendingApprovals.remove(carNumber);
            }
        });

        return future;
    }

    @Transactional(TxType.REQUIRES_NEW)
    void createProposalInNewTransaction(
            Integer carNumber,
            String carMake,
            String carModel,
            Integer carYear,
            String carValue,
            String proposedDisposition,
            String dispositionReason,
            String carCondition,
            String rentalFeedback) {

        // Create new proposal
        ApprovalProposal proposal = new ApprovalProposal();
        proposal.carNumber = carNumber;
        proposal.carMake = carMake;
        proposal.carModel = carModel;
        proposal.carYear = carYear;
        proposal.carValue = carValue;
        proposal.proposedDisposition = proposedDisposition;
        proposal.dispositionReason = dispositionReason;
        proposal.carCondition = carCondition;
        proposal.rentalFeedback = rentalFeedback;
        proposal.status = ApprovalStatus.PENDING;
        proposal.createdAt = LocalDateTime.now();

        proposal.persist();
        entityManager.flush();

        Log.infof("Created approval proposal ID=%d for car %d - %s %s %s (Value: %s, Proposed: %s)",
                proposal.id, carNumber, carYear, carMake, carModel, carValue, proposedDisposition);
        Log.info("⏸️  WORKFLOW PAUSED - Waiting for human approval decision");
        Log.infof("Proposal persisted with ID: %d, status: %s", proposal.id, proposal.status);
    }

    /**
     * Process a human's approval decision and complete the waiting CompletableFuture.
     * This resumes the workflow execution.
     * 
     * @param proposalId The proposal ID
     * @param approved Whether approved or rejected
     * @param reason Human's reasoning
     * @param approvedBy Who made the decision
     * @return The updated proposal
     */
    @Transactional(TxType.REQUIRES_NEW)
    public ApprovalProposal processDecision(Integer proposalId, boolean approved, String reason, String approvedBy) {
        ApprovalProposal proposal = ApprovalProposal.findById(proposalId);
        if (proposal == null) {
            throw new IllegalArgumentException("Proposal not found: " + proposalId);
        }

        if (proposal.status != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Proposal is not pending: " + proposalId);
        }

        // Update proposal with decision
        proposal.status = approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED;
        proposal.decision = approved ? "APPROVED" : "REJECTED";
        proposal.approvalReason = reason;
        proposal.approvedBy = approvedBy;
        proposal.decidedAt = LocalDateTime.now();

        proposal.persist();

        Log.infof("Human decision received for car %d: %s - %s",
                proposal.carNumber, proposal.decision, reason);
        Log.info("▶️  WORKFLOW RESUMED - Continuing with approval decision");

        // Complete the CompletableFuture to resume workflow
        CompletableFuture<ApprovalProposal> future = pendingApprovals.remove(proposal.carNumber);
        if (future != null) {
            future.complete(proposal);
        }

        return proposal;
    }

    /**
     * Get all pending approval proposals.
     */
    public List<ApprovalProposal> getPendingProposals() {
        return ApprovalProposal.findAllPending();
    }

    /**
     * Get a specific proposal by ID.
     */
    public ApprovalProposal getProposal(Integer proposalId) {
        return ApprovalProposal.findById(proposalId);
    }

    /**
     * Check if there's a pending approval for a car.
     */
    public ApprovalProposal getPendingProposalForCar(Integer carNumber) {
        return ApprovalProposal.findPendingByCarNumber(carNumber);
    }
}


