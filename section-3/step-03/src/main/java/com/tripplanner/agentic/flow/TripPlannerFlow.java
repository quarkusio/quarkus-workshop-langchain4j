package com.tripplanner.agentic.flow;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.*;

import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripRequest;
import io.quarkiverse.flow.Flow;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowContextData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collection;

@ApplicationScoped
public class TripPlannerFlow extends Flow {

    @Inject
    TripPlannerFlowAdapter adapter;

    @Override
    public Workflow descriptor() {
        return FuncWorkflowBuilder.workflow("trip-planner-flow")
                .tasks(
                        listen("waitBooking", toOne("com.tripplanner.booking.confirmed"))
                                .outputAs((Collection<Object> c) -> c.iterator().next()),

                        function("planTrip", adapter::planFromRequest, TripRequest.class),

                        emitJson("com.tripplanner.trip.approval.requested", TripPlan.class),

                        listen("waitApproval",
                                toOne(consumed("com.tripplanner.trip.approval.done")
                                        .extensionByInstanceId("flowinstanceid")))
                                .outputAs((Collection<Object> c) -> c.iterator().next())
                                .exportAs((Object approval, WorkflowContextData wf) ->
                                        wf.context().asJavaObject(), Object.class),

                        emitJson("com.tripplanner.trip.confirmed", TripPlan.class))
                .build();
    }
}
