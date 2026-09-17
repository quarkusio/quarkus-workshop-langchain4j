package com.tripplanner.model;

import java.util.List;

public record ItineraryResult(
        String routeOverview,
        List<TripPlan.DayItinerary> itinerary
) {}
