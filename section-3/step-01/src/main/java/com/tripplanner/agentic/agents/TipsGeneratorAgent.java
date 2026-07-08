package com.tripplanner.agentic.agents;

import com.tripplanner.model.ItineraryResult;
import com.tripplanner.model.TripPlan;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;

import java.util.List;

public interface TipsGeneratorAgent {

    @UserMessage("""
            You are a practical travel advisor.
            Based on all the trip details below, generate a list of practical tips.

            Vehicle: {vehicle}
            Route: {itineraryResult}
            Trip type: {tripType}
            Estimated costs: {costs}
            Preferences: {preferences}

            Provide 5 to 8 practical, specific tips covering safety, logistics,
            local customs, packing, and money-saving advice relevant to this specific trip.
            """)
    @Agent(description = "Generates practical travel tips based on the complete trip plan",
           outputKey = "tips")
    List<String> generateTips(TripPlan.VehicleRecommendation vehicle,
                              ItineraryResult itineraryResult,
                              TripPlan.CostEstimate costs,
                              String tripType,
                              String preferences);
}
