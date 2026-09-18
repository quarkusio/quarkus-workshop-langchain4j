package com.tripplanner.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.agentic.flow.TripPlannerFlowAdapter;
import com.tripplanner.model.PlanningRequest;
import com.tripplanner.model.TripApproval;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripPlanStatus;
import com.tripplanner.model.TripRequest;
import com.tripplanner.testsupport.InMemoryMessagingTestResource;
import dev.langchain4j.agentic.agent.AgentInvocationException;
import dev.langchain4j.guardrail.OutputGuardrailException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import io.smallrye.reactive.messaging.ce.CloudEventMetadata;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Smoke tests for the Step 03 Flow boundary: in-memory messaging, store updates, and REST envelopes.
 * Guardrail behaviour through the synchronous API stays in Step 02.
 */
@QuarkusTest
@QuarkusTestResource(InMemoryMessagingTestResource.class)
class TripPlannerFlowTest {
    private static final TripRequest REQUEST = new TripRequest(
            "California Coast", "2026-08-15", 5, "family", 4, "$3000", "beach and scenic drives");
    private static final TripPlan PLAN = new TripPlan(
            new TripPlan.VehicleRecommendation("SUV", "Generic SUV", "Category recommendation only"),
            "Pacific Coast Highway",
            List.of(new TripPlan.DayItinerary(1, "Arrival", "Explore Santa Monica", "Santa Monica")),
            new TripPlan.CostEstimate("$120", "$80", "$10", "$200", "$100", "$50", "$560"));

    @Inject @Any
    InMemoryConnector connector;
    @Inject
    ObjectMapper objectMapper;
    @InjectMock
    TripPlannerFlowAdapter adapter;

    @BeforeEach
    void setup() {
        reset(adapter);
        when(adapter.planFromRequest(any())).thenReturn(PLAN);
        when(adapter.finalizeBooking(any())).thenCallRealMethod();
        connector.sink("flow-in-producer").clear();
        connector.sink("flow-out").clear();
    }

    @Test
    void approvalCompletesOnlyAfterTheOutcomeEvent() throws Exception {
        TripPlanStatus planned = plan();
        assertEquals(REQUEST, planned.request());
        assertEquals(PLAN, planned.plan());

        submit(planned, "approved");
        assertEquals("decision_submitted", status(planned.instanceId()).status());
        relayDecision();
        relayOutput(outcome(planned.instanceId(), "com.tripplanner.booking.finalized"));

        await().atMost(5, SECONDS).until(() -> "confirmed".equals(status(planned.instanceId()).status()));
        TripPlanStatus confirmed = status(planned.instanceId());
        assertEquals(PLAN, confirmed.plan());
        assertTrue(confirmed.confirmation().message().contains("Simulated booking"));
        verify(adapter).finalizeBooking(new TripApproval(planned.instanceId(), "approved", ""));
    }

    @Test
    void rejectionNeverFinalizes() throws Exception {
        TripPlanStatus planned = plan();
        submit(planned, "rejected");
        relayDecision();
        relayOutput(outcome(planned.instanceId(), "com.tripplanner.trip.rejected"));

        await().atMost(5, SECONDS).until(() -> "rejected".equals(status(planned.instanceId()).status()));
        assertNull(status(planned.instanceId()).confirmation());
        verify(adapter, never()).finalizeBooking(any());
    }

    @Test
    void planningFailuresReturnSafeHttpResponses() throws Exception {
        for (boolean guardrail : new boolean[] {true, false}) {
            RuntimeException failure = guardrail
                    ? new AgentInvocationException(new IllegalStateException(new OutputGuardrailException("private response")))
                    : new AgentInvocationException(new IllegalStateException("private provider token"));
            doThrow(failure).when(adapter).planFromRequest(any());

            int baseline = connector.sink("flow-out").received().size();
            CompletableFuture<Response> response = startPlanning();
            await().atMost(5, SECONDS).until(() -> connector.sink("flow-out").received().size() > baseline);
            relayOutput(connector.<String>sink("flow-out").received().get(baseline));

            response.get(5, SECONDS).then()
                    .statusCode(guardrail ? 422 : 500)
                    .body("status", equalTo("failed"))
                    .body("error", equalTo(guardrail ? "guardrail_violation" : "planning_failed"))
                    .body("message", not(containsString("private")));
        }
    }

    private TripPlanStatus plan() throws Exception {
        int baseline = connector.sink("flow-out").received().size();
        CompletableFuture<Response> response = startPlanning();
        await().atMost(5, SECONDS).until(() -> connector.sink("flow-out").received().size() > baseline);
        relayOutput(connector.<String>sink("flow-out").received().get(baseline));
        return response.get(5, SECONDS).then().statusCode(200).body("status", equalTo("awaiting_approval"))
                .extract().as(TripPlanStatus.class);
    }

    private CompletableFuture<Response> startPlanning() throws Exception {
        int baseline = connector.sink("flow-in-producer").received().size();
        CompletableFuture<Response> response = CompletableFuture.supplyAsync(() ->
                given().contentType("application/json").body(REQUEST).post("/trip/plan"));
        await().atMost(5, SECONDS).until(() -> connector.sink("flow-in-producer").received().size() > baseline);
        Message<?> message = connector.sink("flow-in-producer").received().get(baseline);
        assertEquals("com.tripplanner.trip.requested", metadata(message).getType());
        assertInstanceOf(PlanningRequest.class, message.getPayload());
        connector.<Message<String>>source("flow-in").send(Message.of(
                objectMapper.writeValueAsString(message.getPayload()), message.getMetadata()));
        return response;
    }

    private void submit(TripPlanStatus trip, String decision) {
        given().contentType("application/json").body(new TripApproval(trip.instanceId(), decision, ""))
                .put("/trip/approve").then().statusCode(202)
                .body("instanceId", equalTo(trip.instanceId())).body("status", equalTo("decision_submitted"));
    }

    private void relayDecision() throws Exception {
        Message<?> message = connector.sink("flow-in-producer").received().getLast();
        assertEquals("com.tripplanner.trip.approval.done", metadata(message).getType());
        connector.<Message<String>>source("flow-in").send(Message.of(
                objectMapper.writeValueAsString(message.getPayload()), message.getMetadata()));
    }

    private void relayOutput(Message<String> message) {
        connector.<Message<String>>source("flow-out-consumer").send(Message.of(message.getPayload(), message.getMetadata()));
    }

    private Message<String> outcome(String instanceId, String type) {
        await().atMost(5, SECONDS).until(() -> connector.<String>sink("flow-out").received().stream().anyMatch(message ->
                type.equals(metadata(message).getType())
                        && instanceId.equals(metadata(message).getExtension("flowinstanceid").orElse(null))));
        return connector.<String>sink("flow-out").received().stream().filter(message ->
                type.equals(metadata(message).getType())
                        && instanceId.equals(metadata(message).getExtension("flowinstanceid").orElse(null)))
                .findFirst().orElseThrow();
    }

    private TripPlanStatus status(String instanceId) {
        return given().queryParam("instanceId", instanceId).get("/trip/plan/status").then().statusCode(200)
                .extract().as(TripPlanStatus.class);
    }

    private CloudEventMetadata<?> metadata(Message<?> message) {
        return message.getMetadata(CloudEventMetadata.class).orElseThrow();
    }
}
