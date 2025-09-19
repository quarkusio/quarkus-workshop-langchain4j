package com.carmanagement.service;

import com.carmanagement.model.CarInfo;
import com.carmanagement.model.CarStatus;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service class that manages the car data and provides methods to access it.
 */
@ApplicationScoped
public class CarService {
    private final Map<Integer, CarInfo> cars = new ConcurrentHashMap<>();
    
    /**
     * Initialize the service with mock car data.
     */
    @PostConstruct
    void init() {
        generateMockData();
    }
    
    /**
     * Get all cars in the system.
     * 
     * @return List of all cars
     */
    public List<CarInfo> getAllCars() {
        return new ArrayList<>(cars.values());
    }
    
    /**
     * Get a specific car by its ID.
     * 
     * @param id The car ID
     * @return The car with the specified ID, or null if not found
     */
    public CarInfo getCarById(Integer id) {
        return cars.get(id);
    }
    
    /**
     * Generate hardcoded mock data for the car rental fleet.
     */
    private void generateMockData() {
        // Car 1
        cars.put(1, new CarInfo(
            1,
            "Toyota",
            "Corolla",
            2020,
            CarStatus.AVAILABLE
        ));
        
        // Car 2
        cars.put(2, new CarInfo(
            2,
            "Honda",
            "Civic",
            2019,
            CarStatus.RENTED
        ));
        
        // Car 3
        cars.put(3, new CarInfo(
            3,
            "Ford",
            "F-150",
            2021,
            CarStatus.AT_CAR_WASH
        ));
        
        // Car 4
        cars.put(4, new CarInfo(
            4,
            "Chevrolet",
            "Malibu",
            2018,
            CarStatus.AVAILABLE
        ));
        
        // Car 5
        cars.put(5, new CarInfo(
            5,
            "BMW",
            "X5",
            2022,
            CarStatus.AT_CAR_WASH
        ));
        
        // Car 6
        cars.put(6, new CarInfo(
            6,
            "Mercedes-Benz",
            "C-Class",
            2020,
            CarStatus.RENTED
        ));
        
        // Car 7
        cars.put(7, new CarInfo(
            7,
            "Audi",
            "A4",
            2021,
            CarStatus.AVAILABLE
        ));
        
        // Car 8
        cars.put(8, new CarInfo(
            8,
            "Nissan",
            "Altima",
            2017,
            CarStatus.AVAILABLE
        ));
        
        // Car 9
        cars.put(9, new CarInfo(
            9,
            "Toyota",
            "Camry",
            2019,
            CarStatus.AVAILABLE
        ));
        
        // Car 10
        cars.put(10, new CarInfo(
            10,
            "Honda",
            "Accord",
            2020,
            CarStatus.RENTED
        ));
        
        // Car 11
        cars.put(11, new CarInfo(
            11,
            "Ford",
            "Mustang",
            2022,
            CarStatus.AT_CAR_WASH
        ));
        
        // Car 12
        cars.put(12, new CarInfo(
            12,
            "Chevrolet",
            "Silverado",
            2021,
            CarStatus.AVAILABLE
        ));
        
        // Car 13
        cars.put(13, new CarInfo(
            13,
            "BMW",
            "3 Series",
            2020,
            CarStatus.AT_CAR_WASH
        ));
        
        // Car 14
        cars.put(14, new CarInfo(
            14,
            "Mercedes-Benz",
            "E-Class",
            2023,
            CarStatus.AVAILABLE
        ));
        
        // Car 15
        cars.put(15, new CarInfo(
            15,
            "Audi",
            "Q5",
            2022,
            CarStatus.RENTED
        ));
    }
}


