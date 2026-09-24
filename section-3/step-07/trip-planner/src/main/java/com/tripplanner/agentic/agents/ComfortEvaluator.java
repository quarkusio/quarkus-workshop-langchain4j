package com.tripplanner.agentic.agents;

import com.tripplanner.model.TripPlan;
import com.tripplanner.model.VehicleEvaluation;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;

public interface ComfortEvaluator {

    @UserMessage("""
            You are a comfort evaluator for road trip vehicles.
            Rate the vehicle recommendation on a scale of 1-10 for passenger comfort,
            considering interior space, luggage capacity, ride quality, and suitability
            for the number of travelers and trip type.

            Vehicle: {vehicle}
            Trip type: {tripType}
            Number of travelers: {travelers}
            Duration: {days} days
            """)
    @Agent(description = "Evaluates vehicle comfort, space, and luggage capacity",
           outputKey = "comfortEval")
    VehicleEvaluation evaluateComfort(TripPlan.VehicleRecommendation vehicle,
                                      String tripType,
                                      String travelers,
                                      String days);
}
