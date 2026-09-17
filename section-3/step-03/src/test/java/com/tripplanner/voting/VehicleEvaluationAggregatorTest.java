package com.tripplanner.voting;

import com.tripplanner.agentic.workflow.VehicleEvaluators;
import com.tripplanner.model.VehicleEvaluation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VehicleEvaluationAggregatorTest {

    @Test
    void averagesScoresAndConcatenatesSuggestions() {
        var votes = List.<Object>of(
                new VehicleEvaluation(7.0, "Good comfort"),
                new VehicleEvaluation(6.0, "Over budget"),
                new VehicleEvaluation(8.0, "Excellent fuel economy"));

        var result = (VehicleEvaluation) VehicleEvaluators.aggregateVotes(votes);

        assertEquals(7.0, result.score(), 0.01);
        assertTrue(result.suggestions().contains("Good comfort"));
        assertTrue(result.suggestions().contains("Over budget"));
        assertTrue(result.suggestions().contains("Excellent fuel economy"));
    }

    @Test
    void singleVoteReturnsItsScore() {
        var votes = List.<Object>of(new VehicleEvaluation(9.0, "Perfect choice"));
        var result = (VehicleEvaluation) VehicleEvaluators.aggregateVotes(votes);

        assertEquals(9.0, result.score(), 0.01);
        assertEquals("Perfect choice", result.suggestions());
    }

    @Test
    void emptyVotesReturnZeroScore() {
        var result = (VehicleEvaluation) VehicleEvaluators.aggregateVotes(List.of());
        assertEquals(0.0, result.score(), 0.01);
    }

    @Test
    void blankSuggestionsAreSkipped() {
        var votes = List.<Object>of(
                new VehicleEvaluation(7.0, "Good"),
                new VehicleEvaluation(8.0, ""),
                new VehicleEvaluation(6.0, null));

        var result = (VehicleEvaluation) VehicleEvaluators.aggregateVotes(votes);

        assertEquals(7.0, result.score(), 0.01);
        assertEquals("Good", result.suggestions());
    }
}
