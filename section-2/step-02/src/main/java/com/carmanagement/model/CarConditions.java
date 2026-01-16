package com.carmanagement.model;

/**
 * Record representing the conditions of a car.
 *
 * @param generalCondition   A description of the car's general condition
 * @param cleaningRequired    Indicates if a cleaning is required
 */
public record CarConditions(String generalCondition, boolean cleaningRequired) {
}
