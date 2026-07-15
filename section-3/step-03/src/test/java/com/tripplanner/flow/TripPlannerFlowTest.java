package com.tripplanner.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.model.TripApproval;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripRequest;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TripPlannerFlowTest {

    @Inject
    @Any
    InMemoryConnector connector;

    @Inject
    ObjectMapper objectMapper;

    @Test
    void fullHitlCycle() throws Exception {
        InMemorySource<byte[]> source = connector.source("flow-in");
        InMemorySink<byte[]> sink = connector.sink("flow-out");

        TripRequest tripRequest = new TripRequest(
                "California Coast", 5, "family", 4, "$3000", "beach and scenic drives");

        byte[] requestBody = objectMapper.writeValueAsBytes(tripRequest);
        CloudEvent bookingEvent = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create("test:/booking"))
                .withType("com.tripplanner.booking.confirmed")
                .withDataContentType("application/json")
                .withData(requestBody)
                .build();

        source.send(serializeCloudEvent(bookingEvent));

        // Phase 1: wait for the approval-requested event
        await().atMost(60, SECONDS).until(() ->
                sink.received().stream().anyMatch(msg -> {
                    CloudEvent ce = deserializeCloudEvent(msg.getPayload());
                    return ce != null && "com.tripplanner.trip.approval.requested".equals(ce.getType());
                }));

        Message<byte[]> approvalRequestMsg = sink.received().stream()
                .filter(msg -> {
                    CloudEvent ce = deserializeCloudEvent(msg.getPayload());
                    return ce != null && "com.tripplanner.trip.approval.requested".equals(ce.getType());
                })
                .findFirst()
                .orElseThrow();

        CloudEvent approvalRequestCe = deserializeCloudEvent(approvalRequestMsg.getPayload());
        String flowInstanceId = (String) approvalRequestCe.getExtension("flowinstanceid");
        assertNotNull(flowInstanceId, "flowinstanceid must be present on the emitted event");

        // Verify the approval-requested event carries a valid TripPlan
        TripPlan requestedPlan = objectMapper.readValue(approvalRequestCe.getData().toBytes(), TripPlan.class);
        assertNotNull(requestedPlan.vehicle(), "approval-requested event must contain a vehicle recommendation");
        assertNotNull(requestedPlan.itinerary(), "approval-requested event must contain an itinerary");

        // Phase 2: send approval and wait for the confirmed event
        TripApproval approval = new TripApproval("approved", "");
        byte[] approvalBody = objectMapper.writeValueAsBytes(approval);
        CloudEvent approvalEvent = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create("test:/approval"))
                .withType("com.tripplanner.trip.approval.done")
                .withDataContentType("application/json")
                .withData(approvalBody)
                .withExtension("flowinstanceid", flowInstanceId)
                .build();

        source.send(serializeCloudEvent(approvalEvent));

        await().atMost(30, SECONDS).until(() ->
                sink.received().stream().anyMatch(msg -> {
                    CloudEvent ce = deserializeCloudEvent(msg.getPayload());
                    return ce != null && "com.tripplanner.trip.confirmed".equals(ce.getType());
                }));

        // Verify the confirmed event carries the same TripPlan (not the TripApproval)
        Message<byte[]> confirmedMsg = sink.received().stream()
                .filter(msg -> {
                    CloudEvent ce = deserializeCloudEvent(msg.getPayload());
                    return ce != null && "com.tripplanner.trip.confirmed".equals(ce.getType());
                })
                .findFirst()
                .orElseThrow();

        CloudEvent confirmedCe = deserializeCloudEvent(confirmedMsg.getPayload());
        TripPlan confirmedPlan = objectMapper.readValue(confirmedCe.getData().toBytes(), TripPlan.class);
        assertNotNull(confirmedPlan.vehicle(), "confirmed event must contain the trip plan, not the approval");
        assertNotNull(confirmedPlan.itinerary(), "confirmed event must preserve the itinerary");
    }

    private byte[] serializeCloudEvent(CloudEvent ce) {
        return EventFormatProvider.getInstance()
                .resolveFormat(JsonFormat.CONTENT_TYPE)
                .serialize(ce);
    }

    private CloudEvent deserializeCloudEvent(byte[] bytes) {
        try {
            return EventFormatProvider.getInstance()
                    .resolveFormat(JsonFormat.CONTENT_TYPE)
                    .deserialize(bytes);
        } catch (Exception e) {
            return null;
        }
    }
}
