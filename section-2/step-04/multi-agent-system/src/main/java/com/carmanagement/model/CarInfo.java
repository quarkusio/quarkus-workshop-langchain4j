package com.carmanagement.model;

import java.time.LocalDate;

/**
 * Model class representing a car in the rental fleet.
 */
public class CarInfo {
    private Integer id;
    private String make;
    private String model;
    private Integer year;
    private String condition;
    private LocalDate dispositionDate;
    private CarStatus status;
    
    // Default constructor
    public CarInfo() {
    }
    
    // Constructor with all fields
    public CarInfo(Integer id, String make, String model, Integer year, String condition, 
                  LocalDate dispositionDate, CarStatus status) {
        this.id = id;
        this.make = make;
        this.model = model;
        this.year = year;
        this.condition = condition;
        this.dispositionDate = dispositionDate;
        this.status = status;
    }
    
    // Getters and setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getMake() {
        return make;
    }
    
    public void setMake(String make) {
        this.make = make;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public Integer getYear() {
        return year;
    }
    
    public void setYear(Integer year) {
        this.year = year;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    public LocalDate getDispositionDate() {
        return dispositionDate;
    }
    
    public void setDispositionDate(LocalDate dispositionDate) {
        this.dispositionDate = dispositionDate;
    }
    
    public CarStatus getStatus() {
        return status;
    }
    
    public void setStatus(CarStatus status) {
        this.status = status;
    }
}


