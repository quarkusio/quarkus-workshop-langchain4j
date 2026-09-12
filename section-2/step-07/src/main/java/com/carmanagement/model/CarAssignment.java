package com.carmanagement.model;

/**
 * Enum representing the type of possible car assignments for car processing
 */
public enum CarAssignment {
    DISPOSITION,  // Car needs to be disposed of (scrapped, sold, or donated)
    MAINTENANCE,
    CLEANING,
    NONE
}
