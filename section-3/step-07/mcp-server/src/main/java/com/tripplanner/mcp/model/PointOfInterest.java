package com.tripplanner.mcp.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class PointOfInterest extends PanacheEntity {

    public String destination;
    public String tripType;
    public String name;
    public String category;
    public String description;
    public double rating;
}
