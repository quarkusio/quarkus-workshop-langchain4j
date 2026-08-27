package com.demo;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.TransportProtocol;

@ApplicationScoped
public class PricingAgentCard {

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return AgentCard.builder()
                .name("Pricing Agent")
                .description("Estimates the market value of a vehicle based on make, model, year, and condition.")
                .url("http://localhost:8888/")
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(AgentSkill.builder()
                                .id("pricing")
                                .name("Vehicle pricing")
                                .description("Estimates the market value of a vehicle based on make, model, year, and condition")
                                .tags(List.of("pricing", "valuation"))
                                .build()))
                .preferredTransport(TransportProtocol.JSONRPC.asString())
                .supportedInterfaces(Collections.singletonList(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:8888/")))
                .build();
    }
}
