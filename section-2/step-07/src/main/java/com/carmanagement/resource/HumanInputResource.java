package com.carmanagement.resource;

import com.carmanagement.service.HumanInputService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.logging.Log;

import java.util.Map;

/**
 * REST resource for human input in the Human-in-the-Loop pattern.
 * Provides endpoints for humans to view pending requests and provide decisions.
 */
@Path("/api/human-input")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HumanInputResource {

    @Inject
    HumanInputService humanInputService;

    /**
     * Get all pending human input requests.
     * This is called by the UI to display requests awaiting human decision.
     */
    @GET
    @Path("/pending")
    public Map<String, String> getPendingRequests() {
        return humanInputService.getPendingRequests();
    }

    /**
     * Provide human input for a pending request.
     * This is called when a human makes a decision in the UI.
     * 
     * @param requestId The request ID (e.g., "car-123")
     * @param request Request body containing the decision
     */
    @POST
    @Path("/{requestId}")
    public Response provideInput(
            @PathParam("requestId") String requestId,
            Map<String, String> request) {
        
        try {
            String decision = request.get("decision");
            if (decision == null || decision.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Decision is required"))
                        .build();
            }

            if (!humanInputService.hasPendingRequest(requestId)) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "No pending request found for: " + requestId))
                        .build();
            }

            Log.infof("Human decision received for %s: %s", requestId, decision);
            humanInputService.provideInput(requestId, decision);

            return Response.ok(Map.of(
                "message", "Decision recorded",
                "requestId", requestId,
                "decision", decision
            )).build();
            
        } catch (Exception e) {
            Log.error("Error processing human input", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error processing decision: " + e.getMessage()))
                    .build();
        }
    }
}


