package com.tripplanner.resource;

import dev.langchain4j.agentic.agent.AgentInvocationException;
import dev.langchain4j.guardrail.GuardrailException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@Provider
public class GuardrailExceptionMapper implements ExceptionMapper<AgentInvocationException> {

    private static final Logger LOG = Logger.getLogger(GuardrailExceptionMapper.class);

    record ErrorResponse(String error, String message) {}

    @Override
    public Response toResponse(AgentInvocationException exception) {
        Throwable cause = exception;
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (cause != null && visited.add(cause)) {
            if (cause instanceof GuardrailException) {
                LOG.warn("Trip planning rejected by a guardrail", exception);
                return Response.status(422)
                        .entity(new ErrorResponse("guardrail_violation",
                                "The trip plan could not pass the recommendation checks. Please revise your trip details and try again."))
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            cause = cause.getCause();
        }
        LOG.error("Trip planning failed", exception);
        return Response.serverError()
                .entity(new ErrorResponse("planning_failed", "Could not generate the trip plan. Please try again later."))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
