package com.tripplanner.agentic.voting;

import dev.langchain4j.agentic.planner.Action;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.planner.InitPlanningContext;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.planner.PlanningContext;

import java.util.List;
import java.util.ArrayList;

public class VotingPlanner implements Planner {

    private final VotingStrategy strategy;
    private List<AgentInstance> subagents;
    private final List<Object> votes = new ArrayList<>();

    public VotingPlanner(VotingStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public void init(InitPlanningContext context) {
        this.subagents = context.subagents();
    }

    @Override
    public Action firstAction(PlanningContext context) {
        votes.clear();
        return call(subagents);
    }

    @Override
    public Action nextAction(PlanningContext context) {
        // The framework calls this after each parallel agent completes.
        votes.add(context.previousAgentInvocation().output());
        return votes.size() == subagents.size() ? done(strategy.aggregate(votes)) : noOp();
    }

    @Override
    public AgenticSystemTopology topology() {
        return AgenticSystemTopology.PARALLEL;
    }
}
