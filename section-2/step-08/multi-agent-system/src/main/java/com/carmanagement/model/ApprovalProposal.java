package com.carmanagement.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import java.time.LocalDateTime;

/**
 * Entity representing a disposition proposal awaiting human approval.
 * This is the core of the Human-in-the-Loop pattern - proposals are stored
 * and the workflow pauses until a human makes an approval decision.
 */
@Entity
public class ApprovalProposal extends PanacheEntity {

    /**
     * The car number this proposal is for
     */
    @Column(nullable = false)
    public Integer carNumber;

    /**
     * Car make
     */
    public String carMake;

    /**
     * Car model
     */
    public String carModel;

    /**
     * Car year
     */
    public Integer carYear;

    /**
     * Estimated car value
     */
    public String carValue;

    /**
     * Proposed disposition action (SCRAP, SELL, DONATE, KEEP)
     */
    @Column(nullable = false)
    public String proposedDisposition;

    /**
     * Reasoning for the proposed disposition
     */
    @Column(length = 2000)
    public String dispositionReason;

    /**
     * Current car condition
     */
    @Column(length = 1000)
    public String carCondition;

    /**
     * Rental feedback that triggered this proposal
     */
    @Column(length = 2000)
    public String rentalFeedback;

    /**
     * Current status of the approval
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ApprovalStatus status = ApprovalStatus.PENDING;

    /**
     * Human's decision (APPROVED or REJECTED)
     */
    public String decision;

    /**
     * Human's reasoning for their decision
     */
    @Column(length = 1000)
    public String approvalReason;

    /**
     * Who approved/rejected (for audit trail)
     */
    public String approvedBy;

    /**
     * When the proposal was created
     */
    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    /**
     * When the decision was made
     */
    public LocalDateTime decidedAt;

    /**
     * Find pending proposal for a specific car
     */
    public static ApprovalProposal findPendingByCarNumber(Integer carNumber) {
        return find("carNumber = ?1 and status = ?2", carNumber, ApprovalStatus.PENDING).firstResult();
    }

    /**
     * Find all pending proposals
     */
    public static java.util.List<ApprovalProposal> findAllPending() {
        return find("status", ApprovalStatus.PENDING).list();
    }

    /**
     * Approval status enum
     */
    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}


