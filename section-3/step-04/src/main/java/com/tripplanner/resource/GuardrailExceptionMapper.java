package com.tripplanner.resource;

import dev.langchain4j.agentic.agent.AgentInvocationException;
import dev.langchain4j.guardrail.GuardrailException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GuardrailExceptionMapper implements ExceptionMapper<AgentInvocationException> {

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
