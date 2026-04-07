package com.carmanagement.model;

/**
 * Record representing the conditions of a car.
 *
 * @param generalCondition    A description of the car's general condition
 * @param carAssignment       Indicates the action required (DISPOSITION, MAINTENANCE, CLEANING, or NONE)
 * @param dispositionStatus   Status of disposition decision (DISPOSITION_APPROVED, DISPOSITION_REJECTED, or DISPOSITION_NOT_REQUIRED)
 * @param dispositionReason   Reason for disposition decision
 */
public record CarConditions(
    String generalCondition,
    CarAssignment carAssignment,
    String dispositionStatus,
    String dispositionReason
) {
    /**
     * Constructor for backward compatibility without disposition fields.
     */
    public CarConditions(String generalCondition, CarAssignment carAssignment) {
        this(generalCondition, carAssignment, "DISPOSITION_NOT_REQUIRED", null);
    }
}


