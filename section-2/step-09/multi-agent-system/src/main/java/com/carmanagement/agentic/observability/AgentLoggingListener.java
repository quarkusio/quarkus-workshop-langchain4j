package com.carmanagement.agentic.observability;

import dev.langchain4j.agentic.observability.AgentInvocationError;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Logs when an agent starts, completes or fails.
 * Being an AgentListener bean, it is registered on all the agents of the application.
 */
@ApplicationScoped
public class AgentLoggingListener implements AgentListener {

    @Override
    public void beforeAgentInvocation(AgentRequest request) {
        Log.infof("Agent %s started", request.agentName());
    }

    @Override
    public void afterAgentInvocation(AgentResponse response) {
        Log.infof("Agent %s completed: %s", response.agentName(), abbreviate(response.output()));
    }

    @Override
    public void onAgentInvocationError(AgentInvocationError error) {
        Log.warnf("Agent %s failed: %s", error.agentName(), error.error().getMessage());
    }

    private static String abbreviate(Object value) {
        String text = String.valueOf(value).replace('\n', ' ');
        return text.length() > 60 ? text.substring(0, 60) + "..." : text;
    }
}
