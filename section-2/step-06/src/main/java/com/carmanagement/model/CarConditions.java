package com.carmanagement.model;

/**
 * Record representing the conditions of a car.
 *
 * @param generalCondition   A description of the car's general condition
 * @param carAssignment      Indicates the action required
 * @param approvalStatus     Status of human approval (APPROVED/REJECTED/NOT_REQUIRED)
 * @param approvalReason     Reason for approval decision
 */
public record CarConditions(
    String generalCondition, 
    CarAssignment carAssignment,
    String approvalStatus,
    String approvalReason
) {
    /**
     * Constructor for backward compatibility without approval fields.
     */
    public CarConditions(String generalCondition, CarAssignment carAssignment) {
        this(generalCondition, carAssignment, "NOT_REQUIRED", null);
    }
}


