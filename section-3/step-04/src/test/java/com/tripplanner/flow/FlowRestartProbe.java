package com.tripplanner.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.agentic.flow.TripPlanStore;
import com.tripplanner.agentic.flow.TripPlannerFlowAdapter;
import com.tripplanner.model.TripApproval;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripPlanStatus;
import com.tripplanner.model.TripRequest;
import io.quarkiverse.flow.persistence.jpa.WorkflowInstanceRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.reactive.messaging.ce.CloudEventMetadata;
import io.smallrye.reactive.messaging.ce.OutgoingCloudEventMetadata;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in, separate JVM phases; see README. Not selected by the default Surefire test patterns. */
@QuarkusTest
@TestProfile(FlowRestartProbe.RestartProfile.class)
class FlowRestartProbe {
    private static final TripRequest REQUEST = new TripRequest("Restart coast", "2027-07-10", 5, "family", 4, "economy", "Short drives");
    private static final TripPlan PLAN = new TripPlan(null, "Restart route", List.of(), null);

    @Inject
    TripPlanStore store;
    @Inject
    ObjectMapper mapper;
    @Inject @Any
    InMemoryConnector connector;
    @Inject
    WorkflowInstanceRepository checkpoints;
    @Inject
    RestartProfile.ScriptedAdapter adapter;

    @Test
    void runRestartPhase() throws Exception {
        String phase = System.getProperty("trip.restart.phase");
        if ("prepare".equals(phase)) {
            assertNull(store.latest(), "Use an empty, disposable database for the prepare phase");
            var input = store.register(REQUEST);
            var metadata = OutgoingCloudEventMetadata.<String>builder().withId(input.requestId())
                    .withSource(URI.create("test:/restart")).withType("com.tripplanner.trip.requested").build();
            connector.<Message<String>>source("flow-in").send(Message.of(mapper.writeValueAsString(input), Metadata.of(metadata)));
            relayOutcome("com.tripplanner.trip.approval.requested");
            await().atMost(10, SECONDS).until(() -> "awaiting_approval".equals(store.latest().status()));
            TripPlanStatus pending = store.latest();
            assertEquals(REQUEST, pending.request());
            assertEquals(PLAN, pending.plan());
            assertEquals(1, adapter.planningCalls());
            await().atMost(10, SECONDS).until(() -> QuarkusTransaction.requiringNew().call(() ->
                    checkpoints.count("instanceId", pending.instanceId()) == 1));
            return;
        }

        TripPlanStatus restored = store.latest();
        assertNotNull(restored, "The prepare JVM must finish before starting this JVM against the same database");
        assertEquals(REQUEST, restored.request());
        assertEquals(PLAN, restored.plan());
        assertEquals(0, adapter.planningCalls(), "Restoration must not run planning again");
        assertEquals(restored, given().queryParam("requestId", restored.requestId()).get("/trip/plan/status").as(TripPlanStatus.class));
        if ("verify".equals(phase)) {
            assertEquals("confirmed", restored.status());
            assertNotNull(restored.confirmation());
            assertFalse(store.matchesDecision(restored.instanceId(), new TripApproval(restored.instanceId(), "approved", "")));
            assertEquals(restored, given().get("/trip/plan/latest").as(TripPlanStatus.class));
            return;
        }

        assertEquals("restore", phase);
        assertEquals("awaiting_approval", restored.status());
        given().contentType("application/json").body(new TripApproval(restored.instanceId(), "approved", ""))
                .put("/trip/approve").then().statusCode(202);
        Message<?> decision = connector.sink("flow-in-producer").received().getLast();
        connector.<Message<String>>source("flow-in").send(Message.of(mapper.writeValueAsString(decision.getPayload()), decision.getMetadata()));
        relayOutcome("com.tripplanner.booking.finalized");
        await().atMost(10, SECONDS).until(() -> "confirmed".equals(store.byInstanceId(restored.instanceId()).status()));
        TripPlanStatus confirmed = store.byInstanceId(restored.instanceId());
        assertEquals(restored.requestId(), confirmed.requestId());
        assertEquals(restored.request(), confirmed.request());
        assertEquals(restored.plan(), confirmed.plan());
        assertNotNull(confirmed.confirmation());
        assertEquals(0, adapter.planningCalls());
    }

    private void relayOutcome(String type) {
        await().atMost(10, SECONDS).until(() -> connector.<String>sink("flow-out").received().stream()
                .anyMatch(message -> type.equals(message.getMetadata(CloudEventMetadata.class).orElseThrow().getType())));
        var outcome = connector.<String>sink("flow-out").received().stream()
                .filter(message -> type.equals(message.getMetadata(CloudEventMetadata.class).orElseThrow().getType()))
                .findFirst().orElseThrow();
        connector.<Message<String>>source("flow-out-consumer").send(Message.of(outcome.getPayload(), outcome.getMetadata()));
    }

    public static class RestartProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            String url = System.getenv("TRIP_RESTART_JDBC_URL");
            if (url == null || url.isBlank()) throw new IllegalStateException("TRIP_RESTART_JDBC_URL must point to an isolated disposable PostgreSQL database");
            return Map.of(
                    "quarkus.datasource.devservices.enabled", "false",
                    "quarkus.datasource.jdbc.url", url,
                    "quarkus.datasource.username", "restart",
                    "quarkus.datasource.password", "restart",
                    "quarkus.hibernate-orm.schema-management.strategy", "prepare".equals(System.getProperty("trip.restart.phase")) ? "drop-and-create" : "none");
        }

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(ScriptedAdapter.class);
        }

        @Alternative
        @ApplicationScoped
        public static class ScriptedAdapter extends TripPlannerFlowAdapter {
            final AtomicInteger planningCalls = new AtomicInteger();

            public int planningCalls() {
                return planningCalls.get();
            }

            @Override
            public TripPlan planFromRequest(TripRequest request) {
                planningCalls.incrementAndGet();
                return PLAN;
            }
        }
    }
}
