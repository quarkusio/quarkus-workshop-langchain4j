package com.tripplanner.model;

public record TripRequest(
        String destination,
        String startDate,
        int days,
        String tripType,
        int travelers,
        String budget,
        String preferences
) {
}
