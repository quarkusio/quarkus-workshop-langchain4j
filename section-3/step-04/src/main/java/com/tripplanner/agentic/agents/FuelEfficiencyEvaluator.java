package com.tripplanner.agentic.agents;

import com.tripplanner.model.TripPlan;
import com.tripplanner.model.VehicleEvaluation;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;

public interface FuelEfficiencyEvaluator {

    @UserMessage("""
            You are a fuel efficiency evaluator for road trip vehicles.
            Rate the vehicle recommendation on a scale of 1-10 for fuel efficiency,
            considering fuel consumption, driving range, and environmental impact
            for the planned trip duration and destination.

            Vehicle: {vehicle}
            Destination: {destination}
            Duration: {days} days
            Trip type: {tripType}
            """)
    @Agent(description = "Evaluates vehicle fuel consumption, range, and environmental impact",
           outputKey = "fuelEval")
    VehicleEvaluation evaluateFuelEfficiency(TripPlan.VehicleRecommendation vehicle,
                                             String destination,
                                             String days,
                                             String tripType);
}
