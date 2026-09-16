package com.tripplanner.resource;

import com.tripplanner.model.TripError;
import dev.langchain4j.agentic.agent.AgentInvocationException;
import dev.langchain4j.guardrail.OutputGuardrailException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuardrailExceptionMapperTest {

    private final GuardrailExceptionMapper mapper = new GuardrailExceptionMapper();

    @Test
    void nestedGuardrailFailureReturnsSafe422() {
        var failure = new AgentInvocationException(new IllegalStateException(
                new OutputGuardrailException("private model response <script>secret</script>")));
        try (var response = mapper.toResponse(failure)) {
            assertEquals(422, response.getStatus());
            var error = assertInstanceOf(GuardrailExceptionMapper.ErrorResponse.class, response.getEntity());
            assertEquals("guardrail_violation", error.error());
            assertEquals("The trip plan could not pass the recommendation checks. Please revise your trip details and try again.",
                    error.message());
            assertEquals(new TripError(error.error(), error.message()), TripError.from(failure, false));
        }
    }

    @Test
    void unrelatedFailureAndMissingCauseReturnSafe500() {
        for (var failure : new AgentInvocationException[] {
                new AgentInvocationException("private provider token"),
                new AgentInvocationException(new IllegalStateException("GuardrailException: private provider token")) }) {
            try (var response = mapper.toResponse(failure)) {
                assertEquals(500, response.getStatus());
                var error = assertInstanceOf(GuardrailExceptionMapper.ErrorResponse.class, response.getEntity());
                assertEquals("planning_failed", error.error());
                assertEquals("Could not generate the trip plan. Please try again later.", error.message());
                assertEquals(new TripError(error.error(), error.message()), TripError.from(failure, false));
            }
        }
    }

    @Test
    void cyclicCauseChainDoesNotHang() {
        var first = new IllegalStateException("first");
        var second = new IllegalStateException("second", first);
        first.initCause(second);
        var failure = new AgentInvocationException(first);
        try (var response = mapper.toResponse(failure)) {
            assertEquals(500, response.getStatus());
            assertEquals("planning_failed", TripError.from(failure, false).error());
        }
    }
}
