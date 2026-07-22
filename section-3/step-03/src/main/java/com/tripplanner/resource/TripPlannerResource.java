package com.tripplanner.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.agentic.flow.TripPlanStore;
import com.tripplanner.model.TripRequest;
import com.tripplanner.model.TripRequestContext;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonFormat;
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
    Emitter<byte[]> flowInProducer;

    @POST
    @Path("/plan")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response planTrip(TripRequest request) throws Exception {
        tripRequestContext.set(request);

        String previousId = tripPlanStore.latest() != null
                ? tripPlanStore.latest().instanceId() : null;

        byte[] body = objectMapper.writeValueAsBytes(request);
        CloudEvent event = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create("api:/trip/plan"))
                .withType("com.tripplanner.booking.confirmed")
                .withDataContentType("application/json")
                .withData(body)
                .build();

        flowInProducer.send(new JsonFormat().serialize(event));

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
}
