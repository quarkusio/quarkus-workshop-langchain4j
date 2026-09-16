package com.tripplanner;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(TripPlanningFailureTest.ScriptedProfile.class)
class TripPlanningFailureTest {

    @Inject
    ChatModel chatModel;

    ScriptedProfile.ScriptedModel model;

    @BeforeEach
    void reset() {
        model = (ScriptedProfile.ScriptedModel) chatModel;
        model.vehicleCalls.set(0);
        model.itineraryCalls.set(0);
        model.mode = "rewrite";
    }


    @Test
    void correctedFieldsReachTheHttpClient() {
        plan().statusCode(200)
                .body("vehicle.type", equalTo("MPV"))
                .body("vehicle.model", equalTo("Family MPV; specific model subject to availability."))
                .body("vehicle.reasoning", containsString("Suggested category: MPV."))
                .body("vehicle.reasoning", containsString("Confirm seating, luggage capacity, price, and availability"));
        assertEquals(1, model.vehicleCalls.get());
    }

    @Test
    void exhaustedVehicleRepromptsReturn422() {
        model.mode = "reprompt";
        assertGuardrailFailure();
        assertEquals(3, model.vehicleCalls.get(), "Current guardrail executor counts the initial answer in maxRetries");
    }

    @Test
    void exhaustedItineraryRetriesReturn422() {
        model.mode = "retry";
        assertGuardrailFailure();
        assertEquals(3, model.itineraryCalls.get(), "Current guardrail executor counts the initial answer in maxRetries");
    }

    @Test
    void unrelatedAgentFailureReturnsSafe500() {
        model.mode = "provider-failure";
        plan().statusCode(500).contentType("application/json")
                .body("error", equalTo("planning_failed"))
                .body("message", equalTo("Could not generate the trip plan. Please try again later."))
                .body(not(containsString("private-provider-token")));
    }

    private void assertGuardrailFailure() {
        plan().statusCode(422).contentType("application/json")
                .body("error", equalTo("guardrail_violation"))
                .body("message", equalTo("The trip plan could not pass the recommendation checks. Please revise your trip details and try again."));
    }

    private io.restassured.response.ValidatableResponse plan() {
        return given().contentType("application/json").body("""
                {"destination":"Italian Riviera","startDate":"2027-07-10","days":5,
                 "tripType":"family","travelers":4,"budget":"economy","preferences":"coastal towns"}
                """).post("/trip/plan").then();
    }

    public static class ScriptedProfile implements QuarkusTestProfile {
        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(ScriptedModel.class);
        }

        @Alternative
        @Singleton
        public static class ScriptedModel implements ChatModel {
            final AtomicInteger vehicleCalls = new AtomicInteger();
            final AtomicInteger itineraryCalls = new AtomicInteger();
            volatile String mode;

            @Override
            public ChatResponse doChat(ChatRequest request) {
                String prompt = request.messages().reversed().stream().filter(UserMessage.class::isInstance)
                        .map(UserMessage.class::cast).map(UserMessage::singleText)
                        .filter(text -> text.contains("- Destination:") || text.contains("Vehicle:"))
                        .findFirst().orElseThrow();
                String json;
                if (prompt.contains("vehicle specialist")) {
                    vehicleCalls.incrementAndGet();
                    if (mode.equals("provider-failure")) {
                        throw new IllegalStateException("private-provider-token");
                    }
                    json = mode.equals("reprompt")
                            ? "{\"type\":\"Sports car\",\"model\":\"Ferrari Roma\",\"reasoning\":\"Luxury\"}"
                            : "{\"type\":\"Sports car\",\"model\":\"Mazda MX-5\",\"reasoning\":\"Fun\"}";
                } else if (prompt.contains("itinerary planner")) {
                    itineraryCalls.incrementAndGet();
                    json = mode.equals("retry") ? "{\"itinerary\":[]}"
                            : """
                            {"routeOverview":"Local route","itinerary":[
                             {"day":1,"title":"Coast","description":"Explore the coast","overnightStop":"Genoa"}]}
                            """;
                } else if (prompt.contains("cost estimation")) {
                    // Pricing tool execution is covered by the separate pricing-agent script.
                    json = """
                            {"vehiclePerDay":"90","fuel":"0","tolls":"0","accommodation":"0",
                             "food":"0","activities":"0","total":"450"}
                            """;
                } else {
                    throw new AssertionError("Unexpected agent prompt");
                }
                return ChatResponse.builder().aiMessage(AiMessage.from(json)).build();
            }
        }
    }
}
