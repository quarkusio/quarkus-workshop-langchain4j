package com.carmanagement.model;

/**
 * Record representing the conditions of a car.
 *
 * @param generalCondition   A description of the car's general condition
 * @param requiredAction    Indicates the action required
 */
public record CarConditions(String generalCondition, RequiredAction requiredAction) {
}
