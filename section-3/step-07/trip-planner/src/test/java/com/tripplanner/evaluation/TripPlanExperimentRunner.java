package com.tripplanner.evaluation;

import com.tripplanner.agentic.workflow.TripPlannerSystem;
import com.tripplanner.evaluation.LangfuseDatasetSampleLoader.DatasetSample;
import com.tripplanner.model.TripPlan;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkiverse.langchain4j.testing.evaluation.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

/**
 * Runs one dataset item through the planner inside a root span that Langfuse
 * records as an experiment item. The {@code langfuse.experiment.*} attributes
 * link the trace to the dataset item, and the evaluation rule set up in dev
 * mode scores every experiment item of {@code trip-plan-samples}.
 */
@ApplicationScoped
public class TripPlanExperimentRunner {

    public record Result(TripPlan plan, String output, String traceId) {
    }

    @Inject
    TripPlannerSystem tripPlannerSystem;

    @ConfigProperty(name = "quarkus.langchain4j.openai.chat-model.model-name")
    String modelName;

    @WithSpan("trip-plan-evaluation")
    public Result run(String experimentName, DatasetSample item) {
        Parameters p = item.sample().parameters();
        TripPlan plan = tripPlannerSystem.planTrip(p.get(0), p.get(1), p.get(2), p.get(3),
                p.get(4), p.get(5), p.get(6));
        String output = TripPlanText.render(plan);

        Span span = Span.current();
        span.setAttribute("langfuse.experiment.id", experimentName);
        span.setAttribute("langfuse.experiment.name", experimentName);
        span.setAttribute("langfuse.experiment.dataset.id", item.datasetId());
        span.setAttribute("langfuse.experiment.item.id", item.itemId());
        span.setAttribute("langfuse.experiment.item.root_observation_id", span.getSpanContext().getSpanId());
        span.setAttribute("langfuse.experiment.item.expected_output", item.sample().expectedOutput());
        span.setAttribute("langfuse.observation.input", item.inputJson());
        span.setAttribute("langfuse.observation.output", output);
        span.setAttribute(AttributeKey.stringArrayKey("langfuse.trace.tags"), List.of("evaluation"));
        span.setAttribute("langfuse.trace.metadata.sample_id", item.itemId());
        span.setAttribute("langfuse.trace.metadata.model", modelName);

        return new Result(plan, output, span.getSpanContext().getTraceId());
    }
}
