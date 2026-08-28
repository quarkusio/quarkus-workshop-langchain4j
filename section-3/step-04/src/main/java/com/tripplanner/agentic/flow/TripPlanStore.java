package com.tripplanner.agentic.flow;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.model.BookingConfirmation;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripRequest;
import com.tripplanner.model.TripRequestContext;
import io.smallrye.reactive.messaging.ce.CloudEventMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TripPlanStore {

    private static final String STATUS_AWAITING_APPROVAL = "awaiting_approval";
    private static final String STATUS_CONFIRMED = "confirmed";

    @Inject
    ObjectMapper objectMapper;

    @Inject
    TripRequestContext tripRequestContext;

    @Incoming("flow-out-consumer")
    @Transactional
    public CompletionStage<Void> consume(Message<String> message) {
        CloudEventMetadata<?> event = message.getMetadata(CloudEventMetadata.class).orElse(null);
        if (event == null) return message.ack();

        String instanceId = (String) event.getExtension("flowinstanceid").orElse(null);
        if (instanceId == null || instanceId.isBlank()) return message.ack();

        String data = message.getPayload();
        switch (event.getType()) {
            case "com.tripplanner.trip.approval.requested" -> handleApprovalRequested(data, instanceId);
            case "com.tripplanner.booking.finalized" -> handleBookingFinalized(data, instanceId);
            default -> {
            }
        }
        return message.ack();
    }

    @Transactional
    public TripPlanStatus latest() {
        TripPlanEntity entity = TripPlanEntity.findLatestAwaiting();
        return entity == null ? null : toStatus(entity);
    }

    /**
     * Blocks until a new plan appears (one whose instanceId differs from {@code previousId}).
     * Returns null on timeout.
     */
    public TripPlanStatus awaitNextPlan(String previousId, long timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000;
        while (System.currentTimeMillis() < deadline) {
            TripPlanStatus status = latest();
            if (status != null && !status.instanceId().equals(previousId)) {
                return status;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    @Transactional
    public TripPlanStatus byInstanceId(String instanceId) {
        TripPlanEntity entity = TripPlanEntity.findByInstanceId(instanceId);
        return entity == null ? null : toStatus(entity);
    }

    private void handleApprovalRequested(String data, String instanceId) {
        try {
            TripPlan plan = objectMapper.readValue(data, TripPlan.class);
            TripPlanEntity existing = TripPlanEntity.findByInstanceId(instanceId);
            if (existing == null) {
                existing = new TripPlanEntity();
                existing.instanceId = instanceId;
            }
            existing.status = STATUS_AWAITING_APPROVAL;
            existing.planJson = objectMapper.writeValueAsString(plan);
            existing.confirmationJson = null;
            // Persist the originating request so the UI title survives a restart
            TripRequest req = tripRequestContext.get();
            existing.requestJson = req != null ? objectMapper.writeValueAsString(req) : null;
            existing.persist();
        } catch (Exception e) {
            // skip malformed events
        }
    }

    private void handleBookingFinalized(String data, String instanceId) {
        try {
            BookingConfirmation confirmation = objectMapper.readValue(data, BookingConfirmation.class);
            TripPlanEntity entity = TripPlanEntity.findByInstanceId(instanceId);
            if (entity == null) return;
            entity.status = STATUS_CONFIRMED;
            entity.confirmationJson = objectMapper.writeValueAsString(confirmation);
            entity.persist();
        } catch (Exception e) {
            // skip malformed events
        }
    }

    private TripPlanStatus toStatus(TripPlanEntity entity) {
        try {
            TripPlan plan = entity.planJson != null
                    ? objectMapper.readValue(entity.planJson, TripPlan.class) : null;
            BookingConfirmation confirmation = entity.confirmationJson != null
                    ? objectMapper.readValue(entity.confirmationJson, BookingConfirmation.class) : null;
            TripRequest request = entity.requestJson != null
                    ? objectMapper.readValue(entity.requestJson, TripRequest.class) : null;
            return new TripPlanStatus(entity.instanceId, entity.status, plan, confirmation, request);
        } catch (Exception e) {
            return null;
        }
    }

    public record TripPlanStatus(String instanceId, String status, TripPlan plan, BookingConfirmation confirmation, TripRequest request) {
    }
}
