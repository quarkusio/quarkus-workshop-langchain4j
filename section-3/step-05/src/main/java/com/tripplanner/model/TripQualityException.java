package com.tripplanner.model;

public class TripQualityException extends RuntimeException {
    public static final String CODE = "quality_not_met";
    public static final String MESSAGE = "The vehicle recommendation did not meet the quality threshold after three revisions. Please revise your trip details and try again.";

    public TripQualityException() {
        super(MESSAGE);
    }
}
