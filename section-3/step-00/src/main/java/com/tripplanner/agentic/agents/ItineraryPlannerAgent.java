package com.tripplanner.agentic.agents;

import com.tripplanner.model.ItineraryResult;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;

public interface ItineraryPlannerAgent {

    @UserMessage("""
            You are an expert trip itinerary planner.
            Create a detailed day-by-day itinerary and a route overview for the trip.
            Include a title, description, and overnight stop for each day.
            Consider the travel dates when suggesting activities and seasonal attractions.

            - Destination: {destination}
            - Start date: {startDate}
            - Duration: {days} days
            - Trip type: {tripType}
            - Additional preferences: {preferences}
            """)
    @Agent(description = "Creates a detailed day-by-day itinerary and route overview",
           outputKey = "itineraryResult")
    ItineraryResult planItinerary(String destination,
                                  String startDate,
                                  String days,
                                  String tripType,
                                  String preferences);
}
