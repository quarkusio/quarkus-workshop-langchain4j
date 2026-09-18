package com.tripplanner.resource;

import com.tripplanner.model.TripApproval;
import com.tripplanner.model.TripError;
import com.tripplanner.model.TripPlanStatus;
import com.tripplanner.agentic.flow.TripPlanStore;
import io.smallrye.reactive.messaging.ce.OutgoingCloudEventMetadata;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Path("/trip")
public class TripApprovalResource {

    @Inject
    TripPlanStore store;

    @Channel("flow-in-producer")
    Emitter<TripApproval> flowIn;

    @PUT
    @Path("/approve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response approveTrip(TripApproval approval) {
        if (approval == null || approval.instanceId() == null || approval.instanceId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new TripError("invalid_decision", "instanceId is required."))
                    .build();
        }
        if (!"approved".equals(approval.status()) && !"rejected".equals(approval.status())) {
            return Response.status(400).entity(new TripError("invalid_decision", "status must be approved or rejected.")).build();
        }
        TripPlanStatus submitted = store.submitDecision(approval);

        OutgoingCloudEventMetadata<TripApproval> metadata = OutgoingCloudEventMetadata.<TripApproval>builder()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create("api:/trip/approve"))
                .withType("com.tripplanner.trip.approval.done")
                .withDataContentType("application/json")
                .withExtension("flowinstanceid", approval.instanceId())
                .build();

        try {
            flowIn.send(Message.of(approval, Metadata.of(metadata)).withNack(failure -> {
                store.submissionFailed(submitted.requestId(), failure);
                return CompletableFuture.completedFuture(null);
            }));
        } catch (RuntimeException failure) {
            store.submissionFailed(submitted.requestId(), failure);
            return Response.serverError().entity(store.byInstanceId(approval.instanceId())).build();
        }
        return Response.accepted(submitted).build();
    }
}
