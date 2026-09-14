package com.tripplanner.model;

import java.util.List;

public record TripPlan(
        VehicleRecommendation vehicle,
        String routeOverview,
        List<DayItinerary> itinerary,
        CostEstimate costs,
        List<String> tips
) {
    public record VehicleRecommendation(
            String type,
            String model,
            String reasoning
    ) {}

    public record DayItinerary(
            int day,
            String title,
            String description,
            String overnightStop
    ) {}

    public record CostEstimate(
            String vehiclePerDay,
            String fuel,
            String tolls,
            String accommodation,
            String food,
            String activities,
            String total
    ) {}
}
