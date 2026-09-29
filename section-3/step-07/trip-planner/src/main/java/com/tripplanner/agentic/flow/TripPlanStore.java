package com.tripplanner.agentic.flow;

import java.time.Duration;
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
import com.tripplanner.model.TripPlanEntity;
import com.tripplanner.model.TripPlanStatus;
import com.tripplanner.model.TripRequest;
import io.serverlessworkflow.impl.lifecycle.WorkflowExecutionListener;
import io.serverlessworkflow.impl.lifecycle.WorkflowFailedEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.ce.CloudEventMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;

@ApplicationScoped
public class TripPlanStore implements WorkflowExecutionListener {
    private static final Logger LOG = Logger.getLogger(TripPlanStore.class);

    @Inject
    ObjectMapper objectMapper;

    @Transactional(REQUIRES_NEW)
    public PlanningRequest register(TripRequest request) {
        TripPlanEntity entity = new TripPlanEntity();
        entity.requestId = UUID.randomUUID().toString();
        entity.request = request;
        entity.status = "planning";
        entity.persist();
        return new PlanningRequest(entity.requestId, request);
    }

    // Bind the persisted request before any model work or result publication.
    @Transactional(REQUIRES_NEW)
    public TripPlanStatus bind(PlanningRequest input, String instanceId) {
        TripPlanEntity entity = TripPlanEntity.find("requestId", input.requestId())
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
        if (entity == null || !"planning".equals(entity.status) || entity.instanceId != null
                || !Objects.equals(input.request(), entity.request)) {
            throw new IllegalStateException("Unknown or duplicate planning request");
        }
        entity.instanceId = Objects.requireNonNull(instanceId);
        return toStatus(entity);
    }

    @Incoming("flow-out-consumer")
    @Blocking
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
        // accept() commits before acknowledgement. Database failures escape to messaging's nack path.
        return message.ack();
    }

    @Transactional(REQUIRES_NEW)
    public void accept(String instanceId, String state, TripPlanStatus result) {
        if (result == null || !instanceId.equals(result.instanceId()) || !state.equals(result.status())) return;
        TripPlanEntity entity = TripPlanEntity.find("instanceId", instanceId)
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
        if (entity == null || !entity.requestId.equals(result.requestId())
                || !entity.request.equals(result.request())) return;
        boolean planning = "planning".equals(entity.status);
        if (planning && !state.equals("awaiting_approval") && !state.equals("failed")) return;
        if (!planning && (!"decision_submitted".equals(entity.status) || state.equals("awaiting_approval"))) return;
        if (state.equals("awaiting_approval") && result.plan() == null) return;
        if (state.equals("confirmed") && result.confirmation() == null) return;
        // Finalization cannot replace or discard the plan the customer reviewed.
        entity.status = state;
        if (planning) entity.plan = result.plan();
        entity.confirmation = result.confirmation();
        entity.error = result.error();
        entity.message = result.message();
    }

    @Transactional(REQUIRES_NEW)
    public TripPlanStatus submitDecision(TripApproval approval) {
        TripPlanEntity entity = TripPlanEntity.find("instanceId", approval.instanceId())
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
        if (entity == null) throw error(404, "unknown_trip", "The requested trip was not found.");
        if (!"awaiting_approval".equals(entity.status)) {
            throw error(409, "decision_not_pending", "This trip is not awaiting a decision.");
        }
        entity.status = "decision_submitted";
        entity.acceptedDecision = approval;
        entity.confirmation = null;
        entity.error = null;
        entity.message = null;
        return toStatus(entity);
    }

    @Transactional(REQUIRES_NEW)
    public boolean matchesDecision(String instanceId, TripApproval approval) {
        TripPlanEntity entity = TripPlanEntity.find("instanceId", instanceId).firstResult();
        return entity != null && "decision_submitted".equals(entity.status)
                && approval != null && approval.equals(entity.acceptedDecision);
    }

    @Transactional(REQUIRES_NEW)
    public void submissionFailed(String requestId, Throwable failure) {
        TripPlanEntity entity = TripPlanEntity.find("requestId", requestId)
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
        if (entity == null || !("planning".equals(entity.status) || "awaiting_approval".equals(entity.status)
                || "decision_submitted".equals(entity.status))) return;
        TripPlanStatus failed = toStatus(entity).failed(failure);
        entity.status = failed.status();
        entity.confirmation = null;
        entity.error = failed.error();
        entity.message = failed.message();
    }

    @Override
    public void onWorkflowFailed(WorkflowFailedEvent event) {
        // Publishing the domain outcome can itself fail. Only a terminal engine failure takes this fallback path.
        TripPlanStatus previous = byInstanceId(event.workflowContext().instanceData().id());
        if (previous != null) submissionFailed(previous.requestId(), event.cause());
    }

    public TripPlanStatus awaitPlan(String requestId, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            TripPlanStatus status = byRequestId(requestId);
            if (status != null && !"planning".equals(status.status())) return status;
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) return null;
            // Each read has its own transaction; no connection is held while waiting.
            TimeUnit.NANOSECONDS.sleep(Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(50)));
        }
    }

    @Transactional(REQUIRES_NEW)
    public TripPlanStatus byRequestId(String requestId) {
        return toStatus(TripPlanEntity.find("requestId", requestId).firstResult());
    }

    @Transactional(REQUIRES_NEW)
    public TripPlanStatus byInstanceId(String instanceId) {
        return toStatus(TripPlanEntity.find("instanceId", instanceId).firstResult());
    }

    @Transactional(REQUIRES_NEW)
    public TripPlanStatus latest() {
        // Registration is serialized, so generated IDs give a stable request order, including terminal trips.
        return toStatus(TripPlanEntity.find("order by id desc").firstResult());
    }

    private TripPlanStatus toStatus(TripPlanEntity entity) {
        return entity == null ? null : new TripPlanStatus(entity.requestId, entity.instanceId, entity.request,
                entity.status, entity.plan, entity.confirmation, entity.error, entity.message);
    }

    private static WebApplicationException error(int status, String code, String message) {
        return new WebApplicationException(Response.status(status).entity(new TripError(code, message)).build());
    }
}
