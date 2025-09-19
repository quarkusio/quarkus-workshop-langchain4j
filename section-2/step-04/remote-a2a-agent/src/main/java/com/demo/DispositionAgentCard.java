package com.demo;

import java.util.Collections;
import java.util.List;

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
                .documentationUrl("http://example.com/docs")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .stateTransitionHistory(true)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(new AgentSkill.Builder()
                                .id("disposition")
                                .name("Car disposition")
                                .description("Makes a request to dispose of a car (SCRAP, SELL, or DONATE)")
                                .tags(List.of("disposition"))
                                .build()))
                .protocolVersion("0.2.5")
                .build();
    }
}