package com.carmanagement.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Model class representing a car in the rental fleet.
 */
@Entity()
@Table(name="car_info")
public class CarInfo extends PanacheEntity {
    public String make;
    public String model;
    public Integer year;
    
    @Enumerated(EnumType.STRING)
    public CarStatus status;
}


