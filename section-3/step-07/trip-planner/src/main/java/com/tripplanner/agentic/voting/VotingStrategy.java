package com.tripplanner.agentic.voting;

import java.util.Collection;

@FunctionalInterface
public interface VotingStrategy {
    Object aggregate(Collection<Object> votes);
}
