package com.tripplanner.agentic.agents;

import com.tripplanner.guardrails.TripSafetyGuardrail;
import com.tripplanner.model.ItineraryResult;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.OutputGuardrails;
import io.quarkiverse.langchain4j.skills.Skills;

public interface ItineraryPlannerAgent {

    @UserMessage("""
            You are an expert trip itinerary planner.
            Create a detailed day-by-day itinerary and a route overview for the trip.
            Include a title, description, and overnight stop for each day.

            - Destination: {destination}
            - Duration: {days} days
            - Trip type: {tripType}
            - Additional preferences: {preferences}
            """)
    @Agent(description = "Creates a detailed day-by-day itinerary and route overview",
           outputKey = "itineraryResult")
    @OutputGuardrails(value = TripSafetyGuardrail.class, maxRetries = 3)
    @Skills
    ItineraryResult planItinerary(String destination,
                                  Integer days,
                                  String tripType,
                                  String preferences);
}
