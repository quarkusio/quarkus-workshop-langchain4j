package com.tripplanner.mcp.model;

public record PointOfInterest(
        String name,
        String category,
        String description,
        double rating
) {}
