package com.tripplanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.agentic.agents.CostEstimatorAgent;
import com.tripplanner.agentic.agents.ItineraryPlannerAgent;
import com.tripplanner.agentic.agents.VehicleAdvisorAgent;
import com.tripplanner.agentic.workflow.ResearchPhase;
import com.tripplanner.agentic.workflow.TripPlannerSystem;
import com.tripplanner.model.ItineraryResult;
import com.tripplanner.model.TripPlan;
import dev.langchain4j.agentic.declarative.ParallelAgent;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(TripPlanContractTest.ScriptedProfile.class)
class TripPlanContractTest {
    static final TripPlan.VehicleRecommendation VEHICLE =
            new TripPlan.VehicleRecommendation("MPV", "Family car", "Room for four travelers");
    static final ItineraryResult ITINERARY = new ItineraryResult("Local coastal route", List.of(
            new TripPlan.DayItinerary(1, "Coast", "Explore the coast", "Genoa")));
    static final TripPlan.CostEstimate COSTS =
            new TripPlan.CostEstimate("90", "10", "0", "100", "50", "20", "270");

    @Inject
    ObjectMapper mapper;

    @Test
    void threeAgentWorkflowAssemblesThePlanWithoutTips() throws Exception {
        Class<?>[] inputs = { String.class, String.class, String.class, String.class,
                String.class, String.class, String.class };
        assertArrayEquals(new Class<?>[] { ResearchPhase.class, CostEstimatorAgent.class },
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

    @Test
    void planningEndpointReturnsTheSameJsonContract() throws Exception {
        String response = given().contentType("application/json").body("""
                {"destination":"Italian Riviera","startDate":"2027-07-10","days":1,
                 "tripType":"family","travelers":4,"budget":"moderate","preferences":"coastal towns"}
                """).post("/trip/plan").then().statusCode(200).contentType("application/json")
                .extract().asString();
        assertEquals(mapper.valueToTree(TripPlannerSystem.output(VEHICLE, ITINERARY, COSTS)),
                mapper.readTree(response));
    }

    @Test
    void chatEndpointIsNotPartOfTheBaseline() {
        given().contentType("application/json").body("""
                {"sessionId":"contract-test","message":"Plan a trip"}
                """).post("/trip/chat").then().statusCode(404);
    }

    public static class ScriptedProfile implements QuarkusTestProfile {
        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(ScriptedModel.class);
        }

        @Alternative
        @Singleton
        public static class ScriptedModel implements ChatModel {
            @Inject
            ObjectMapper mapper;

            @Override
            public ChatResponse doChat(ChatRequest request) {
                String prompt = request.messages().reversed().stream().filter(UserMessage.class::isInstance)
                        .map(UserMessage.class::cast).map(UserMessage::singleText)
                        .filter(text -> text.contains("- Destination:") || text.contains("Vehicle:"))
                        .findFirst().orElseThrow();
                Object result;
                if (prompt.contains("vehicle specialist")) {
                    result = VEHICLE;
                } else if (prompt.contains("itinerary planner")) {
                    result = ITINERARY;
                } else if (prompt.contains("cost estimation")) {
                    assertTrue(prompt.contains(VEHICLE.model()), "Cost estimation must receive the vehicle");
                    assertTrue(prompt.contains(ITINERARY.routeOverview()), "Cost estimation must receive the route");
                    // This baseline checks assembly; skill and pricing tool calls have separate exercises.
                    result = COSTS;
                } else {
                    throw new AssertionError("Unexpected agent prompt");
                }
                return ChatResponse.builder().aiMessage(AiMessage.from(mapper.valueToTree(result).toString())).build();
            }
        }
    }
}
