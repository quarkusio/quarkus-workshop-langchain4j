package dev.langchain4j.quarkus.workshop;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static io.restassured.RestAssured.given;

import jakarta.inject.Inject;


@QuarkusTest
public class WeatherServiceTest {

    @Test
    public void testGetForecast() {

        // Use RestAssured to test the endpoint
        var s = given()
                .when().get("weather?latitude=52.52&longitude=13.41")
                .then()
                .statusCode(200)
                .extract().body().asString();
        assertThat(s).contains("temperature_2m", "rain", "precipitation");
    }
}
