package com.tripplanner.agentic.agents;

import com.tripplanner.model.ItineraryResult;
import com.tripplanner.model.TripPlan;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;

public interface CostEstimatorAgent {

    @UserMessage("""
            You are a travel cost estimation expert.
            Based on the vehicle recommendation and planned itinerary, provide a detailed cost breakdown.

            Vehicle: {vehicle}
            Route: {itineraryResult}
            Number of travelers: {travelers}
            Budget range: {budget}

            Provide realistic cost estimates for: vehicle rental per day, fuel, tolls,
            accommodation, food, activities, and a total estimate.
            Use string format for all amounts (e.g., "€150/day").
            """)
    @Agent(description = "Estimates all costs for the trip based on vehicle, itinerary, and budget",
           outputKey = "costs")
    TripPlan.CostEstimate estimateCosts(TripPlan.VehicleRecommendation vehicle,
                                        ItineraryResult itineraryResult,
                                        int travelers,
                                        String budget);
}
