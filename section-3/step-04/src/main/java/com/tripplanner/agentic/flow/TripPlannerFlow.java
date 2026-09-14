package com.tripplanner.agentic.flow;

import static io.quarkiverse.flow.dsl.FlowDSL.*;

import com.tripplanner.model.BookingConfirmation;
import com.tripplanner.model.TripApproval;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripRequest;
import io.quarkiverse.flow.Flow;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.Workflow;
import io.quarkiverse.flow.dsl.FlowWorkflowBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TripPlannerFlow extends Flow {

    @Inject
    TripPlannerFlowAdapter adapter;

    @Override
    public Workflow descriptor() {
        return FlowWorkflowBuilder.workflow("trip-planner-flow")
                .schedule(on(one("com.tripplanner.booking.confirmed").first()))
                .tasks(
                        function("planTrip", (TripRequest req) -> adapter.planFromRequest(req)),

                        emitJson("com.tripplanner.trip.approval.requested", TripPlan.class),

                        listen("waitApproval",
                                toOne(consumed("com.tripplanner.trip.approval.done")
                                        .extensionByInstanceId("flowinstanceid"))),

                        switchWhenOrElse(
                                (TripApproval approval) -> "approved".equals(approval.status()),
                                "finalizeBooking",
                                FlowDirectiveEnum.END,
                                TripApproval.class),

                        function("finalizeBooking", adapter::finalizeBooking),

                        emitJson("com.tripplanner.booking.finalized", BookingConfirmation.class))
                .build();
    }
}
