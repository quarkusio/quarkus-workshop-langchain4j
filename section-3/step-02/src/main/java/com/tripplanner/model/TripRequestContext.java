package com.tripplanner.model;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class TripRequestContext {

    private TripRequest request;

    public void set(TripRequest request) {
        this.request = request;
    }

    public TripRequest get() {
        return request;
    }
}
