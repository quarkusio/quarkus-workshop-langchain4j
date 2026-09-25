package com.tripplanner.agentic.workflow;

import com.tripplanner.agentic.agents.VehicleReviser;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.VehicleEvaluation;
import com.tripplanner.model.TripQualityException;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.declarative.ExitCondition;
import dev.langchain4j.agentic.declarative.LoopAgent;

public interface VehicleReviewLoop {

    int MAX_REVISIONS = 3;

    @LoopAgent(
            name = "vehicleReviewLoop",
            description = "Iteratively evaluates and refines the vehicle recommendation",
            outputKey = "vehicle",
            // One initial evaluation plus an evaluation after each permitted revision.
            maxIterations = MAX_REVISIONS + 1,
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

    @ExitCondition(
            testExitAtLoopEnd = false,
            description = "Exits when the average evaluation score reaches 7.5")
    static boolean shouldExit(VehicleEvaluation evaluation, AgenticScope scope) {
        if (evaluation != null && evaluation.score() >= 7.5) return true;
        // Checked after evaluation, before another revision can leave an unscored candidate.
        if (scope.agentInvocations(VehicleEvaluators.class).size() > MAX_REVISIONS) throw new TripQualityException();
        return false;
    }
}
