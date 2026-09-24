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
    void rejectsMissingOrMalformedVotes() {
        var valid = new VehicleEvaluation(8, "Good");
        assertThrows(IllegalStateException.class, () -> VehicleEvaluators.aggregateVotes(List.of()));
        assertThrows(IllegalStateException.class, () -> VehicleEvaluators.aggregateVotes(List.of(valid)));
        assertThrows(IllegalStateException.class, () -> VehicleEvaluators.aggregateVotes(
                java.util.Arrays.asList(valid, null, valid)));
        for (double score : new double[] { Double.NaN, Double.POSITIVE_INFINITY, -1, 0, 11 }) {
            assertThrows(IllegalStateException.class, () -> VehicleEvaluators.aggregateVotes(
                    List.of(valid, valid, new VehicleEvaluation(score, "Invalid"))));
        }
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
