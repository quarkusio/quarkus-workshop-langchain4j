# Car Management API

A simple REST API for a car rental agency to manage their fleet of cars. This project is built with Quarkus, the Supersonic Subatomic Java Framework.

## Features

- Get information about all cars in the fleet
- Get detailed information about a specific car by ID
- Mock data generation for demonstration purposes

## Prerequisites

- JDK 17+
- Maven 3.8.1+

## Running the application

### Development mode

```bash
cd car-management
./mvnw quarkus:dev
```

This command will start the application in development mode. The application will be accessible at http://localhost:8080.

### Production mode

```bash
cd car-management
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

### Creating a native executable

You can create a native executable using:

```bash
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```bash
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

## API Endpoints

### Get All Cars

```
GET /cars
```

Returns a list of all cars in the fleet.

**Example Response:**
```json
[
  {
    "id": 1,
    "make": "Toyota",
    "model": "Corolla",
    "year": 2020,
    "lastRenterFeedback": "Great car, very comfortable",
    "condition": "Good condition, minor wear and tear",
    "dispositionDate": "2026-03-15",
    "status": "available to rent"
  },
  {
    "id": 2,
    "make": "Honda",
    "model": "Civic",
    "year": 2019,
    "lastRenterFeedback": "Fuel efficient and reliable",
    "condition": "Small scratch on rear bumper",
    "dispositionDate": null,
    "status": "in maintenance"
  }
]
```

### Get Car by ID

```
GET /cars/{id}
```

Returns detailed information about a specific car.

**Example Response:**
```json
{
  "id": 1,
  "make": "Toyota",
  "model": "Corolla",
  "year": 2020,
  "lastRenterFeedback": "Great car, very comfortable",
  "condition": "Good condition, minor wear and tear",
  "dispositionDate": "2026-03-15",
  "status": "available to rent"
}
```

If the car is not found, the API will return a 404 Not Found response.

## Project Structure

- `com.carmanagement.model`: Contains the data models (CarInfo, CarStatus)
- `com.carmanagement.service`: Contains the business logic and data management
- `com.carmanagement.resource`: Contains the REST endpoints

## Mock Data

The application generates 15 mock car entries with varied:
- Makes and models (Toyota, Honda, Ford, BMW, etc.)
- Years (between 2015-2023)
- Conditions (from "Like new" to "Needs repairs")
- Statuses (in maintenance, rented, at car wash, available to rent)