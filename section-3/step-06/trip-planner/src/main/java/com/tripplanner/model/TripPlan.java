package com.tripplanner.model;

import dev.langchain4j.model.output.structured.Description;
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
            @Description("Leave empty. Only the application sets this field.") String guardrailOverride
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
