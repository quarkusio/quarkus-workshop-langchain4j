package com.tripplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TripPlan(
        VehicleRecommendation vehicle,
        String routeOverview,
        List<DayItinerary> itinerary,
        CostEstimate costs
) {
    public record VehicleRecommendation(
            String type,
            String model,
            String reasoning,
            @JsonProperty(access = JsonProperty.Access.READ_ONLY) String guardrailOverride
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
