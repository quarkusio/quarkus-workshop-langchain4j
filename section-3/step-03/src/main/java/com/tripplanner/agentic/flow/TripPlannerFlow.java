package com.tripplanner.agentic.flow;

import static io.quarkiverse.flow.dsl.FlowDSL.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.model.PlanningRequest;
import com.tripplanner.model.TripApproval;
import com.tripplanner.model.TripPlanStatus;
import io.quarkiverse.flow.Flow;
import io.cloudevents.CloudEvent;
import io.serverlessworkflow.impl.WorkflowContextData;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.Workflow;
import io.quarkiverse.flow.dsl.FlowWorkflowBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;
import java.io.IOException;

@ApplicationScoped
public class TripPlannerFlow extends Flow {

    @Inject
    TripPlannerFlowAdapter adapter;

    @Inject
    TripPlanStore store;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public Workflow descriptor() {
        return FlowWorkflowBuilder.workflow("trip-planner-flow")
                .schedule(on(one("com.tripplanner.trip.requested").first()))
                .tasks(
                        withInstanceId("planTrip", this::plan, PlanningRequest.class),
                        switchWhenOrElse((TripPlanStatus result) -> "failed".equals(result.status()),
                                "publishFailure", "requestApproval", TripPlanStatus.class),
                        emitJson("requestApproval", "com.tripplanner.trip.approval.requested", TripPlanStatus.class),
                        listen("waitApproval",
                                toOne(consumed("com.tripplanner.trip.approval.done")
                                        .envelope(this::matchesDecision))),
                        withInstanceId("resolveDecision", this::resolveDecision, TripApproval.class),
                        switchCase(
                                caseOf((TripPlanStatus result) -> "confirmed".equals(result.status()), TripPlanStatus.class)
                                        .then("publishConfirmation"),
                                caseOf((TripPlanStatus result) -> "rejected".equals(result.status()), TripPlanStatus.class)
                                        .then("publishRejection"),
                                caseDefault("publishFailure")),
                        emitJson("publishConfirmation", "com.tripplanner.booking.finalized", TripPlanStatus.class)
                                .then(FlowDirectiveEnum.END),
                        emitJson("publishRejection", "com.tripplanner.trip.rejected", TripPlanStatus.class)
                                .then(FlowDirectiveEnum.END),
                        emitJson("publishFailure", "com.tripplanner.trip.failed", TripPlanStatus.class))
                .build();
    }

    private TripPlanStatus plan(String instanceId, PlanningRequest input) {
        TripPlanStatus status = store.bind(input, instanceId);
        try {
            return status.outcome("awaiting_approval", Objects.requireNonNull(adapter.planFromRequest(status.request())), null);
        } catch (Exception failure) {
            return status.failed(failure);
        }
    }

    private TripPlanStatus resolveDecision(String instanceId, TripApproval approval) {
        TripPlanStatus status = store.byInstanceId(instanceId);
        try {
            if (!store.matchesDecision(instanceId, approval)) {
                throw new IllegalStateException("Decision does not match the pending workflow");
            }
            if ("rejected".equals(approval.status())) return status.outcome("rejected", status.plan(), null);
            if (!"approved".equals(approval.status())) throw new IllegalArgumentException("Invalid decision");
            return status.outcome("confirmed", status.plan(), Objects.requireNonNull(adapter.finalizeBooking(approval)));
        } catch (Exception failure) {
            return status.failed(failure);
        }
    }

    private boolean matchesDecision(CloudEvent event, WorkflowContextData context) {
        // One envelope predicate keeps both checks: dataAs() would replace an existing envelope predicate.
        String instanceId = context.instanceData().id();
        if (!instanceId.equals(event.getExtension("flowinstanceid")) || event.getData() == null) return false;
        try {
            return store.matchesDecision(instanceId, objectMapper.readValue(event.getData().toBytes(), TripApproval.class));
        } catch (IOException malformed) {
            return false;
        }
    }
}
