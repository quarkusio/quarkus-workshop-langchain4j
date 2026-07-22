package com.tripplanner.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.model.TripApproval;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonFormat;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.net.URI;
import java.util.UUID;

@Path("/trip")
public class TripApprovalResource {

    @Inject
    ObjectMapper objectMapper;

    @Channel("flow-in-producer")
    Emitter<byte[]> flowIn;

    @PUT
    @Path("/approve")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response approveTrip(TripApproval approval) throws Exception {
        if (approval.instanceId() == null || approval.instanceId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("instanceId is required")
                    .build();
        }

        byte[] body = objectMapper.writeValueAsBytes(approval);

        CloudEvent ce = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create("api:/trip/approve"))
                .withType("com.tripplanner.trip.approval.done")
                .withDataContentType("application/json")
                .withData(body)
                .withExtension("flowinstanceid", approval.instanceId())
                .build();

        flowIn.send(new JsonFormat().serialize(ce));
        return Response.accepted().build();
    }
}
