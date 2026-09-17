package com.tripplanner.agentic.voting;

import dev.langchain4j.agentic.planner.Action;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.planner.InitPlanningContext;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.planner.PlanningContext;

import java.util.List;
import java.util.stream.Collectors;

public class VotingPlanner implements Planner {

    private final VotingStrategy strategy;
    private List<AgentInstance> subagents;

    public VotingPlanner(VotingStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public void init(InitPlanningContext context) {
        this.subagents = context.subagents();
    }

    @Override
    public Action firstAction(PlanningContext context) {
        return call(subagents);
    }

    @Override
    public Action nextAction(PlanningContext context) {
        List<Object> votes = subagents.stream()
                .map(agent -> context.agenticScope().readState(agent.outputKey()))
                .collect(Collectors.toList());
        return done(strategy.aggregate(votes));
    }

    @Override
    public AgenticSystemTopology topology() {
        return AgenticSystemTopology.PARALLEL;
    }
}
