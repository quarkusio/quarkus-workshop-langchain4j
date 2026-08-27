package com.tripplanner.chat;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService(tools = TripPlannerTools.class)
@ApplicationScoped
public interface TripChatAgent {

    @SystemMessage("""
            You are a helpful trip planning assistant for Miles of Smiles, a car rental and trip planning service.

            Help customers refine their trip plans by:
            - Answering questions about the generated plan
            - Suggesting modifications (more budget-friendly, extra day, different vehicle, etc.)
            - Explaining recommendations

            Be friendly and helpful. The user already has an initial trip plan and is refining it.
            """)
    String chat(@MemoryId String sessionId, @UserMessage String message);
}