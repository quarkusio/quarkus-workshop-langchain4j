package com.tripplanner.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.model.TripApproval;
import io.smallrye.reactive.messaging.ce.OutgoingCloudEventMetadata;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;

import java.net.URI;
import java.util.UUID;

@Path("/trip")
public class TripApprovalResource {

    @Inject
    ObjectMapper objectMapper;

    @Channel("flow-in-producer")
    Emitter<String> flowIn;

    @PUT
    @Path("/approve")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response approveTrip(TripApproval approval) throws Exception {
        if (approval.instanceId() == null || approval.instanceId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("instanceId is required")
                    .build();
        }

        String body = objectMapper.writeValueAsString(approval);

        OutgoingCloudEventMetadata<String> metadata = OutgoingCloudEventMetadata.<String>builder()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create("api:/trip/approve"))
                .withType("com.tripplanner.trip.approval.done")
                .withDataContentType("application/json")
                .withExtension("flowinstanceid", approval.instanceId())
                .build();

        flowIn.send(Message.of(body, Metadata.of(metadata)));
        return Response.accepted().build();
    }
}
