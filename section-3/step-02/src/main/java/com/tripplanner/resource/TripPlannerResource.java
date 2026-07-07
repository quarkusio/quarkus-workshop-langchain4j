package com.tripplanner.resource;

import com.tripplanner.agentic.agents.TripPlannerAgent;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripRequest;
import com.tripplanner.model.TripRequestContext;
import dev.langchain4j.agentic.agent.AgentInvocationException;
import dev.langchain4j.guardrail.GuardrailException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Path("/trip")
public class TripPlannerResource {

    @Inject
    TripPlannerAgent tripPlannerAgent;

    @Inject
    TripRequestContext tripRequestContext;

    @POST
    @Path("/plan")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TripPlan planTrip(TripRequest request) {
        tripRequestContext.set(request);
        return tripPlannerAgent.planTrip(
                request.destination(),
                request.days(),
                request.tripType(),
                request.travelers(),
                request.budget(),
                request.preferences()
        );
    }

    @Provider
    public static class GuardrailExceptionMapper implements ExceptionMapper<AgentInvocationException> {

        record ErrorResponse(String error, String message) {}

        @Override
        public Response toResponse(AgentInvocationException exception) {
            Throwable cause = exception.getCause();
            while (cause != null && !(cause instanceof GuardrailException)) {
                cause = cause.getCause();
            }
            String message = cause != null ? cause.getMessage() : exception.getMessage();
            return Response.status(422)
                    .entity(new ErrorResponse("guardrail_violation", message))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}
