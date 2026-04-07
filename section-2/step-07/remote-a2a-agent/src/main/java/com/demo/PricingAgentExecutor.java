package com.demo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.quarkus.logging.Log;

import java.util.ArrayList;
import java.util.List;

import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;

/**
 * Executor for the PricingAgent.
 * Handles the integration between the A2A framework and the PricingAgent.
 */
@ApplicationScoped
public class PricingAgentExecutor {

    @Produces
    public AgentExecutor agentExecutor(PricingAgent pricingAgent) {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                Log.info("Remote A2A PricingAgent called");

                TaskUpdater updater = new TaskUpdater(context, eventQueue);
                if (context.getTask() == null) {
                    updater.submit();
                }
                updater.startWork();

                List<String> inputs = new ArrayList<>();

                // Process the request message
                Message message = context.getMessage();
                if (message.getParts() != null) {
                    for (Part<?> part : message.getParts()) {
                        if (part instanceof TextPart textPart) {
                            inputs.add(textPart.getText());
                        }
                    }
                }

                Log.debugf("Estimating value for %s %s %s",
                    inputs.get(0), inputs.get(1), inputs.get(2));

                // Call the pricing agent with all parameters
                String agentResponse = pricingAgent.estimateValue(
                        inputs.get(0),                      // carMake
                        inputs.get(1),                      // carModel
                        Integer.parseInt(inputs.get(2)),    // carYear
                        inputs.get(3));                     // carCondition

                Log.debugf("PricingAgent response: %s", agentResponse);

                // Return the result
                TextPart responsePart = new TextPart(agentResponse, null);
                List<Part<?>> parts = List.of(responsePart);
                updater.addArtifact(parts, null, null, null);
                updater.complete();
            }

            @Override
            public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                throw new UnsupportedOperationError();
            }
        };
    }
}
