package com.demo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;

import java.util.ArrayList;
import java.util.List;

import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;

/**
 * Executor for the DispositionAgent.
 * Handles the integration between the A2A framework and the DispositionAgent.
 */
@ApplicationScoped
public class DispositionAgentExecutor {

    @Inject
    DispositionAgent dispositionAgent;
    
    @Inject
    DispositionTool dispositionTool;

    @Produces
    public AgentExecutor agentExecutor(DispositionAgent dispositionAgent) {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {

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
                            System.out.println("Text part: " + textPart.getText());
                            inputs.add(textPart.getText());
                        }
                    }
                }
            
                // Call the agent with all parameters as strings
                String agentResponse = dispositionAgent.processDisposition(
                        inputs.get(0),                      // carMake
                        inputs.get(1),                      // carModel
                        Integer.parseInt(inputs.get(2)),    // carYear
                        Integer.parseInt(inputs.get(3)),    // carNumber
                        inputs.get(4),                      // carCondition
                        inputs.get(5));                     // dispositionRequest
                
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