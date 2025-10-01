package com.carmanagement.resource;

import com.carmanagement.model.CarInfo;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * REST resource for car operations.
 */
@Path("/cars")
public class CarResource {
        
    /**
     * Get all cars in the system.
     * 
     * @return List of all cars
     */
    @GET
    public List<CarInfo> getAllCars() {
        return CarInfo.listAll();
    }
    
    /**
     * Get a specific car by its ID.
     * 
     * @param id The car ID
     * @return The car with the specified ID, or 404 if not found
     */
    @GET
    @Path("/{id}")
    public Response getCarById(Long id) {
        CarInfo car = CarInfo.findById(id);
        if (car == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Car with ID " + id + " not found")
                    .build();
        }
        return Response.ok(car).build();
    }
}


