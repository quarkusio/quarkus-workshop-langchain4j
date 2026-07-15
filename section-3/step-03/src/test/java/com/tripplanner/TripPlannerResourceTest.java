package com.tripplanner;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class TripPlannerResourceTest {

    @Test
    void testPlanEndpoint() {
        given()
                .contentType("application/json")
                .body("""
                        {
                            "destination": "Italian Riviera",
                            "days": 5,
                            "tripType": "family",
                            "travelers": 4,
                            "budget": "moderate (€1000-€2500)",
                            "preferences": "We love coastal towns and good food"
                        }
                        """)
                .when().post("/trip/plan")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("vehicle", notNullValue())
                .body("itinerary", notNullValue());
    }
}
