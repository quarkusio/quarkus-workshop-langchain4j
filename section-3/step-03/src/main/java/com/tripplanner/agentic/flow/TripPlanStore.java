package com.tripplanner.agentic.flow;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.model.BookingConfirmation;
import com.tripplanner.model.TripPlan;
import io.smallrye.reactive.messaging.ce.CloudEventMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TripPlanStore {

    private static final String STATUS_AWAITING_APPROVAL = "awaiting_approval";
    private static final String STATUS_CONFIRMED = "confirmed";
    private final ConcurrentMap<String, TripPlanStatus> plansByInstanceId = new ConcurrentHashMap<>();
    private final AtomicReference<String> latestInstanceId = new AtomicReference<>();
    @Inject
    ObjectMapper objectMapper;

    @Incoming("flow-out-consumer")
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

    public TripPlanStatus latest() {
        String instanceId = latestInstanceId.get();
        if (instanceId == null) {
            return null;
        }
        return plansByInstanceId.get(instanceId);
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

    public TripPlanStatus byInstanceId(String instanceId) {
        return plansByInstanceId.get(instanceId);
    }

    private void handleApprovalRequested(String data, String instanceId) {
        try {
            TripPlan plan = objectMapper.readValue(data, TripPlan.class);
            plansByInstanceId.put(instanceId, new TripPlanStatus(instanceId, STATUS_AWAITING_APPROVAL, plan, null));
            latestInstanceId.set(instanceId);
        } catch (Exception e) {
            // skip malformed events
        }
    }

    private void handleBookingFinalized(String data, String instanceId) {
        try {
            BookingConfirmation confirmation = objectMapper.readValue(data, BookingConfirmation.class);
            TripPlanStatus previous = plansByInstanceId.get(instanceId);
            TripPlan plan = previous == null ? null : previous.plan();
            plansByInstanceId.put(instanceId, new TripPlanStatus(instanceId, STATUS_CONFIRMED, plan, confirmation));
            latestInstanceId.set(instanceId);
        } catch (Exception e) {
            // skip malformed events
        }
    }

    public record TripPlanStatus(String instanceId, String status, TripPlan plan, BookingConfirmation confirmation) {
    }
}
