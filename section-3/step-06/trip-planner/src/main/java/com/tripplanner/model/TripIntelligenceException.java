package com.tripplanner.model;

public class TripIntelligenceException extends RuntimeException {
    public TripIntelligenceException() {
        super("The trip intelligence service returned unusable data. Please try again later.");
    }
}
