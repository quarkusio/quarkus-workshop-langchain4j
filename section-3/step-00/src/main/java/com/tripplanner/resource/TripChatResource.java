package com.tripplanner.resource;

import com.tripplanner.chat.TripChatAgent;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/trip/chat")
public class TripChatResource {

    @Inject
    TripChatAgent chatAgent;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ChatResponse chat(ChatRequest request) {
        String response = chatAgent.chat(request.sessionId(), request.message());
        return new ChatResponse(response);
    }

    public record ChatRequest(String sessionId, String message) {}
    public record ChatResponse(String response) {}
}