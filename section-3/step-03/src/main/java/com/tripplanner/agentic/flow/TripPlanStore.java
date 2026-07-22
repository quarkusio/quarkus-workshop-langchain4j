package com.tripplanner.agentic.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.model.BookingConfirmation;
import com.tripplanner.model.TripPlan;
import io.cloudevents.CloudEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class TripPlanStore {

    private static final String STATUS_AWAITING_APPROVAL = "awaiting_approval";
    private static final String STATUS_CONFIRMED = "confirmed";

    @Inject
    ObjectMapper objectMapper;

    private final ConcurrentMap<String, TripPlanStatus> plansByInstanceId = new ConcurrentHashMap<>();
    private final AtomicReference<String> latestInstanceId = new AtomicReference<>();

    @Incoming("flow-out-consumer")
    public void consume(CloudEvent event) {
        if (event == null) {
            return;
        }

        String instanceId = (String) event.getExtension("flowinstanceid");
        if (instanceId == null || instanceId.isBlank()) {
            return;
        }

        switch (event.getType()) {
            case "com.tripplanner.trip.approval.requested" -> handleApprovalRequested(event, instanceId);
            case "com.tripplanner.booking.finalized" -> handleBookingFinalized(event, instanceId);
            default -> {
            }
        }
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

    private void handleApprovalRequested(CloudEvent event, String instanceId) {
        try {
            TripPlan plan = objectMapper.readValue(event.getData().toBytes(), TripPlan.class);
            plansByInstanceId.put(instanceId, new TripPlanStatus(instanceId, STATUS_AWAITING_APPROVAL, plan, null));
            latestInstanceId.set(instanceId);
        } catch (Exception e) {
            // skip malformed events
        }
    }

    private void handleBookingFinalized(CloudEvent event, String instanceId) {
        try {
            BookingConfirmation confirmation = objectMapper.readValue(event.getData().toBytes(), BookingConfirmation.class);
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
