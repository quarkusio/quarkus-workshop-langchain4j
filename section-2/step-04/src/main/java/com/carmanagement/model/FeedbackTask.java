package com.carmanagement.model;

/**
 * Record representing a feedback analysis task with its configuration.
 * Contains the type of feedback, system instructions, output key, and sentinel value.
 */
public record FeedbackTask(
        FeedbackType feedbackType,
        String systemInstructions,
        String outputKey,
        String notRequiredValue) {

    /**
     * Factory method for creating a cleaning feedback task.
     */
    public static FeedbackTask cleaning() {
        return new FeedbackTask(
                FeedbackType.CLEANING,
                """
                You are a cleaning analyzer for a car rental company. Your job is to determine if a car needs cleaning based on feedback.
                Analyze the feedback and car information to decide if a cleaning is needed.
                If the feedback mentions dirt, mud, stains, or anything that suggests the car is dirty, recommend a cleaning.
                Be specific about what type of cleaning is needed (exterior, interior, detailing, waxing).
                If no interior or exterior car cleaning services are needed based on the feedback, respond with "CLEANING_NOT_REQUIRED".
                Include the reason for your choice but keep your response short.
                """,
                "cleaningRequest",
                "CLEANING_NOT_REQUIRED"
        );
    }

    /**
     * Factory method for creating a maintenance feedback task.
     */
    public static FeedbackTask maintenance() {
        return new FeedbackTask(
                FeedbackType.MAINTENANCE,
                """
                You are a car maintenance analyzer for a car rental company. Your job is to determine if a car needs maintenance based on feedback.
                Analyze the feedback and car information to decide if maintenance is needed.
                If the feedback mentions mechanical issues, strange noises, performance problems, significant body damage or anything that suggests
                the car needs maintenance, recommend appropriate maintenance.
                Be specific about what type of maintenance is needed (oil change, tire rotation, brake service, engine service, transmission service, body work).
                If no service of any kind, repairs or maintenance are needed, respond with "MAINTENANCE_NOT_REQUIRED".
                Include the reason for your choice but keep your response short.
                """,
                "maintenanceRequest",
                "MAINTENANCE_NOT_REQUIRED"
        );
    }

    /**
     * Factory method for creating a disposition feedback task.
     */
    public static FeedbackTask disposition() {
        return new FeedbackTask(
                FeedbackType.DISPOSITION,
                """
                You are a disposition analyzer for a car rental company. Your job is to determine if a car should be considered for disposition (removal from fleet).
                
                Analyze the feedback for SEVERE issues that would make the car uneconomical to keep:
                - Major accidents: "wrecked", "totaled", "destroyed", "crashed", "collision"
                - Severe damage: "frame damage", "structural damage", "major damage"
                - Safety concerns: "unsafe", "not drivable", "inoperable", "dangerous"
                - Catastrophic mechanical failure: "engine blown", "transmission failed", "major mechanical failure"
                
                If you detect ANY of these severe issues, respond with:
                "DISPOSITION_REQUIRED: [brief description of the severe issue]"
                
                If the car has only minor or moderate issues that can be repaired, respond with:
                "DISPOSITION_NOT_REQUIRED"
                
                Keep your response concise.
                """,
                "dispositionRequest",
                "DISPOSITION_NOT_REQUIRED"
        );
    }
}

