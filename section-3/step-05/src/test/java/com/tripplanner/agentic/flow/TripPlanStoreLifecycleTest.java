package com.tripplanner.agentic.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.model.BookingConfirmation;
import com.tripplanner.model.TripApproval;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripPlanStatus;
import com.tripplanner.model.TripRequest;
import io.serverlessworkflow.impl.WorkflowContextData;
import io.serverlessworkflow.impl.WorkflowInstanceData;
import io.serverlessworkflow.impl.lifecycle.TaskFailedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowCompletedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowFailedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowSuspendedEvent;
import io.smallrye.reactive.messaging.ce.OutgoingCloudEventMetadata;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.narayana.jta.QuarkusTransaction;
import com.tripplanner.model.TripPlanEntity;
import jakarta.inject.Inject;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class TripPlanStoreLifecycleTest {
    @Inject
    TripPlanStore store;
    private final TripRequest request = new TripRequest("Coast", "2027-07-10", 5, "family", 4, "economy", "");
    private final TripPlan plan = new TripPlan(null, "Local route", List.of(), null);

    @BeforeEach
    void clearTrips() {
        QuarkusTransaction.requiringNew().run(() -> TripPlanEntity.deleteAll());
    }

    @Test
    void unrelatedAndNonterminalLifecycleEventsDoNotFailPlanning() throws Exception {
        var input = store.register(request);
        var pending = store.bind(input, "expected-instance");
        var context = context(pending.instanceId());
        store.onTaskFailed(mock(TaskFailedEvent.class));
        store.onWorkflowSuspended(new WorkflowSuspendedEvent(context));
        store.onWorkflowCompleted(new WorkflowCompletedEvent(context, null));
        store.onWorkflowFailed(new WorkflowFailedEvent(context("another-instance"), new IllegalStateException("private")));
        assertEquals(pending, store.byRequestId(input.requestId()));
        assertNull(store.awaitPlan(input.requestId(), Duration.ofMillis(1)));
        var concurrent = store.register(request);

        store.onWorkflowFailed(new WorkflowFailedEvent(context, new IllegalStateException("private")));
        assertEquals("failed", store.awaitPlan(input.requestId(), Duration.ofMillis(1)).status());
        assertEquals("planning", store.byRequestId(concurrent.requestId()).status());
    }

    @Test
    void actualWorkflowFailureWhileAwaitingApprovalRetainsThePlan() throws Exception {
        var pending = awaitingApproval();
        store.onWorkflowFailed(new WorkflowFailedEvent(context(pending.instanceId()), new IllegalStateException("private")));
        var failed = store.byInstanceId(pending.instanceId());
        assertEquals("failed", failed.status());
        assertEquals(plan, failed.plan());
        assertEquals(request, failed.request());
        assertEquals("finalization_failed", failed.error());
        assertNull(failed.confirmation());
    }

    @Test
    void duplicateFailureDoesNotOverwriteAnExistingTerminalOutcomeOrAnotherRequest() throws Exception {
        for (String state : new String[] {"confirmed", "rejected", "failed"}) {
            var pending = awaitingApproval();
            store.submitDecision(new TripApproval(pending.instanceId(), "approved", ""));
            var result = state.equals("failed") ? pending.failed(new IllegalStateException("original"))
                    : pending.outcome(state, plan, state.equals("confirmed") ? new BookingConfirmation("SIMULATED", "Simulation") : null);
            String type = switch (state) {
                case "confirmed" -> "com.tripplanner.booking.finalized";
                case "rejected" -> "com.tripplanner.trip.rejected";
                default -> "com.tripplanner.trip.failed";
            };
            deliver(type, result);
            var next = store.register(request);
            store.onWorkflowFailed(new WorkflowFailedEvent(context(pending.instanceId()), new IllegalStateException("late")));
            assertEquals(result, store.byInstanceId(pending.instanceId()));
            assertEquals("planning", store.byRequestId(next.requestId()).status());
            store.submissionFailed(next.requestId(), new IllegalStateException("cleanup"));
        }
    }

    private TripPlanStatus awaitingApproval() throws Exception {
        var input = store.register(request);
        var pending = store.bind(input, "instance-" + input.requestId()).outcome("awaiting_approval", plan, null);
        deliver("com.tripplanner.trip.approval.requested", pending);
        return pending;
    }

    private void deliver(String type, TripPlanStatus status) throws Exception {
        store.objectMapper = new ObjectMapper();
        var metadata = OutgoingCloudEventMetadata.<String>builder().withId(status.requestId()).withSource(URI.create("test:/trip"))
                .withType(type).withExtension("flowinstanceid", status.instanceId()).build();
        store.consume(Message.of(store.objectMapper.writeValueAsString(status), Metadata.of(metadata))).toCompletableFuture().join();
    }

    private WorkflowContextData context(String instanceId) {
        var context = mock(WorkflowContextData.class);
        var instance = mock(WorkflowInstanceData.class);
        when(instance.id()).thenReturn(instanceId);
        when(context.instanceData()).thenReturn(instance);
        return context;
    }
}
