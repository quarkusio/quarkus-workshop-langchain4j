package com.carmanagement.resource;

import com.carmanagement.model.CarInfo;
import com.carmanagement.service.CarService;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST resource for car operations.
 */
@Path("/cars")
@Produces(MediaType.APPLICATION_JSON)
public class CarResource {
    
    @Inject
    CarService carService;
    
    /**
     * Get all cars in the system.
     * 
     * @return List of all cars
     */
    @GET
    public List<CarInfo> getAllCars() {
        return carService.getAllCars();
    }
    
    /**
     * Get a specific car by its ID.
     * 
     * @param id The car ID
     * @return The car with the specified ID, or 404 if not found
     */
    @GET
    @Path("/{id}")
    public Response getCarById(@PathParam("id") Integer id) {
        CarInfo car = carService.getCarById(id);
        if (car == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Car with ID " + id + " not found")
                    .build();
        }
        return Response.ok(car).build();
    }
}


