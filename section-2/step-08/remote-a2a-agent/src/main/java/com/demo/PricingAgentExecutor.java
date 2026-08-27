package com.demo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.TaskNotCancelableError;
import org.a2aproject.sdk.spec.TextPart;
import io.quarkus.logging.Log;

import java.util.List;

@ApplicationScoped
public class PricingAgentExecutor {

    @Produces
    public AgentExecutor agentExecutor(PricingAgent pricingAgent) {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
                Log.info("Remote A2A PricingAgent called");

                if (context.getTask() == null) {
                    emitter.submit();
                }
                emitter.startWork();

                String[] inputs = context.getUserInput("\n").split("\n");

                Log.debugf("Estimating value for %s %s %s",
                    inputs[0], inputs[1], inputs[2]);

                String agentResponse = pricingAgent.estimateValue(
                        inputs[0],                      // carMake
                        inputs[1],                      // carModel
                        Integer.parseInt(inputs[2]),     // carYear
                        inputs[3]);                     // carCondition

                Log.debugf("PricingAgent response: %s", agentResponse);

                emitter.addArtifact(List.of(new TextPart(agentResponse)));
                emitter.complete();
            }

            @Override
            public void cancel(RequestContext context, AgentEmitter emitter) throws A2AError {
                throw new TaskNotCancelableError();
            }
        };
    }
}
