package com.tripplanner.agentic.flow;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "trip_plan_status")
public class TripPlanEntity extends PanacheEntity {

    @Column(unique = true, nullable = false)
    public String instanceId;

    @Column(nullable = false)
    public String status;

    /** JSON-serialized TripPlan */
    @Column(columnDefinition = "TEXT")
    public String planJson;

    /** JSON-serialized BookingConfirmation, null until confirmed */
    @Column(columnDefinition = "TEXT")
    public String confirmationJson;

    /** JSON-serialized TripRequest — persisted so the UI title survives a restart */
    @Column(columnDefinition = "TEXT")
    public String requestJson;

    public static TripPlanEntity findByInstanceId(String instanceId) {
        return find("instanceId", instanceId).firstResult();
    }

    public static TripPlanEntity findLatestAwaiting() {
        return find("status", "awaiting_approval")
                .firstResult();
    }
}
