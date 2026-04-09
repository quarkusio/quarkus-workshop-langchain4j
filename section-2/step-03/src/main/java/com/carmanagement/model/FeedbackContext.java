package com.carmanagement.model;

/**
 * Context object that encapsulates all feedback data.
 * Provides a clean way to pass feedback information between agents and workflows.
 */
public record FeedbackContext(
    String rentalFeedback,
    String cleaningFeedback,
    String maintenanceFeedback
) {
    /**
     * Constructor that ensures null values are converted to empty strings.
     */
    public FeedbackContext {
        rentalFeedback = rentalFeedback != null ? rentalFeedback : "";
        cleaningFeedback = cleaningFeedback != null ? cleaningFeedback : "";
        maintenanceFeedback = maintenanceFeedback != null ? maintenanceFeedback : "";
    }
}