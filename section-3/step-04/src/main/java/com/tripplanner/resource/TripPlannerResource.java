package com.tripplanner.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.agentic.flow.TripPlanStore;
import com.tripplanner.model.TripRequest;
import com.tripplanner.model.TripRequestContext;
import io.smallrye.reactive.messaging.ce.OutgoingCloudEventMetadata;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;

import java.net.URI;
import java.util.UUID;

@Path("/trip")
public class TripPlannerResource {

    private static final long PLAN_TIMEOUT_SECONDS = 120;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    TripRequestContext tripRequestContext;

    @Inject
    TripPlanStore tripPlanStore;

    @Channel("flow-in-producer")
    Emitter<String> flowInProducer;

    @POST
    @Path("/plan")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response planTrip(TripRequest request) throws Exception {
        tripRequestContext.set(request);

        String previousId = tripPlanStore.latest() != null
                ? tripPlanStore.latest().instanceId() : null;

        String body = objectMapper.writeValueAsString(request);
        OutgoingCloudEventMetadata<String> metadata = OutgoingCloudEventMetadata.<String>builder()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create("api:/trip/plan"))
                .withType("com.tripplanner.booking.confirmed")
                .withDataContentType("application/json")
                .build();

        flowInProducer.send(Message.of(body, Metadata.of(metadata)));

        TripPlanStore.TripPlanStatus planStatus =
                tripPlanStore.awaitNextPlan(previousId, PLAN_TIMEOUT_SECONDS);

        if (planStatus == null) {
            return Response.status(Response.Status.GATEWAY_TIMEOUT)
                    .entity("Plan generation timed out")
                    .build();
        }
        return Response.ok(planStatus).build();
    }

    @GET
    @Path("/plan/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response planStatus(@QueryParam("instanceId") String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("instanceId is required")
                    .build();
        }
        TripPlanStore.TripPlanStatus status = tripPlanStore.byInstanceId(instanceId);
        if (status == null) {
            return Response.noContent().build();
        }
        return Response.ok(status).build();
    }

    /**
     * Returns the most recent plan awaiting approval, if any.
     * Used by the UI to restore the results page after a page reload or app restart.
     * Returns 204 No Content when no awaiting plan exists.
     */
    @GET
    @Path("/plan/latest")
    @Produces(MediaType.APPLICATION_JSON)
    public Response latestPlan() {
        TripPlanStore.TripPlanStatus status = tripPlanStore.latest();
        if (status == null || !"awaiting_approval".equals(status.status())) {
            return Response.noContent().build();
        }
        return Response.ok(status).build();
    }
}
