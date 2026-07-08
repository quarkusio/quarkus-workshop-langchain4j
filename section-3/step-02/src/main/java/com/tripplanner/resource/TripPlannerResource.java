package com.tripplanner.resource;

import com.tripplanner.agentic.workflow.TripPlannerSystem;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripRequest;
import com.tripplanner.model.TripRequestContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/trip")
public class TripPlannerResource {

    @Inject
    TripPlannerSystem tripPlannerSystem;

    @Inject
    TripRequestContext tripRequestContext;

    @POST
    @Path("/plan")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TripPlan planTrip(TripRequest request) {
        tripRequestContext.set(request);
        return tripPlannerSystem.planTrip(
                request.destination(),
                request.days(),
                request.tripType(),
                request.travelers(),
                request.budget(),
                request.preferences()
        );
    }
}
