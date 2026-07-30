package com.tripplanner.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.agentic.flow.TripPlannerFlowAdapter;
import com.tripplanner.model.BookingConfirmation;
import com.tripplanner.model.TripApproval;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripRequest;
import com.tripplanner.resource.TripApprovalResource;
import com.tripplanner.resource.TripPlannerResource;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.ce.CloudEventMetadata;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TripPlannerFlowTest {

    @Inject
    @Any
    InMemoryConnector connector;

    @Inject
    TripPlannerResource tripPlannerResource;

    @Inject
    TripApprovalResource tripApprovalResource;

    @InjectMock
    TripPlannerFlowAdapter flowAdapter;

    @Inject
    ObjectMapper objectMapper;

    @BeforeEach
    void setupMocks() {
        TripPlan stubbedPlan = new TripPlan(
                new TripPlan.VehicleRecommendation("SUV", "Miles Roamer X", "Great for coastal drives"),
                "Pacific Coast Highway from LA to San Francisco",
                List.of(new TripPlan.DayItinerary(1, "Arrival", "Settle in and explore Santa Monica", "Santa Monica")),
                new TripPlan.CostEstimate("$120", "$80", "$10", "$200", "$100", "$50", "$560"),
                List.of("Book accommodation in advance"));
        Mockito.when(flowAdapter.planFromRequest(Mockito.any())).thenReturn(stubbedPlan);
        Mockito.when(flowAdapter.finalizeBooking(Mockito.any())).thenCallRealMethod();
    }

    @Test
    void approvePathEmitsBookingFinalized() throws Exception {
        InMemorySink<String> sink = connector.sink("flow-out");
        int baseline = sink.received().size();

        TripRequest tripRequest = new TripRequest(
                "California Coast", "2026-08-15", 5, "family", 4, "$3000", "beach and scenic drives");
        Thread planThread = new Thread(() -> {
            try { tripPlannerResource.planTrip(tripRequest); } catch (Exception ignored) {}
        });
        planThread.setDaemon(true);
        planThread.start();

        // Phase 1: wait for the approval-requested event on flow-out
        await().atMost(30, SECONDS).until(() ->
                sink.received().stream().skip(baseline)
                        .anyMatch(msg -> isType(msg, "com.tripplanner.trip.approval.requested")));

        Message<String> approvalMsg = sink.received().stream()
                .skip(baseline)
                .filter(msg -> isType(msg, "com.tripplanner.trip.approval.requested"))
                .findFirst()
                .orElseThrow();

        CloudEventMetadata<?> approvalMeta = approvalMsg.getMetadata(CloudEventMetadata.class).orElseThrow();
        String flowInstanceId = approvalMeta.<String>getExtension("flowinstanceid").orElse(null);
        assertNotNull(flowInstanceId, "flowinstanceid must be present on the emitted event");

        TripPlan requestedPlan = objectMapper.readValue(approvalMsg.getPayload(), TripPlan.class);
        assertNotNull(requestedPlan.vehicle(), "approval-requested event must contain a vehicle recommendation");
        assertNotNull(requestedPlan.itinerary(), "approval-requested event must contain an itinerary");

        // Phase 2: send approval via the resource and wait for finalized event
        tripApprovalResource.approveTrip(new TripApproval(flowInstanceId, "approved", ""));

        await().atMost(15, SECONDS).until(() ->
                sink.received().stream().skip(baseline)
                        .anyMatch(msg -> isType(msg, "com.tripplanner.booking.finalized")));

        Message<String> finalizedMsg = sink.received().stream()
                .skip(baseline)
                .filter(msg -> isType(msg, "com.tripplanner.booking.finalized"))
                .findFirst()
                .orElseThrow();

        BookingConfirmation confirmation = objectMapper.readValue(finalizedMsg.getPayload(), BookingConfirmation.class);
        assertNotNull(confirmation.bookingReference(), "booking finalization must include a booking reference");
        assertFalse(confirmation.bookingReference().isBlank(), "booking reference must not be blank");
    }

    @Test
    void rejectPathDoesNotEmitBookingFinalized() throws Exception {
        InMemorySink<String> sink = connector.sink("flow-out");
        int baseline = sink.received().size();

        TripRequest tripRequest = new TripRequest(
                "California Coast", "2026-08-15", 5, "family", 4, "$3000", "beach and scenic drives");
        Thread planThread = new Thread(() -> {
            try { tripPlannerResource.planTrip(tripRequest); } catch (Exception ignored) {}
        });
        planThread.setDaemon(true);
        planThread.start();

        await().atMost(30, SECONDS).until(() ->
                sink.received().stream().skip(baseline)
                        .anyMatch(msg -> isType(msg, "com.tripplanner.trip.approval.requested")));

        Message<String> approvalMsg = sink.received().stream()
                .skip(baseline)
                .filter(msg -> isType(msg, "com.tripplanner.trip.approval.requested"))
                .findFirst()
                .orElseThrow();

        CloudEventMetadata<?> approvalMeta = approvalMsg.getMetadata(CloudEventMetadata.class).orElseThrow();
        String flowInstanceId = approvalMeta.<String>getExtension("flowinstanceid").orElse(null);
        assertNotNull(flowInstanceId, "flowinstanceid must be present on the emitted event");

        tripApprovalResource.approveTrip(new TripApproval(flowInstanceId, "rejected", ""));

        await().during(3, SECONDS).atMost(5, SECONDS).until(() ->
                sink.received().stream().skip(baseline)
                        .noneMatch(msg -> isType(msg, "com.tripplanner.booking.finalized")));
    }

    // --- helpers ---

    private boolean isType(Message<String> msg, String type) {
        return msg.getMetadata(CloudEventMetadata.class)
                .map(ce -> type.equals(ce.getType()))
                .orElse(false);
    }
}
