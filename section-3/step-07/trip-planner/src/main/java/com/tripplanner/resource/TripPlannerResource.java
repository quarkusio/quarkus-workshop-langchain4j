package com.tripplanner.resource;

import com.tripplanner.agentic.flow.TripPlanStore;
import com.tripplanner.model.PlanningRequest;
import com.tripplanner.model.TripError;
import com.tripplanner.model.TripPlanStatus;
import com.tripplanner.model.TripRequest;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Path("/trip")
@Produces(MediaType.APPLICATION_JSON)
public class TripPlannerResource {
    @ConfigProperty(name = "trip.planning.timeout", defaultValue = "PT120S")
    Duration planTimeout;

    @Inject
    TripPlanStore tripPlanStore;

    @Channel("flow-in-producer")
    Emitter<PlanningRequest> flowInProducer;

    @POST
    @Path("/plan")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response planTrip(TripRequest request) {
        if (request == null) return Response.status(400).entity(new TripError("invalid_request", "Trip details are required.")).build();
        PlanningRequest input = tripPlanStore.register(request);
        OutgoingCloudEventMetadata<PlanningRequest> metadata = OutgoingCloudEventMetadata.<PlanningRequest>builder()
                .withId(input.requestId())
                .withSource(URI.create("api:/trip/plan"))
                .withType("com.tripplanner.trip.requested")
                .withDataContentType("application/json")
                .build();
        try {
            flowInProducer.send(Message.of(input, Metadata.of(metadata)).withNack(failure -> {
                tripPlanStore.submissionFailed(input.requestId(), failure);
                return CompletableFuture.completedFuture(null);
            }));
        } catch (RuntimeException failure) {
            tripPlanStore.submissionFailed(input.requestId(), failure);
        }
        try {
            TripPlanStatus result = tripPlanStore.awaitPlan(input.requestId(), planTimeout);
            if (result == null) {
                TripPlanStatus pending = tripPlanStore.byRequestId(input.requestId());
                return Response.status(504).entity(new TripPlanStatus(pending.requestId(), pending.instanceId(),
                        pending.request(), pending.status(), pending.plan(), pending.confirmation(), "planning_timeout",
                        "Planning is taking longer than expected. The workflow may still complete; check its status before retrying.")).build();
            }
            int code = "failed".equals(result.status()) ? new TripError(result.error(), result.message()).httpStatus() : 200;
            return Response.status(code).entity(result).build();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return Response.status(503).entity(new TripError("wait_interrupted",
                    "The wait was interrupted. The workflow may still complete; check its status before retrying.")).build();
        }
    }

    @GET
    @Path("/plan/status")
    public Response planStatus(@QueryParam("instanceId") String instanceId, @QueryParam("requestId") String requestId) {
        if ((instanceId == null || instanceId.isBlank()) && (requestId == null || requestId.isBlank())) {
            return Response.status(400).entity(new TripError("invalid_request", "instanceId or requestId is required.")).build();
        }
        TripPlanStatus status = instanceId != null && !instanceId.isBlank()
                ? tripPlanStore.byInstanceId(instanceId) : tripPlanStore.byRequestId(requestId);
        if (status == null) return Response.status(404).entity(new TripError("unknown_trip", "The requested trip was not found.")).build();
        return Response.ok(status).build();
    }

    @GET
    @Path("/plan/latest")
    public Response latestPlan() {
        TripPlanStatus status = tripPlanStore.latest();
        return status == null ? Response.noContent().build() : Response.ok(status).build();
    }
}
