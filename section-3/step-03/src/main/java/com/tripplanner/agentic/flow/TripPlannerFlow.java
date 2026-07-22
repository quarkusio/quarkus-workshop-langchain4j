package com.tripplanner.agentic.flow;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.*;

import com.tripplanner.model.BookingConfirmation;
import com.tripplanner.model.TripApproval;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripRequest;
import io.quarkiverse.flow.Flow;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.function.Function;

@ApplicationScoped
public class TripPlannerFlow extends Flow {

    @Inject
    TripPlannerFlowAdapter adapter;

    @Override
    public Workflow descriptor() {
        return FuncWorkflowBuilder.workflow("trip-planner-flow")
                .schedule(s -> s.on(events ->
                        events.one(f -> f.with(p -> p.type("com.tripplanner.booking.confirmed")))))
                .tasks(
                        set(".[0].data"),

                        function("planTrip",
                                (Function<TripRequest, TripPlan>) req -> adapter.planFromRequest(req),
                                TripRequest.class),

                        emitJson("com.tripplanner.trip.approval.requested", TripPlan.class),

                        listen("waitApproval",
                                toOne(consumed("com.tripplanner.trip.approval.done")
                                        .extensionByInstanceId("flowinstanceid"))),

                        switchWhenOrElse(
                                (TripApproval approval) -> "approved".equals(approval.status()),
                                "finalizeBooking",
                                FlowDirectiveEnum.END,
                                TripApproval.class),

                        function("finalizeBooking",
                                (Function<TripApproval, BookingConfirmation>) adapter::finalizeBooking,
                                TripApproval.class),

                        emitJson("com.tripplanner.booking.finalized", BookingConfirmation.class))
                .build();
    }
}
