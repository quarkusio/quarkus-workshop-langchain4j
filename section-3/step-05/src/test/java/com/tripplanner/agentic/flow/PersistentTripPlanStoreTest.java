package com.tripplanner.agentic.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.model.BookingConfirmation;
import com.tripplanner.model.PlanningRequest;
import com.tripplanner.model.TripApproval;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripPlanEntity;
import com.tripplanner.model.TripPlanStatus;
import com.tripplanner.model.TripRequest;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.ce.OutgoingCloudEventMetadata;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PersistentTripPlanStoreTest {
    @Inject
    TripPlanStore store;
    @Inject
    ObjectMapper mapper;

    private final TripRequest request = new TripRequest("Coast", "2027-07-10", 5, "family", 4, "economy", "Short drives");
    private final TripPlan plan = new TripPlan(new TripPlan.VehicleRecommendation("MPV", "Family MPV", "Category only"),
            "Local route", List.of(new TripPlan.DayItinerary(1, "Arrival", "Walk around town", "Genoa")),
            new TripPlan.CostEstimate("90", "0", "0", "0", "0", "0", "450"));

    @BeforeEach
    @AfterEach
    void clearTrips() {
        QuarkusTransaction.requiringNew().run(() -> TripPlanEntity.deleteAll());
    }

    @Test
    void qualityFailureSurvivesStoreRecreation() {
        PlanningRequest input = store.register(request);
        store.submissionFailed(input.requestId(), new com.tripplanner.model.TripQualityException());
        TripPlanStatus failed = freshRead(input.requestId());
        assertEquals("failed", failed.status());
        assertEquals("quality_not_met", failed.error());
        assertEquals(com.tripplanner.model.TripQualityException.MESSAGE, failed.message());
        assertNull(failed.plan());
    }

    @Test
    void registrationCommitsOriginalRequestBeforeBindingAndSurvivesStoreRecreation() {
        assertNull(store.latest());
        PlanningRequest input = store.register(request);
        TripPlanStatus persisted = freshRead(input.requestId());
        assertEquals(new TripPlanStatus(input.requestId(), null, request, "planning", null, null, null, null), persisted);
        assertEquals(persisted, store.latest());
        assertNull(store.byInstanceId("unknown"));
        assertNull(store.byRequestId("unknown"));
        QuarkusTransaction.requiringNew().run(() -> {
            Object destination = TripPlanEntity.getEntityManager().createNativeQuery(
                    "select request ->> 'destination' from trip_plan_status where requestId = :id")
                    .setParameter("id", input.requestId()).getSingleResult();
            assertEquals(request.destination(), destination);
        });
        String instanceId = UUID.randomUUID().toString();
        TripPlanStatus bound = store.bind(input, instanceId);
        assertEquals(bound, freshRead(input.requestId()));
        assertEquals(bound, freshInstanceRead(instanceId));
        assertEquals(request, bound.request());
    }

    @Test
    void bindingRejectsUnknownMismatchedAndDuplicateRequestsWithoutChangingTheRow() {
        PlanningRequest input = store.register(request);
        assertThrows(IllegalStateException.class, () -> store.bind(new PlanningRequest("unknown", request), "one"));
        TripRequest changed = new TripRequest("Different", "2027-07-10", 5, "family", 4, "economy", "");
        assertThrows(IllegalStateException.class, () -> store.bind(new PlanningRequest(input.requestId(), changed), "one"));
        assertNull(freshRead(input.requestId()).instanceId());
        TripPlanStatus bound = store.bind(input, "one");
        assertThrows(IllegalStateException.class, () -> store.bind(input, "two"));
        assertEquals(bound, freshRead(input.requestId()));
        assertNull(store.byInstanceId("two"));
    }

    @Test
    void concurrentRegistrationsPersistIndependentRequests() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<CompletableFuture<PlanningRequest>> attempts = IntStream.range(0, 6).mapToObj(index ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        assertTrue(start.await(5, SECONDS));
                        return store.register(new TripRequest("Destination " + index, "2027-07-10",
                                5, "family", index + 1, "economy", ""));
                    } catch (InterruptedException interrupted) {
                        throw new RuntimeException(interrupted);
                    }
                })).toList();
        start.countDown();
        List<PlanningRequest> results = attempts.stream().map(future -> {
            try { return future.get(10, SECONDS); }
            catch (Exception failure) { throw new RuntimeException(failure); }
        }).toList();
        assertEquals(6, results.stream().map(PlanningRequest::requestId).distinct().count());
        for (PlanningRequest result : results) {
            var persisted = freshRead(result.requestId());
            assertEquals(result.request(), persisted.request());
            assertEquals("planning", persisted.status());
        }
        store.submissionFailed(results.getFirst().requestId(), new IllegalStateException("private token"));
        assertEquals("failed", freshRead(results.getFirst().requestId()).status());
        assertEquals("planning", freshRead(results.getLast().requestId()).status());
    }

    @Test
    void timeoutDoesNotFailPlanningAndReadsDoNotHoldTheWriterTransaction() throws Exception {
        PlanningRequest input = store.register(request);
        assertNull(store.awaitPlan(input.requestId(), Duration.ofMillis(10)));
        assertEquals("planning", freshRead(input.requestId()).status());
        var concurrent = store.register(request);
        var waiter = CompletableFuture.supplyAsync(() -> {
            try { return store.awaitPlan(input.requestId(), Duration.ofSeconds(5)); }
            catch (InterruptedException interrupted) { throw new RuntimeException(interrupted); }
        });
        store.submissionFailed(input.requestId(), new IllegalStateException("private token"));
        assertEquals("failed", waiter.get(5, SECONDS).status());
        assertEquals(freshRead(input.requestId()), waiter.get());
        assertEquals("planning", freshRead(concurrent.requestId()).status());
    }

    @Test
    void approvalAndAcceptedDecisionSurviveNewStoresAndRejectDuplicateOrContradictoryDecisions() throws Exception {
        TripPlanStatus pending = awaitingApproval();
        assertEquals(pending, freshRead(pending.requestId()));
        TripApproval approval = new TripApproval(pending.instanceId(), "approved", "Original feedback");
        assertFalse(store.matchesDecision(pending.instanceId(), approval));
        TripPlanStatus submitted = store.submitDecision(approval);
        assertEquals("decision_submitted", freshRead(pending.requestId()).status());
        assertEquals(submitted, freshInstanceRead(pending.instanceId()));
        QuarkusTransaction.requiringNew().run(() -> {
            TripPlanStore recreated = new TripPlanStore();
            assertTrue(recreated.matchesDecision(pending.instanceId(), approval));
            assertFalse(recreated.matchesDecision(pending.instanceId(), new TripApproval(pending.instanceId(), "rejected", "Original feedback")));
            assertFalse(recreated.matchesDecision(pending.instanceId(), new TripApproval(pending.instanceId(), "approved", "Changed feedback")));
            assertFalse(recreated.matchesDecision("unknown", approval));
            assertFalse(recreated.matchesDecision(pending.instanceId(), null));
            TripPlanEntity row = TripPlanEntity.find("requestId", pending.requestId()).firstResult();
            assertEquals(approval, row.acceptedDecision);
        });
        assertThrows(WebApplicationException.class, () -> store.submitDecision(approval));
        assertThrows(WebApplicationException.class, () -> store.submitDecision(new TripApproval("unknown", "approved", "")));
        assertEquals(submitted, freshRead(pending.requestId()));
    }

    @Test
    void everyTerminalStatePersistsAndReplaysCannotReplaceItOrItsReviewedPlan() throws Exception {
        for (String terminal : List.of("confirmed", "rejected", "failed")) {
            TripPlanStatus pending = awaitingApproval();
            TripApproval decision = new TripApproval(pending.instanceId(), terminal.equals("rejected") ? "rejected" : "approved", "");
            store.submitDecision(decision);
            TripPlanStatus outcome = terminal.equals("failed") ? pending.failed(new IllegalStateException("private credentials"))
                    : pending.outcome(terminal, plan, terminal.equals("confirmed") ? new BookingConfirmation("SIM-1", "Simulated") : null);
            // Even a terminal event carrying a different plan cannot replace the reviewed one.
            deliver(pending.instanceId(), new TripPlanStatus(outcome.requestId(), outcome.instanceId(), outcome.request(),
                    outcome.status(), null, outcome.confirmation(), outcome.error(), outcome.message()));
            assertEquals(outcome, freshInstanceRead(pending.instanceId()));
            assertEquals(outcome, store.latest());
            deliver(pending.instanceId(), outcome);
            deliver(pending.instanceId(), pending);
            deliver(pending.instanceId(), pending.outcome("confirmed", plan, new BookingConfirmation("REPLAY", "Wrong")));
            store.submissionFailed(pending.requestId(), new IllegalStateException("late failure"));
            assertEquals(outcome, freshRead(pending.requestId()));
            assertFalse(store.matchesDecision(pending.instanceId(), decision));
            QuarkusTransaction.requiringNew().run(() -> {
                TripPlanEntity row = TripPlanEntity.find("requestId", pending.requestId()).firstResult();
                assertEquals(decision, row.acceptedDecision);
            });
        }
    }

    @Test
    void planningFailureBeforeOrAfterBindingPersistsSafeError() throws Exception {
        for (boolean bind : List.of(false, true)) {
            PlanningRequest input = store.register(request);
            if (bind) store.bind(input, UUID.randomUUID().toString());
            store.submissionFailed(input.requestId(), new IllegalStateException("private provider credentials"));
            TripPlanStatus failed = freshRead(input.requestId());
            assertEquals("failed", failed.status());
            assertEquals("planning_failed", failed.error());
            assertEquals("Could not generate the trip plan. Please try again later.", failed.message());
            assertEquals(request, failed.request());
            assertNull(failed.plan());
            assertNull(failed.confirmation());
            assertEquals(bind, failed.instanceId() != null);
            assertEquals(failed, store.latest());
        }
        assertDoesNotThrow(() -> store.register(request));
    }

    @Test
    void latestUsesRegistrationOrderNotStatusOrLastUpdateTime() throws Exception {
        TripPlanStatus first = awaitingApproval();
        TripPlanStatus second = awaitingApproval();
        store.submitDecision(new TripApproval(second.instanceId(), "rejected", ""));
        TripPlanStatus rejected = second.outcome("rejected", plan, null);
        deliver(second.instanceId(), rejected);
        assertEquals(rejected, store.latest());
        store.submissionFailed(first.requestId(), new IllegalStateException("late failure of older request"));
        assertEquals(rejected, QuarkusTransaction.requiringNew().call(() -> new TripPlanStore().latest()));
        assertEquals("failed", freshRead(first.requestId()).status());
        PlanningRequest newest = store.register(request);
        assertEquals(newest.requestId(), store.latest().requestId());
        assertEquals("planning", store.latest().status());
    }

    @Test
    void mismatchedIdentityAndIllegalTransitionsAreIgnored() throws Exception {
        PlanningRequest input = store.register(request);
        TripPlanStatus planning = store.bind(input, "expected-instance");
        TripPlanStatus pending = planning.outcome("awaiting_approval", plan, null);
        deliver("unrelated-instance", pending);
        deliver(planning.instanceId(), new TripPlanStatus("unrelated-request", planning.instanceId(), request,
                "awaiting_approval", plan, null, null, null));
        deliver(planning.instanceId(), new TripPlanStatus(input.requestId(), planning.instanceId(), null,
                "awaiting_approval", plan, null, null, null));
        deliver(planning.instanceId(), planning.outcome("awaiting_approval", null, null));
        deliver(planning.instanceId(), planning.outcome("confirmed", plan, new BookingConfirmation("TOO-EARLY", "Wrong")));
        deliver(planning.instanceId(), planning.outcome("rejected", plan, null));
        store.consume(event("com.tripplanner.trip.rejected", planning.instanceId(), mapper.writeValueAsString(pending)))
                .toCompletableFuture().join();
        assertEquals(planning, freshRead(input.requestId()));
        deliver(planning.instanceId(), pending);
        deliver(planning.instanceId(), pending.outcome("rejected", plan, null));
        assertEquals(pending, freshRead(input.requestId()));
        TripPlanStatus submitted = store.submitDecision(new TripApproval(planning.instanceId(), "approved", ""));
        deliver(planning.instanceId(), pending);
        deliver(planning.instanceId(), pending.outcome("confirmed", plan, null));
        assertEquals(submitted, freshRead(input.requestId()));
    }

    @Test
    void malformedEventsAreAcknowledgedButDatabaseFailuresAreNot() throws Exception {
        PlanningRequest input = store.register(request);
        TripPlanStatus planning = store.bind(input, "expected-instance");
        AtomicBoolean acknowledged = new AtomicBoolean();
        var malformed = event("com.tripplanner.trip.approval.requested", planning.instanceId(), "not-json")
                .withAck(() -> { acknowledged.set(true); return CompletableFuture.completedFuture(null); });
        store.consume(malformed).toCompletableFuture().join();
        assertTrue(acknowledged.get());
        assertEquals(planning, freshRead(input.requestId()));
        QuarkusTransaction.requiringNew().run(() -> TripPlanEntity.getEntityManager().createNativeQuery(
                "alter table trip_plan_status add constraint test_reject_outcome check (status <> 'awaiting_approval')").executeUpdate());
        try {
            acknowledged.set(false);
            var valid = event("com.tripplanner.trip.approval.requested", planning.instanceId(),
                    mapper.writeValueAsString(planning.outcome("awaiting_approval", plan, null)))
                    .withAck(() -> { acknowledged.set(true); return CompletableFuture.completedFuture(null); });
            assertThrows(RuntimeException.class, () -> store.consume(valid).toCompletableFuture().join());
            assertFalse(acknowledged.get(), "A failed database commit must not acknowledge the event");
            assertEquals(planning, freshRead(input.requestId()));
        } finally {
            QuarkusTransaction.requiringNew().run(() -> TripPlanEntity.getEntityManager().createNativeQuery(
                    "alter table trip_plan_status drop constraint test_reject_outcome").executeUpdate());
        }
        deliver(planning.instanceId(), planning.outcome("awaiting_approval", plan, null));
        assertEquals("awaiting_approval", freshRead(input.requestId()).status());
    }

    private TripPlanStatus freshRead(String requestId) {
        // A new store and transaction force a database fetch, not an in-memory map or managed-entity assertion.
        return QuarkusTransaction.requiringNew().call(() -> new TripPlanStore().byRequestId(requestId));
    }

    private TripPlanStatus freshInstanceRead(String instanceId) {
        return QuarkusTransaction.requiringNew().call(() -> new TripPlanStore().byInstanceId(instanceId));
    }

    private TripPlanStatus awaitingApproval() throws Exception {
        PlanningRequest input = store.register(request);
        TripPlanStatus pending = store.bind(input, UUID.randomUUID().toString()).outcome("awaiting_approval", plan, null);
        deliver(pending.instanceId(), pending);
        return pending;
    }

    private void deliver(String instanceId, TripPlanStatus status) throws Exception {
        String type = switch (status.status()) {
            case "awaiting_approval" -> "com.tripplanner.trip.approval.requested";
            case "confirmed" -> "com.tripplanner.booking.finalized";
            case "rejected" -> "com.tripplanner.trip.rejected";
            default -> "com.tripplanner.trip.failed";
        };
        store.consume(event(type, instanceId, mapper.writeValueAsString(status))).toCompletableFuture().join();
    }

    private Message<String> event(String type, String instanceId, String payload) {
        var metadata = OutgoingCloudEventMetadata.<String>builder().withId(UUID.randomUUID().toString())
                .withSource(URI.create("test:/trip")).withType(type).withExtension("flowinstanceid", instanceId).build();
        return Message.of(payload, Metadata.of(metadata));
    }
}
