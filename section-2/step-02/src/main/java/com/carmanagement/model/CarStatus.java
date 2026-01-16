package com.carmanagement.model;

/**
 * Enum representing the possible statuses of a car in the rental fleet.
 */
public enum CarStatus {
    RENTED("rented"),
    AT_CLEANING("at cleaning"),
    AVAILABLE("available to rent");
    
    private final String value;
    
    CarStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}


