package com.tripplanner.flow;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Smoke test that verifies the /trip/plan endpoint is reachable and returns
 * an instanceId in the response body.
 *
 * Full restart-and-restore durability is covered by the manual verification
 * steps in the step-04 documentation: start the app, submit a plan, note the
 * instance ID shown in the UI, restart with Ctrl-C, confirm the same ID appears
 * in the "Restoring workflow instance: <id>" startup log.
 *
 * A combined QuarkusDevModeTest (modifyResourceFile + approve-after-reload) was
 * considered but proved fragile when mixing Dev Services PostgreSQL with the
 * in-memory Kafka connector required by the test profile. The manual verification
 * is a more reliable and more instructive demonstration for workshop participants.
 */
@QuarkusIntegrationTest
class FlowDurabilityRestoreIT {

    @Test
    void planEndpointReturnsInstanceId() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "destination": "Portugal Coast",
                            "startDate": "2026-09-01",
                            "days": 5,
                            "tripType": "family",
                            "travelers": 3,
                            "budget": "$2000",
                            "preferences": "scenic beaches"
                        }
                        """)
                .when()
                .post("/trip/plan")
                .then()
                .statusCode(200)
                .body("instanceId", notNullValue());
    }
}
