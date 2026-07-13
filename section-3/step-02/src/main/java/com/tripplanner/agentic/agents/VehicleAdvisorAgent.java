package com.tripplanner.agentic.agents;

import com.tripplanner.guardrails.TripAppropriatenessGuardrail;
import com.tripplanner.model.TripPlan;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.OutputGuardrails;
import io.quarkiverse.langchain4j.skills.Skills;

public interface VehicleAdvisorAgent {

    @UserMessage("""
            You are a vehicle specialist for road trips.
            Based on the trip details below, recommend the most suitable vehicle.
            Consider the destination terrain, trip type, number of travelers, and budget.

            - Destination: {destination}
            - Trip type: {tripType}
            - Number of travelers: {travelers}
            - Budget: {budget}
            - Additional preferences: {preferences}
            """)
    @Agent(description = "Recommends the best vehicle for the trip based on destination, travelers, and budget",
           outputKey = "vehicle")
    @OutputGuardrails(value = TripAppropriatenessGuardrail.class, maxRetries = 3)
    @Skills
    TripPlan.VehicleRecommendation recommendVehicle(String destination,
                                                    String tripType,
                                                    Integer travelers,
                                                    String budget,
                                                    String preferences);
}
