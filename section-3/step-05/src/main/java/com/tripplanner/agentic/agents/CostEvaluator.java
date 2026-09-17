package com.tripplanner.agentic.agents;

import com.tripplanner.model.TripPlan;
import com.tripplanner.model.VehicleEvaluation;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;

public interface CostEvaluator {

    @UserMessage("""
            You are a cost evaluator for road trip vehicles.
            Rate the vehicle recommendation on a scale of 1-10 for cost efficiency,
            considering rental price relative to budget, fuel economy, and overall
            value for money for the trip duration.

            Vehicle: {vehicle}
            Budget: {budget}
            Number of travelers: {travelers}
            Duration: {days} days
            """)
    @Agent(description = "Evaluates vehicle rental cost and budget fit",
           outputKey = "costEval")
    VehicleEvaluation evaluateCost(TripPlan.VehicleRecommendation vehicle,
                                   String budget,
                                   String travelers,
                                   String days);
}
