package com.tripplanner.evaluation;

import io.quarkiverse.langchain4j.testing.evaluation.EvaluationAssertions;
import io.quarkiverse.langchain4j.testing.evaluation.SampleLoaderResolver;
import io.quarkiverse.langchain4j.testing.evaluation.Scorer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class TripPlanEvaluationHarnessTest {
    @TempDir Path directory;

    @Test
    void loadsIndependentRequirementsAndChecksEachRequestedDuration() throws Exception {
        var samples = SampleLoaderResolver.load("src/test/resources/evaluation/samples.yaml", String.class);
        assertEquals(3, samples.size());
        var report = new Scorer().evaluate(samples, parameters -> TripPlanText.render(
                TripPlanInvariantStrategyTest.validPlan(Integer.parseInt(parameters.get(2).toString()))),
                new TripPlanInvariantStrategy());
        EvaluationAssertions.assertThat(report).hasAllPassed().hasEvaluationCount(3);
        Path file = directory.resolve("report.json");
        report.saveAs(file, "json", java.util.Map.of("includeDetails", true));
        assertTrue(Files.readString(file).contains("rome-family-seven-days"));
    }

    @Test
    void oneFixedPlanCannotPassRequestsForDifferentDurations() throws Exception {
        var samples = SampleLoaderResolver.load("src/test/resources/evaluation/samples.yaml", String.class);
        var report = new Scorer().evaluate(samples,
                parameters -> TripPlanText.render(TripPlanInvariantStrategyTest.validPlan(1)),
                new TripPlanInvariantStrategy());
        EvaluationAssertions.assertThat(report).hasEvaluationCount(3).hasFailedCount(3);
    }
}
