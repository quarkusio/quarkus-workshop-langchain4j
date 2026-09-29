package com.tripplanner.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "trip_plan_status", indexes = @Index(name = "trip_status_idx", columnList = "status"))
public class TripPlanEntity extends PanacheEntityBase {
    // No per-JVM ID blocks: registration order must remain stable across application instances.
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trip_plan_sequence")
    @SequenceGenerator(name = "trip_plan_sequence", sequenceName = "trip_plan_sequence", allocationSize = 1)
    public Long id;

    @Column(unique = true, nullable = false)
    public String requestId;

    @Column(unique = true)
    public String instanceId;

    @Column(nullable = false)
    public String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    public TripRequest request;

    @JdbcTypeCode(SqlTypes.JSON)
    public TripPlan plan;

    @JdbcTypeCode(SqlTypes.JSON)
    public BookingConfirmation confirmation;

    @JdbcTypeCode(SqlTypes.JSON)
    public TripApproval acceptedDecision;

    public String error;

    @Column(columnDefinition = "TEXT")
    public String message;
}
