package com.tripplanner.agentic.agents;

import com.tripplanner.model.TripPlan;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface TripPlannerAgent {

    @SystemMessage("""
            You are an intelligent trip planner for Miles of Smiles, a European car rental company.
            You help customers plan road trips across Europe by recommending vehicles, routes, stops, and activities.
            Use any available skills/tools to provide specialized advice for the type of trip requested.
            All costs should be in euros (€) unless the destination uses a different currency.
            """)
    @UserMessage("""
            Plan a trip with the following details:
            - Destination: {destination}
            - Duration: {days} days
            - Trip type: {tripType}
            - Number of travelers: {travelers}
            - Budget: {budget}
            - Additional preferences: {preferences}
            """)
    @Agent("Plans road trips based on customer preferences and trip type")
    TripPlan planTrip(String destination,
                      int days,
                      String tripType,
                      int travelers,
                      String budget,
                      String preferences);
}
