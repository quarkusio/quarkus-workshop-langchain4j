package com.tripplanner.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.agentic.flow.TripPlannerFlowAdapter;
import com.tripplanner.model.PlanningRequest;
import com.tripplanner.model.TripApproval;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripPlanStatus;
import com.tripplanner.model.TripRequest;
import dev.langchain4j.agentic.agent.AgentInvocationException;
import dev.langchain4j.guardrail.OutputGuardrailException;
import com.tripplanner.testsupport.InMemoryMessagingTestResource;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkiverse.flow.messaging.FlowDomainEventsPublisher;
import io.cloudevents.CloudEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import io.smallrye.reactive.messaging.ce.CloudEventMetadata;
import io.smallrye.reactive.messaging.ce.OutgoingCloudEventMetadata;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@QuarkusTestResource(InMemoryMessagingTestResource.class)
@TestProfile(TripPlannerFlowTest.PublicationProfile.class)
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

    @InjectSpy
    FlowDomainEventsPublisher publisher;

    @BeforeEach
    void setup() {
        reset(adapter, publisher);
        when(adapter.planFromRequest(any())).thenReturn(PLAN);
        when(adapter.finalizeBooking(any())).thenCallRealMethod();
        connector.sink("flow-in-producer").clear();
        connector.sink("flow-out").clear();
    }

    @Test
    void approvalRestoresOriginalRequestAndConfirmsOnlyAfterOutcome() throws Exception {
        TripPlanStatus planned = plan();
        assertEquals(REQUEST, planned.request());
        assertEquals(PLAN, planned.plan());
        assertNotEquals(planned.requestId(), planned.instanceId());
        assertEquals(planned, given().get("/trip/plan/latest").as(TripPlanStatus.class));
        assertEquals(planned, status(planned.instanceId()));

        submit(planned, "approved");
        assertEquals("decision_submitted", status(planned.instanceId()).status());
        verify(adapter, never()).finalizeBooking(any());
        relayDecision();
        Message<String> outcome = outcome(planned.instanceId(), "com.tripplanner.booking.finalized");
        // Publication alone does not change the REST state until the store consumes the event.
        assertEquals("decision_submitted", status(planned.instanceId()).status());
        relayOutput(outcome);
        await().atMost(5, SECONDS).until(() -> "confirmed".equals(status(planned.instanceId()).status()));
        TripPlanStatus confirmed = status(planned.instanceId());
        assertEquals(PLAN, confirmed.plan());
        assertEquals(REQUEST, confirmed.request());
        assertTrue(confirmed.confirmation().message().contains("Simulated booking"));
        assertTrue(confirmed.confirmation().message().contains("No vehicle has been reserved"));
        verify(adapter).finalizeBooking(new TripApproval(planned.instanceId(), "approved", ""));
        given().contentType("application/json").body(new TripApproval(planned.instanceId(), "rejected", ""))
                .put("/trip/approve").then().statusCode(409);
    }

    @Test
    void rejectionSurvivesRefreshAndNeverFinalizes() throws Exception {
        TripPlanStatus planned = plan();
        submit(planned, "rejected");
        relayDecision();
        relayOutput(outcome(planned.instanceId(), "com.tripplanner.trip.rejected"));
        await().atMost(5, SECONDS).until(() -> "rejected".equals(status(planned.instanceId()).status()));
        TripPlanStatus restored = given().get("/trip/plan/latest").as(TripPlanStatus.class);
        assertEquals(planned.instanceId(), restored.instanceId());
        assertEquals("rejected", restored.status());
        assertEquals(REQUEST, restored.request());
        assertEquals(PLAN, restored.plan());
        assertNull(restored.confirmation());
        verify(adapter, never()).finalizeBooking(any());
        given().contentType("application/json").body(new TripApproval(planned.instanceId(), "approved", ""))
                .put("/trip/approve").then().statusCode(409);
    }

    @Test
    void invalidUnknownAndDuplicateDecisionsAreNotAccepted() throws Exception {
        given().contentType("application/json").body("null").put("/trip/approve").then().statusCode(400);
        given().contentType("application/json").body(new TripApproval(" ", "approved", ""))
                .put("/trip/approve").then().statusCode(400);
        given().contentType("application/json").body(new TripApproval("unknown", "approved", ""))
                .put("/trip/approve").then().statusCode(404);
        TripPlanStatus planned = plan();
        for (String invalid : new String[] {null, "", "approve", "APPROVED"}) {
            given().contentType("application/json").body(new TripApproval(planned.instanceId(), invalid, ""))
                    .put("/trip/approve").then().statusCode(400).body("error", equalTo("invalid_decision"));
        }
        assertEquals("awaiting_approval", status(planned.instanceId()).status());
        submit(planned, "rejected");
        given().contentType("application/json").body(new TripApproval(planned.instanceId(), "approved", ""))
                .put("/trip/approve").then().statusCode(409);
        relayDecision();
        relayOutput(outcome(planned.instanceId(), "com.tripplanner.trip.rejected"));
    }

    @Test
    void wrongInstanceDoesNotResumeApprovalWait() throws Exception {
        TripPlanStatus first = plan();
        TripPlanStatus second = plan();
        connector.<Message<String>>source("flow-in").send(event("com.tripplanner.trip.approval.done", "unrelated-instance",
                new TripApproval(first.instanceId(), "approved", "")));
        await().during(300, MILLISECONDS).atMost(2, SECONDS).until(() ->
                "awaiting_approval".equals(status(first.instanceId()).status())
                && "awaiting_approval".equals(status(second.instanceId()).status()));
        verify(adapter, never()).finalizeBooking(any());
        submit(second, "approved");
        relayDecision();
        relayOutput(outcome(second.instanceId(), "com.tripplanner.booking.finalized"));
        await().atMost(5, SECONDS).until(() -> "confirmed".equals(status(second.instanceId()).status()));
        assertEquals("awaiting_approval", status(first.instanceId()).status());
        submit(first, "rejected");
        relayDecision();
        relayOutput(outcome(first.instanceId(), "com.tripplanner.trip.rejected"));
        verify(adapter, times(1)).finalizeBooking(any());
    }

    @Test
    void unsolicitedAndContradictoryDecisionsLeaveTheApprovalWaitIntact() throws Exception {
        TripPlanStatus planned = plan();
        int baseline = connector.sink("flow-out").received().size();
        connector.<Message<String>>source("flow-in").send(event("com.tripplanner.trip.approval.done", planned.instanceId(),
                new TripApproval(planned.instanceId(), "approved", "")));
        await().during(300, MILLISECONDS).atMost(2, SECONDS).until(() ->
                connector.sink("flow-out").received().size() == baseline);
        assertEquals("awaiting_approval", status(planned.instanceId()).status());

        submit(planned, "rejected");
        connector.<Message<String>>source("flow-in").send(event("com.tripplanner.trip.approval.done", planned.instanceId(),
                new TripApproval(planned.instanceId(), "approved", "")));
        await().during(300, MILLISECONDS).atMost(2, SECONDS).until(() ->
                connector.sink("flow-out").received().size() == baseline);
        assertEquals("decision_submitted", status(planned.instanceId()).status());
        verify(adapter, never()).finalizeBooking(any());

        relayDecision();
        relayOutput(outcome(planned.instanceId(), "com.tripplanner.trip.rejected"));
        await().atMost(5, SECONDS).until(() -> "rejected".equals(status(planned.instanceId()).status()));
    }

    @Test
    void decisionPayloadForAnotherSubmittedTripCannotConsumeMatchingEnvelopeWait() throws Exception {
        TripPlanStatus first = plan();
        TripPlanStatus second = plan();
        submit(first, "rejected");
        Message<?> firstDecision = connector.sink("flow-in-producer").received().getLast();
        submit(second, "approved");
        int baseline = connector.sink("flow-out").received().size();

        connector.<Message<String>>source("flow-in").send(event("com.tripplanner.trip.approval.done", first.instanceId(),
                new TripApproval(second.instanceId(), "approved", "")));
        await().during(300, MILLISECONDS).atMost(2, SECONDS).until(() ->
                connector.sink("flow-out").received().size() == baseline);
        verify(adapter, never()).finalizeBooking(any());
        relayDecision();
        relayOutput(outcome(second.instanceId(), "com.tripplanner.booking.finalized"));
        connector.<Message<String>>source("flow-in").send(Message.of(
                objectMapper.writeValueAsString(firstDecision.getPayload()), firstDecision.getMetadata()));
        relayOutput(outcome(first.instanceId(), "com.tripplanner.trip.rejected"));
        await().atMost(5, SECONDS).until(() -> "confirmed".equals(status(second.instanceId()).status())
                && "rejected".equals(status(first.instanceId()).status()));
    }

    @Test
    void unrelatedPlanningEventCannotCompleteCurrentHttpRequest() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(adapter.planFromRequest(any())).thenAnswer(invocation -> {
            entered.countDown();
            assertTrue(release.await(10, SECONDS));
            return PLAN;
        });
        CompletableFuture<Response> response = startPlanning();
        try {
            assertTrue(entered.await(5, SECONDS));
            TripPlanStatus pending = given().get("/trip/plan/latest").as(TripPlanStatus.class);
            TripPlanStatus unrelated = new TripPlanStatus("unrelated-request", pending.instanceId(), REQUEST,
                    "awaiting_approval", PLAN, null, null, null);
            connector.<Message<String>>source("flow-out-consumer").send(event("com.tripplanner.trip.approval.requested",
                    pending.instanceId(), unrelated));
            connector.<Message<String>>source("flow-out-consumer").send(event("com.tripplanner.trip.approval.requested",
                    "unrelated-instance", pending.outcome("awaiting_approval", PLAN, null)));
            await().during(300, MILLISECONDS).atMost(2, SECONDS).until(() -> !response.isDone());
            assertEquals("planning", status(pending.instanceId()).status());
        } finally {
            release.countDown();
        }
        relayOutput(nextPlanningOutcome());
        TripPlanStatus result = response.get(5, SECONDS).then().statusCode(200).extract().as(TripPlanStatus.class);
        assertEquals(PLAN, result.plan());
    }

    @Test
    void planningFailuresCrossEventBoundaryWithSafe422Or500() throws Exception {
        for (boolean guardrail : new boolean[] {true, false}) {
            RuntimeException failure = guardrail
                    ? new AgentInvocationException(new IllegalStateException(new OutputGuardrailException("private response <script>secret</script>")))
                    : new AgentInvocationException(new IllegalStateException("private provider token"));
            doThrow(failure).when(adapter).planFromRequest(any());
            int baseline = connector.sink("flow-out").received().size();
            CompletableFuture<Response> response = startPlanning();
            await().atMost(5, SECONDS).until(() -> connector.sink("flow-out").received().size() > baseline);
            Message<String> emitted = connector.<String>sink("flow-out").received().get(baseline);
            assertEquals("com.tripplanner.trip.failed", metadata(emitted).getType());
            relayOutput(emitted);
            Response http = response.get(5, SECONDS);
            http.then().statusCode(guardrail ? 422 : 500)
                    .body("status", equalTo("failed"))
                    .body("error", equalTo(guardrail ? "guardrail_violation" : "planning_failed"))
                    .body("message", not(containsString("private")))
                    .body("message", not(containsString("<script>")));
            TripPlanStatus result = http.as(TripPlanStatus.class);
            assertEquals(metadata(emitted).getExtension("flowinstanceid").orElseThrow(), result.instanceId());
            assertEquals(REQUEST, result.request());
            assertEquals(result, status(result.instanceId()));
            assertNull(result.plan());
            assertNull(result.confirmation());
        }
    }

    @Test
    void finalizationFailurePreservesReviewedPlan() throws Exception {
        for (boolean guardrail : new boolean[] {true, false}) {
            TripPlanStatus planned = plan();
            doThrow(guardrail
                    ? new AgentInvocationException(new OutputGuardrailException("private guardrail data"))
                    : new IllegalStateException("private booking credentials")).when(adapter).finalizeBooking(any());
            submit(planned, "approved");
            relayDecision();
            relayOutput(outcome(planned.instanceId(), "com.tripplanner.trip.failed"));
            await().atMost(5, SECONDS).until(() -> "failed".equals(status(planned.instanceId()).status()));
            TripPlanStatus failed = status(planned.instanceId());
            assertEquals(PLAN, failed.plan());
            assertEquals(REQUEST, failed.request());
            assertEquals(planned.requestId(), failed.requestId());
            assertEquals(guardrail ? "guardrail_violation" : "finalization_failed", failed.error());
            assertTrue(failed.message().contains("simulated booking"));
            assertTrue(failed.message().contains("Your trip plan is still available"));
            assertFalse(failed.message().contains("private"));
            assertNull(failed.confirmation());
            assertEquals(failed, given().get("/trip/plan/latest").as(TripPlanStatus.class));
        }
    }

    @Test
    void nullAdapterResultsPublishFailureInsteadOfWaitingIndefinitely() throws Exception {
        when(adapter.planFromRequest(any())).thenReturn(null);
        CompletableFuture<Response> response = startPlanning();
        Message<String> planningFailure = nextPlanningOutcome();
        assertEquals("com.tripplanner.trip.failed", metadata(planningFailure).getType());
        relayOutput(planningFailure);
        response.get(5, SECONDS).then().statusCode(500).body("error", equalTo("planning_failed"));

        when(adapter.planFromRequest(any())).thenReturn(PLAN);
        when(adapter.finalizeBooking(any())).thenReturn(null);
        TripPlanStatus planned = plan();
        submit(planned, "approved");
        relayDecision();
        relayOutput(outcome(planned.instanceId(), "com.tripplanner.trip.failed"));
        await().atMost(5, SECONDS).until(() -> "failed".equals(status(planned.instanceId()).status()));
        assertEquals(PLAN, status(planned.instanceId()).plan());
        assertNull(status(planned.instanceId()).confirmation());
    }

    @Test
    void failedApprovalPublicationRecordsCorrelatedFailureAndReleasesPlanningLock() throws Exception {
        AtomicReference<CloudEvent> attempted = failPublication("com.tripplanner.trip.approval.requested");
        Response response = startPlanning().get(5, SECONDS);
        response.then().statusCode(500).body("status", equalTo("failed"))
                .body("error", equalTo("planning_failed"))
                .body("message", equalTo("Could not generate the trip plan. Please try again later."))
                .body(not(containsString("private broker credentials")));
        TripPlanStatus failed = response.as(TripPlanStatus.class);
        assertEquals(attempted.get().getExtension("flowinstanceid"), failed.instanceId());
        assertEquals(failed, status(failed.instanceId()));
        assertTrue(connector.sink("flow-out").received().isEmpty(), "No domain outcome was delivered to the store");

        reset(publisher);
        TripPlanStatus next = plan();
        assertNotEquals(failed.requestId(), next.requestId());
        assertEquals("awaiting_approval", next.status());
    }

    @Test
    void failedTerminalPublicationPreservesPlanAndDoesNotClaimConfirmationOrRejection() throws Exception {
        for (String decision : new String[] {"approved", "rejected"}) {
            TripPlanStatus planned = plan();
            String type = decision.equals("approved") ? "com.tripplanner.booking.finalized" : "com.tripplanner.trip.rejected";
            AtomicReference<CloudEvent> attempted = failPublication(type);
            submit(planned, decision);
            relayDecision();
            await().atMost(5, SECONDS).until(() -> "failed".equals(status(planned.instanceId()).status()));
            TripPlanStatus failed = status(planned.instanceId());
            assertEquals(planned.instanceId(), attempted.get().getExtension("flowinstanceid"));
            assertEquals(planned.requestId(), failed.requestId());
            assertEquals(planned.request(), failed.request());
            assertEquals(PLAN, failed.plan());
            assertEquals("finalization_failed", failed.error());
            assertEquals("Could not finalize the simulated booking. Your trip plan is still available.", failed.message());
            assertNull(failed.confirmation());
            assertEquals(failed, given().get("/trip/plan/latest").as(TripPlanStatus.class));
            assertTrue(connector.<String>sink("flow-out").received().stream()
                    .noneMatch(message -> type.equals(metadata(message).getType())));
            reset(publisher);
        }
    }

    @Test
    void failureOutcomePublicationFailureStillFinishesThePlanningRequest() throws Exception {
        doThrow(new AgentInvocationException(new OutputGuardrailException("private model response")))
                .when(adapter).planFromRequest(any());
        AtomicReference<CloudEvent> attempted = failPublication("com.tripplanner.trip.failed");
        Response response = startPlanning().get(5, SECONDS);
        response.then().statusCode(500).body("status", equalTo("failed"))
                .body("error", equalTo("planning_failed"));
        TripPlanStatus failed = response.as(TripPlanStatus.class);
        assertEquals(attempted.get().getExtension("flowinstanceid"), failed.instanceId());
        assertEquals(failed, status(failed.instanceId()));
        assertTrue(connector.sink("flow-out").received().isEmpty());
    }

    @Test
    void planningTimeoutLeavesItsWorkflowRunningAndAllowsAnotherRequest() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        when(adapter.planFromRequest(any())).thenAnswer(invocation -> {
            assertTrue(release.await(10, SECONDS));
            return PLAN;
        });
        CompletableFuture<Response> response = startPlanning();
        TripPlanStatus timedOut;
        CompletableFuture<Response> concurrent;
        try {
            Response http = response.get(5, SECONDS);
            http.then().statusCode(504).body("error", equalTo("planning_timeout"))
                    .body("message", containsString("may still complete"));
            timedOut = http.as(TripPlanStatus.class);
            assertEquals("planning", timedOut.status());
            concurrent = startPlanning();
        } finally {
            release.countDown();
        }
        await().atMost(5, SECONDS).until(() -> connector.sink("flow-out").received().size() == 2);
        for (Message<String> output : connector.<String>sink("flow-out").received()) relayOutput(output);
        TripPlanStatus other = concurrent.get(5, SECONDS).then().statusCode(200)
                .body("status", equalTo("awaiting_approval")).extract().as(TripPlanStatus.class);
        assertNotEquals(timedOut.requestId(), other.requestId());
        assertNotEquals(timedOut.instanceId(), other.instanceId());
        await().atMost(5, SECONDS).until(() -> "awaiting_approval".equals(status(timedOut.instanceId()).status()));
        given().queryParam("requestId", timedOut.requestId()).get("/trip/plan/status").then().statusCode(200)
                .body("instanceId", equalTo(timedOut.instanceId())).body("status", equalTo("awaiting_approval"));
    }

    private TripPlanStatus plan() throws Exception {
        int baseline = connector.sink("flow-out").received().size();
        CompletableFuture<Response> response = startPlanning();
        await().atMost(5, SECONDS).until(() -> connector.sink("flow-out").received().size() > baseline);
        relayOutput(connector.<String>sink("flow-out").received().get(baseline));
        return response.get(5, SECONDS).then().statusCode(200).body("status", equalTo("awaiting_approval"))
                .extract().as(TripPlanStatus.class);
    }

    private AtomicReference<CloudEvent> failPublication(String type) {
        AtomicReference<CloudEvent> attempted = new AtomicReference<>();
        doAnswer(invocation -> {
            attempted.set(invocation.getArgument(0));
            return CompletableFuture.failedFuture(new IllegalStateException("private broker credentials"));
        }).when(publisher).publish(argThat(event -> type.equals(event.getType())));
        return attempted;
    }

    private CompletableFuture<Response> startPlanning() throws Exception {
        int baseline = connector.sink("flow-in-producer").received().size();
        CompletableFuture<Response> response = CompletableFuture.supplyAsync(() ->
                given().contentType("application/json").body(REQUEST).post("/trip/plan"));
        await().atMost(5, SECONDS).until(() -> connector.sink("flow-in-producer").received().size() > baseline);
        Message<?> message = connector.sink("flow-in-producer").received().get(baseline);
        assertEquals("com.tripplanner.trip.requested", metadata(message).getType());
        assertInstanceOf(PlanningRequest.class, message.getPayload());
        connector.<Message<String>>source("flow-in").send(Message.of(objectMapper.writeValueAsString(message.getPayload()), message.getMetadata()));
        return response;
    }

    private void submit(TripPlanStatus trip, String decision) {
        given().contentType("application/json").body(new TripApproval(trip.instanceId(), decision, ""))
                .put("/trip/approve").then().statusCode(202)
                .body("instanceId", equalTo(trip.instanceId())).body("status", equalTo("decision_submitted"));
    }

    private void relayDecision() throws Exception {
        List<? extends Message<?>> messages = connector.sink("flow-in-producer").received();
        Message<?> message = messages.getLast();
        assertEquals("com.tripplanner.trip.approval.done", metadata(message).getType());
        connector.<Message<String>>source("flow-in").send(Message.of(objectMapper.writeValueAsString(message.getPayload()), message.getMetadata()));
    }

    private void relayOutput(Message<String> message) {
        connector.<Message<String>>source("flow-out-consumer").send(Message.of(message.getPayload(), message.getMetadata()));
    }

    private Message<String> nextPlanningOutcome() {
        await().atMost(5, SECONDS).until(() -> !connector.sink("flow-out").received().isEmpty());
        return connector.<String>sink("flow-out").received().getFirst();
    }

    private Message<String> outcome(String instanceId, String type) {
        await().atMost(5, SECONDS).until(() -> connector.<String>sink("flow-out").received().stream().anyMatch(message ->
                type.equals(metadata(message).getType()) && instanceId.equals(metadata(message).getExtension("flowinstanceid").orElse(null))));
        return connector.<String>sink("flow-out").received().stream().filter(message ->
                type.equals(metadata(message).getType()) && instanceId.equals(metadata(message).getExtension("flowinstanceid").orElse(null)))
                .findFirst().orElseThrow();
    }

    private TripPlanStatus status(String instanceId) {
        return given().queryParam("instanceId", instanceId).get("/trip/plan/status").then().statusCode(200)
                .extract().as(TripPlanStatus.class);
    }

    private CloudEventMetadata<?> metadata(Message<?> message) {
        return message.getMetadata(CloudEventMetadata.class).orElseThrow();
    }

    private Message<String> event(String type, String instanceId, Object payload) throws Exception {
        var metadata = OutgoingCloudEventMetadata.<String>builder().withId(UUID.randomUUID().toString())
                .withType(type).withSource(URI.create("test:/trip"))
                .withDataContentType("application/json").withExtension("flowinstanceid", instanceId).build();
        return Message.of(objectMapper.writeValueAsString(payload), Metadata.of(metadata));
    }

    public static class PublicationProfile implements QuarkusTestProfile {
        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(SpyablePublisher.class);
        }

        // The extension publisher is @Dependent; a profile-local normal scope permits a controlled spy.
        @Alternative
        @ApplicationScoped
        public static class SpyablePublisher extends FlowDomainEventsPublisher {}
    }
}
