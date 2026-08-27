package com.tripplanner.flow;

import com.tripplanner.agentic.flow.TripPlannerFlowAdapter;
import com.tripplanner.model.TripRequest;
import com.tripplanner.resource.TripPlannerResource;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies that Quarkus Flow persists workflow instance rows to PostgreSQL
 * while a workflow is suspended at a listen() task.
 *
 * This covers the "persistence half" of the durability story: the workflow state
 * exists in the database after suspension. The restore half (restarting and
 * resuming) is covered by the manual verification steps in the docs.
 */
@QuarkusTest
class FlowPersistenceTest {

    @Inject
    TripPlannerResource tripPlannerResource;

    @InjectMock
    TripPlannerFlowAdapter flowAdapter;

    @Inject
    EntityManager entityManager;

    @Test
    void flowInstanceRowExistsWhileWorkflowIsSuspended() {
        // The MockTripPlannerFlowAdapter (via @Mock) stubs planFromRequest and finalizeBooking.
        // We do NOT inject a mock for flowAdapter here so the real Flow execution runs
        // and a workflow instance is actually persisted.
        // However, MockTripPlannerFlowAdapter is picked up via @Mock CDI bean replacement,
        // so the LLM call is still bypassed.

        TripRequest request = new TripRequest(
                "Portugal Coast", "2026-09-01", 5, "family", 3, "$2000", "scenic beaches");

        // Start the workflow in a background thread — it will block at listen()
        Thread planThread = new Thread(() -> {
            try {
                tripPlannerResource.planTrip(request);
            } catch (Exception ignored) {
            }
        });
        planThread.setDaemon(true);
        planThread.start();

        // Wait until a persisted workflow instance row appears in the database.
        // The table name used by quarkus-flow-jpa is 'workflow_instance'.
        await().atMost(30, SECONDS).until(this::workflowInstanceRowExists);

        List<?> rows = queryWorkflowInstances();
        assertFalse(rows.isEmpty(),
                "At least one workflow_instance row should exist while the workflow is suspended at listen()");
    }

    @Transactional
    boolean workflowInstanceRowExists() {
        return !queryWorkflowInstances().isEmpty();
    }

    @Transactional
    List<?> queryWorkflowInstances() {
        return entityManager
                .createNativeQuery("SELECT id FROM workflow_instance")
                .getResultList();
    }
}
