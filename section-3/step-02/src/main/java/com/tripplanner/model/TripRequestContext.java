package com.tripplanner.model;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TripRequestContext {

    private volatile TripRequest request;

    public void set(TripRequest request) {
        this.request = request;
    }

    public TripRequest get() {
        return request;
    }
}
