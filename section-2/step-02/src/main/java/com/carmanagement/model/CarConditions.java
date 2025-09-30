package com.carmanagement.model;

/**
 * Record representing the conditions of a car.
 *
 * @param generalCondition   A description of the car's general condition
 * @param carWashRequired    Indicates if a car wash is required
 */
public record CarConditions(String generalCondition, boolean carWashRequired) {
}
