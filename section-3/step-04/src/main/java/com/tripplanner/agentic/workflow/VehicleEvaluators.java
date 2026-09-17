package com.tripplanner.agentic.workflow;

import com.tripplanner.agentic.agents.ComfortEvaluator;
import com.tripplanner.agentic.agents.CostEvaluator;
import com.tripplanner.agentic.agents.FuelEfficiencyEvaluator;
import com.tripplanner.agentic.voting.VotingPlanner;
import com.tripplanner.model.VehicleEvaluation;
import dev.langchain4j.agentic.declarative.PlannerAgent;
import dev.langchain4j.agentic.declarative.PlannerSupplier;
import dev.langchain4j.agentic.planner.Planner;

import java.util.Collection;

public interface VehicleEvaluators {

    @PlannerAgent(
            name = "vehicleEvaluators",
            description = "Dispatches evaluators in parallel and aggregates their votes",
            outputKey = "evaluation",
            subAgents = {
                    ComfortEvaluator.class,
                    CostEvaluator.class,
                    FuelEfficiencyEvaluator.class
            })
    VehicleEvaluation evaluate(String destination,
                               String tripType,
                               String travelers,
                               String days,
                               String budget);

    @PlannerSupplier
    static Planner planner() {
        return new VotingPlanner(VehicleEvaluators::aggregateVotes);
    }

    static Object aggregateVotes(Collection<Object> votes) {
        double totalScore = 0;
        StringBuilder suggestions = new StringBuilder();
        int count = 0;
        for (Object vote : votes) {
            if (vote instanceof VehicleEvaluation eval) {
                totalScore += eval.score();
                if (eval.suggestions() != null && !eval.suggestions().isBlank()) {
                    if (!suggestions.isEmpty()) {
                        suggestions.append("; ");
                    }
                    suggestions.append(eval.suggestions());
                }
                count++;
            }
        }
        double avgScore = count > 0 ? totalScore / count : 0;
        return new VehicleEvaluation(avgScore, suggestions.toString());
    }
}
