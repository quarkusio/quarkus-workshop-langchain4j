package com.tripplanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.model.TripPlanStatus;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import com.tripplanner.testsupport.InMemoryMessagingTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.reactive.messaging.ce.CloudEventMetadata;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(InMemoryMessagingTestResource.class)
@TestProfile(TripPlanningFailureTest.ScriptedProfile.class)
class TripPlanningFailureTest {

    @Inject
    ChatModel chatModel;
    @Inject @Any
    InMemoryConnector connector;
    @Inject
    ObjectMapper objectMapper;

    ScriptedProfile.ScriptedModel model;

    @BeforeEach
    void reset() {
        model = (ScriptedProfile.ScriptedModel) chatModel;
        model.vehicleCalls.set(0);
        model.itineraryCalls.set(0);
        model.mode = "rewrite";
        connector.sink("flow-in-producer").clear();
        connector.sink("flow-out").clear();
    }

    @Test
    void correctedFieldsReachTheHttpClientThroughFlow() throws Exception {
        plan().statusCode(200)
                .body("status", equalTo("awaiting_approval"))
                .body("plan.vehicle.type", equalTo("MPV"))
                .body("plan.vehicle.model", equalTo("Family MPV; specific model subject to availability."))
                .body("plan.vehicle.reasoning", containsString("Suggested category: MPV."))
                .body("plan.vehicle.reasoning", containsString("Confirm seating, luggage capacity, price, and availability"));
        assertEquals(1, model.vehicleCalls.get());
    }

    @Test
    void exhaustedVehicleRepromptsReturn422() throws Exception {
        model.mode = "reprompt";
        assertGuardrailFailure();
        assertEquals(3, model.vehicleCalls.get(), "Current guardrail executor counts the initial answer in maxRetries");
    }

    @Test
    void exhaustedItineraryRetriesReturn422() throws Exception {
        model.mode = "retry";
        assertGuardrailFailure();
        assertEquals(3, model.itineraryCalls.get(), "Current guardrail executor counts the initial answer in maxRetries");
    }

    @Test
    void unrelatedAgentFailureReturnsSafe500() throws Exception {
        model.mode = "provider-failure";
        plan().statusCode(500).contentType("application/json")
                .body("status", equalTo("failed"))
                .body("error", equalTo("planning_failed"))
                .body("message", equalTo("Could not generate the trip plan. Please try again later."))
                .body(not(containsString("private-provider-token")));
    }

    private void assertGuardrailFailure() throws Exception {
        plan().statusCode(422).contentType("application/json")
                .body("status", equalTo("failed"))
                .body("error", equalTo("guardrail_violation"))
                .body("message", equalTo("The trip plan could not pass the recommendation checks. Please revise your trip details and try again."));
    }

    private io.restassured.response.ValidatableResponse plan() throws Exception {
        int producerBaseline = connector.sink("flow-in-producer").received().size();
        int outBaseline = connector.sink("flow-out").received().size();
        var response = CompletableFuture.supplyAsync(() -> given().contentType("application/json").body("""
                {"destination":"Italian Riviera","startDate":"2027-07-10","days":5,
                 "tripType":"family","travelers":4,"budget":"economy","preferences":"coastal towns"}
                """).post("/trip/plan"));
        await().pollInterval(50, MILLISECONDS).atMost(10, SECONDS)
                .until(() -> connector.sink("flow-in-producer").received().size() > producerBaseline);
        Message<?> input = connector.sink("flow-in-producer").received().get(producerBaseline);
        assertEquals("com.tripplanner.trip.requested", input.getMetadata(CloudEventMetadata.class).orElseThrow().getType());
        connector.<Message<String>>source("flow-in").send(Message.of(
                objectMapper.writeValueAsString(input.getPayload()), input.getMetadata()));
        await().pollInterval(50, MILLISECONDS).atMost(10, SECONDS)
                .until(() -> connector.sink("flow-out").received().size() > outBaseline);
        Message<String> output = connector.<String>sink("flow-out").received().get(outBaseline);
        connector.<Message<String>>source("flow-out-consumer").send(Message.of(output.getPayload(), output.getMetadata()));
        var http = response.get(5, SECONDS);
        var status = http.as(TripPlanStatus.class);
        assertEquals(output.getMetadata(CloudEventMetadata.class).orElseThrow().getExtension("flowinstanceid").orElseThrow(),
                status.instanceId());
        assertEquals(status, given().queryParam("instanceId", status.instanceId()).get("/trip/plan/status").as(TripPlanStatus.class));
        return http.then();
    }

    public static class ScriptedProfile implements QuarkusTestProfile {
        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(ScriptedModel.class);
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("trip.planning.timeout", "PT15S");
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
