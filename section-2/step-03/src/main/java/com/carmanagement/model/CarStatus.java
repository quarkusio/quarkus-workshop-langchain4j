package com.carmanagement.model;

/**
 * Enum representing the possible statuses of a car in the rental fleet.
 */
public enum CarStatus {
    IN_MAINTENANCE("in maintenance"),
    RENTED("rented"),
    AT_CAR_WASH("at car wash"),
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


