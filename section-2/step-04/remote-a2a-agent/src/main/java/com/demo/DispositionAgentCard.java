package com.demo;

import java.util.Collections;
import java.util.List;

import io.a2a.spec.AgentInterface;
import io.a2a.spec.TransportProtocol;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;

@ApplicationScoped
public class DispositionAgentCard {

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return new AgentCard.Builder()
                .name("Disposition Agent")
                .description("Determines how a car should be disposed of based on the car condition and disposition request.")
                .url("http://localhost:8888/")
                .version("1.0.0")
                .protocolVersion("1.0.0")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .stateTransitionHistory(false)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(new AgentSkill.Builder()
                                .id("disposition")
                                .name("Car disposition")
                                .description("Makes a request to dispose of a car (SCRAP, SELL, or DONATE)")
                                .tags(List.of("disposition"))
                                .build()))
                .preferredTransport(TransportProtocol.JSONRPC.asString())
                .additionalInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:8888")))
                .build();
    }
}