package com.carmanagement.resource;

import com.carmanagement.model.ApprovalProposal;
import com.carmanagement.service.ApprovalService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.logging.Log;

import java.util.List;
import java.util.Map;

/**
 * REST resource for managing approval proposals.
 * Provides endpoints for humans to view and approve/reject proposals.
 */
@Path("/api/approvals")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ApprovalResource {

    @Inject
    ApprovalService approvalService;

    /**
     * Get all pending approval proposals.
     * This is called by the UI to display proposals awaiting human decision.
     */
    @GET
    @Path("/pending")
    public List<ApprovalProposal> getPendingProposals() {
        return approvalService.getPendingProposals();
    }

    /**
     * Get a specific proposal by ID.
     */
    @GET
    @Path("/{proposalId}")
    public Response getProposal(@PathParam("proposalId") Long proposalId) {
        ApprovalProposal proposal = approvalService.getProposal(proposalId);
        if (proposal == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Proposal not found"))
                    .build();
        }
        return Response.ok(proposal).build();
    }

    /**
     * Approve a proposal.
     * This is called when a human clicks the "Approve" button in the UI.
     * 
     * @param proposalId The proposal ID
     * @param request Request body containing reason and approvedBy
     */
    @POST
    @Path("/{proposalId}/approve")
    public Response approveProposal(
            @PathParam("proposalId") Long proposalId,
            Map<String, String> request) {
        
        try {
            String reason = request.getOrDefault("reason", "Approved by human reviewer");
            String approvedBy = request.getOrDefault("approvedBy", "Workshop User");

            Log.infof("Approval request received for proposal %d by %s", proposalId, approvedBy);

            ApprovalProposal proposal = approvalService.processDecision(
                    proposalId, true, reason, approvedBy);

            return Response.ok(proposal).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            Log.error("Error approving proposal", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error processing approval: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Reject a proposal.
     * This is called when a human clicks the "Reject" button in the UI.
     * 
     * @param proposalId The proposal ID
     * @param request Request body containing reason and approvedBy
     */
    @POST
    @Path("/{proposalId}/reject")
    public Response rejectProposal(
            @PathParam("proposalId") Long proposalId,
            Map<String, String> request) {
        
        try {
            String reason = request.getOrDefault("reason", "Rejected by human reviewer");
            String approvedBy = request.getOrDefault("approvedBy", "Workshop User");

            Log.infof("Rejection request received for proposal %d by %s", proposalId, approvedBy);

            ApprovalProposal proposal = approvalService.processDecision(
                    proposalId, false, reason, approvedBy);

            return Response.ok(proposal).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            Log.error("Error rejecting proposal", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error processing rejection: " + e.getMessage()))
                    .build();
        }
    }
}


