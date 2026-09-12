package com.carmanagement.model;

/**
 * Record containing the three feedback analysis results.
 * These are the outputs from the parallel feedback analysis workflow.
 */
public record FeedbackAnalysisResults(
        String cleaningAnalysis,
        String maintenanceAnalysis,
        String dispositionAnalysis
) {
}