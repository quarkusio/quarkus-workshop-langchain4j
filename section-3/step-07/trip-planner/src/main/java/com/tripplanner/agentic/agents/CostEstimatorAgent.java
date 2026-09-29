package com.tripplanner.agentic.agents;

import com.tripplanner.agentic.tools.RentalPricingTool;
import com.tripplanner.model.ItineraryResult;
import com.tripplanner.model.TripPlan;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.ToolBox;

public interface CostEstimatorAgent {

    @UserMessage("""
            You are a travel cost estimation expert.
            Based on the vehicle recommendation and planned itinerary, provide a detailed cost breakdown.

            Vehicle: {vehicle}
            Route: {itineraryResult}
            Rental duration in days: {days}
            Number of travelers: {travelers}
            Budget range: {budget}

            Call estimateRental before answering. Choose the closest supported category
            (compact, estate, suv, or mpv) for the recommended vehicle and use the rental duration above.
            If the tool rejects the arguments, correct them using its error message.
            Do not invent a rental price if no valid estimate is available.
            Use the returned dailyRate unchanged for vehiclePerDay, and include rentalTotal
            exactly once in the total estimate. These are fictional workshop prices in EUR.
            Estimate fuel, tolls, accommodation, food, and activities for the whole trip separately.
            Use string format for all amounts (e.g., "€150/day").
            """)
    @ToolBox(RentalPricingTool.class)
    @Agent(description = "Estimates all costs for the trip based on vehicle, itinerary, and budget",
           outputKey = "costs")
    TripPlan.CostEstimate estimateCosts(TripPlan.VehicleRecommendation vehicle,
                                        ItineraryResult itineraryResult,
                                        String days,
                                        String travelers,
                                        String budget);
}
