package com.tripplanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.agentic.agents.CostEstimatorAgent;
import com.tripplanner.agentic.agents.ItineraryPlannerAgent;
import com.tripplanner.agentic.agents.VehicleAdvisorAgent;
import com.tripplanner.agentic.workflow.ResearchPhase;
import com.tripplanner.agentic.workflow.TripPlannerSystem;
import com.tripplanner.agentic.workflow.VehicleReviewLoop;
import com.tripplanner.model.ItineraryResult;
import com.tripplanner.model.TripPlan;
import dev.langchain4j.agentic.declarative.ParallelAgent;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TripPlanContractTest {
    static final TripPlan.VehicleRecommendation VEHICLE =
            new TripPlan.VehicleRecommendation("MPV", "Family car", "Room for four travelers", null);
    static final ItineraryResult ITINERARY = new ItineraryResult("Local coastal route", List.of(
            new TripPlan.DayItinerary(1, "Coast", "Explore the coast", "Genoa")));
    static final TripPlan.CostEstimate COSTS =
            new TripPlan.CostEstimate("90", "10", "0", "100", "50", "20", "270");

    @Inject
    ObjectMapper mapper;

    @Test
    void reviewLoopSitsBetweenResearchAndCostEstimationWithoutChangingTheJsonContract() throws Exception {
        Class<?>[] inputs = { String.class, String.class, String.class, String.class,
                String.class, String.class, String.class };
        assertArrayEquals(new Class<?>[] { ResearchPhase.class, VehicleReviewLoop.class, CostEstimatorAgent.class },
                TripPlannerSystem.class.getMethod("planTrip", inputs).getAnnotation(SequenceAgent.class).subAgents());
        assertArrayEquals(new Class<?>[] { VehicleAdvisorAgent.class, ItineraryPlannerAgent.class },
                ResearchPhase.class.getMethod("research", inputs).getAnnotation(ParallelAgent.class).subAgents());

        TripPlan plan = TripPlannerSystem.output(VEHICLE, ITINERARY, COSTS);
        assertSame(VEHICLE, plan.vehicle());
        assertEquals(ITINERARY.routeOverview(), plan.routeOverview());
        assertSame(ITINERARY.itinerary(), plan.itinerary());
        assertSame(COSTS, plan.costs());
        var json = mapper.valueToTree(plan);
        assertEquals(4, json.size());
        assertFalse(json.has("tips"));
        assertEquals(plan, mapper.treeToValue(json, TripPlan.class));
    }
}
