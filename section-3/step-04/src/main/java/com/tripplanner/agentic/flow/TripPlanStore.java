package com.tripplanner.agentic.flow;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.model.PlanningRequest;
import com.tripplanner.model.TripApproval;
import com.tripplanner.model.TripError;
import com.tripplanner.model.TripPlanStatus;
import com.tripplanner.model.TripRequest;
import io.serverlessworkflow.impl.lifecycle.WorkflowExecutionListener;
import io.serverlessworkflow.impl.lifecycle.WorkflowFailedEvent;
import io.smallrye.reactive.messaging.ce.CloudEventMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class TripPlanStore implements WorkflowExecutionListener {
    private static final Logger LOG = Logger.getLogger(TripPlanStore.class);
    private final Map<String, TripPlanStatus> requests = new HashMap<>();
    private final Map<String, String> instances = new HashMap<>();
    private final Map<String, TripApproval> decisions = new HashMap<>();
    private String latestRequestId;

    @Inject
    ObjectMapper objectMapper;

    public synchronized PlanningRequest register(TripRequest request) {
        String requestId = UUID.randomUUID().toString();
        requests.put(requestId, new TripPlanStatus(requestId, null, request, "planning", null, null, null, null));
        latestRequestId = requestId;
        return new PlanningRequest(requestId, request);
    }

    // Bind the request to the actual Flow instance before any model work or result publication.
    public synchronized TripPlanStatus bind(PlanningRequest input, String instanceId) {
        TripPlanStatus status = requests.get(input.requestId());
        if (status == null || !"planning".equals(status.status()) || status.instanceId() != null
                || !Objects.equals(input.request(), status.request())) {
            throw new IllegalStateException("Unknown or duplicate planning request");
        }
        TripPlanStatus bound = new TripPlanStatus(status.requestId(), instanceId, status.request(),
                "planning", null, null, null, null);
        requests.put(input.requestId(), bound);
        instances.put(instanceId, input.requestId());
        return bound;
    }

    @Incoming("flow-out-consumer")
    public CompletionStage<Void> consume(Message<String> message) {
        CloudEventMetadata<?> event = message.getMetadata(CloudEventMetadata.class).orElse(null);
        if (event == null) return message.ack();
        String instanceId = event.<String>getExtension("flowinstanceid").orElse(null);
        String state = switch (event.getType()) {
            case "com.tripplanner.trip.approval.requested" -> "awaiting_approval";
            case "com.tripplanner.booking.finalized" -> "confirmed";
            case "com.tripplanner.trip.rejected" -> "rejected";
            case "com.tripplanner.trip.failed" -> "failed";
            default -> null;
        };
        if (instanceId == null || state == null) return message.ack();
        try {
            accept(instanceId, state, objectMapper.readValue(message.getPayload(), TripPlanStatus.class));
        } catch (JsonProcessingException e) {
            LOG.warn("Ignoring malformed trip outcome event");
        }
        return message.ack();
    }

    private synchronized void accept(String instanceId, String state, TripPlanStatus result) {
        if (result == null || !instanceId.equals(result.instanceId()) || !state.equals(result.status())) return;
        TripPlanStatus previous = byInstanceId(instanceId);
        if (previous == null || !previous.requestId().equals(result.requestId())
                || !previous.request().equals(result.request())) return;
        boolean planning = "planning".equals(previous.status());
        if (planning && !state.equals("awaiting_approval") && !state.equals("failed")) return;
        if (!planning && (!"decision_submitted".equals(previous.status()) || state.equals("awaiting_approval"))) return;
        if (state.equals("awaiting_approval") && result.plan() == null) return;
        if (state.equals("confirmed") && result.confirmation() == null) return;
        // Finalization cannot replace or discard the plan the customer reviewed.
        requests.put(previous.requestId(), new TripPlanStatus(previous.requestId(), instanceId, previous.request(), state,
                planning ? result.plan() : previous.plan(), result.confirmation(), result.error(), result.message()));
        if (!planning) decisions.remove(instanceId);
        notifyAll();
    }

    public synchronized TripPlanStatus submitDecision(TripApproval approval) {
        String instanceId = approval.instanceId();
        TripPlanStatus previous = byInstanceId(instanceId);
        if (previous == null) throw error(404, "unknown_trip", "The requested trip was not found.");
        if (!"awaiting_approval".equals(previous.status())) {
            throw error(409, "decision_not_pending", "This trip is not awaiting a decision.");
        }
        TripPlanStatus submitted = previous.outcome("decision_submitted", previous.plan(), null);
        requests.put(previous.requestId(), submitted);
        decisions.put(instanceId, approval);
        return submitted;
    }

    public synchronized boolean matchesDecision(String instanceId, TripApproval approval) {
        TripPlanStatus status = byInstanceId(instanceId);
        return status != null && "decision_submitted".equals(status.status())
                && approval != null && approval.equals(decisions.get(instanceId));
    }

    public synchronized void submissionFailed(String requestId, Throwable failure) {
        TripPlanStatus previous = requests.get(requestId);
        if (previous == null || !("planning".equals(previous.status()) || "awaiting_approval".equals(previous.status())
                || "decision_submitted".equals(previous.status()))) return;
        requests.put(requestId, previous.failed(failure));
        decisions.remove(previous.instanceId());
        notifyAll();
    }

    @Override
    public synchronized void onWorkflowFailed(WorkflowFailedEvent event) {
        // Publishing the domain outcome can itself fail. Only a terminal engine failure takes this fallback path.
        TripPlanStatus previous = byInstanceId(event.workflowContext().instanceData().id());
        if (previous != null) {
            submissionFailed(previous.requestId(), event.cause());
        }
    }

    public synchronized TripPlanStatus awaitPlan(String requestId, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            TripPlanStatus status = requests.get(requestId);
            if (status != null && !"planning".equals(status.status())) return status;
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) return null;
            TimeUnit.NANOSECONDS.timedWait(this, remaining);
        }
    }

    public synchronized TripPlanStatus byRequestId(String requestId) {
        return requests.get(requestId);
    }

    public synchronized TripPlanStatus byInstanceId(String instanceId) {
        return requests.get(instances.get(instanceId));
    }

    public synchronized TripPlanStatus latest() {
        return requests.get(latestRequestId);
    }

    private static WebApplicationException error(int status, String code, String message) {
        return new WebApplicationException(Response.status(status).entity(new TripError(code, message)).build());
    }
}
