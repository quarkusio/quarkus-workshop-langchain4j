package com.carmanagement.model;

/**
 * Record containing the three feedback analysis results.
 */
public record FeedbackRequests(
        String cleaningRequest,
        String maintenanceRequest,
        String dispositionRequest
) {
}