package com.tripplanner.agentic.workflow;

import com.tripplanner.agentic.agents.VehicleReviser;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.VehicleEvaluation;
import dev.langchain4j.agentic.declarative.ExitCondition;
import dev.langchain4j.agentic.declarative.LoopAgent;

public interface VehicleReviewLoop {

    @LoopAgent(
            name = "vehicleReviewLoop",
            description = "Iteratively evaluates and refines the vehicle recommendation",
            outputKey = "vehicle",
            maxIterations = 3,
            subAgents = {
                    VehicleEvaluators.class,
                    VehicleReviser.class
            })
    TripPlan.VehicleRecommendation reviewVehicle(String destination,
                                                  String startDate,
                                                  String days,
                                                  String tripType,
                                                  String travelers,
                                                  String budget,
                                                  String preferences);

    @ExitCondition(testExitAtLoopEnd = true,
            description = "Exits when the average evaluation score reaches 7.5")
    static boolean shouldExit(VehicleEvaluation evaluation) {
        return evaluation != null && evaluation.score() >= 7.5;
    }
}
