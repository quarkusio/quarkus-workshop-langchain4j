package com.carmanagement.model;

/**
 * Context object that encapsulates feedback data.
 * Provides a clean way to pass feedback information between agents and workflows.
 */
public record FeedbackContext(
    String rentalFeedback,
    String cleaningFeedback
) {
    /**
     * Constructor that ensures null values are converted to empty strings.
     */
    public FeedbackContext {
        rentalFeedback = rentalFeedback != null ? rentalFeedback : "";
        cleaningFeedback = cleaningFeedback != null ? cleaningFeedback : "";
    }
}