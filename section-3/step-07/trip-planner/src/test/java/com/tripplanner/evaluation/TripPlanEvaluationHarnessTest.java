package com.tripplanner.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkiverse.langchain4j.testing.evaluation.EvaluationAssertions;
import io.quarkiverse.langchain4j.testing.evaluation.EvaluationReport;
import io.quarkiverse.langchain4j.testing.evaluation.SampleLoaderResolver;
import io.quarkiverse.langchain4j.testing.evaluation.Samples;
import io.quarkiverse.langchain4j.testing.evaluation.Scorer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The scorer, sample loading, and report assertions, exercised offline against
 * the real samples file with the deterministic invariant strategy. Proves the
 * repeatable-experiment machinery (same inputs -> comparable per-case results,
 * saveable report) without any model or container.
 */
class TripPlanEvaluationHarnessTest {

    private static final String SAMPLES = "src/test/resources/evaluation/samples.yaml";

    /** A structurally valid plan text; passes the invariant for any request. */
    private static final String KNOWN_GOOD =
            "Vehicle: Family MPV (Room for two travelers and luggage)\n"
            + "Route: Coastal route through the region\n"
            + "Day 1: Arrival\nDay 2: Family day\nDay 3: Departure\n"
            + "Cost total: 310\n";

    /** A plan with a gap in the itinerary; fails the invariant for any request. */
    private static final String KNOWN_BAD =
            "Vehicle: Family MPV (Room for two travelers and luggage)\n"
            + "Route: Coastal route through the region\n"
            + "Day 1: Arrival\n"
            + "Day 3: Departure\n"
            + "Cost total: 310\n";

    @Test
    void loadsTheSampledTripRequests() throws IOException {
        Samples<String> samples = SampleLoaderResolver.load(SAMPLES, String.class);

        assertEquals(2, samples.size());
        assertEquals("rome-family-three-days", samples.get(0).name());
        // The request parameters are preserved so a repeated run is comparable.
        assertEquals("Rome", samples.get(0).parameters().get(0));
        assertTrue(samples.get(0).tags().contains("smoke"));
    }

    @Test
    void aKnownGoodSavedOutputPassesEverySample() throws IOException {
        Samples<String> samples = SampleLoaderResolver.load(SAMPLES, String.class);

        EvaluationReport<String> report = new Scorer().evaluate(
                samples, parameters -> KNOWN_GOOD, new TripPlanInvariantStrategy());

        EvaluationAssertions.assertThat(report)
                .hasAllPassed()
                .hasScore(100.0)
                .hasEvaluationCount(2);
    }

    @Test
    void aBrokenSavedOutputFailsAndIsReportedPerCase() throws IOException {
        Samples<String> samples = SampleLoaderResolver.load(SAMPLES, String.class);

        EvaluationReport<String> report = new Scorer().evaluate(
                samples, parameters -> KNOWN_BAD, new TripPlanInvariantStrategy());

        EvaluationAssertions.assertThat(report)
                .hasEvaluationCount(2)
                .hasFailedCount(2);
        // Per-case detail names the violation, so the report is diagnostic.
        String details = report.evaluations().stream()
                .findFirst().orElseThrow().explanation();
        assertTrue(details.contains("day 2"), "per-case explanation should name the missing day: " + details);
    }

    @Test
    void theReportSavesAsJsonWithPerCaseDetails() throws IOException {
        Samples<String> samples = SampleLoaderResolver.load(SAMPLES, String.class);

        EvaluationReport<String> report = new Scorer().evaluate(
                samples, parameters -> KNOWN_GOOD, new TripPlanInvariantStrategy());

        Path out = Files.createTempDirectory("trip-plan-eval").resolve("report.json");
        report.saveAs(out, "json", java.util.Map.of("includeDetails", true));
        assertTrue(Files.exists(out));
        assertTrue(Files.size(out) > 0, "saved report must not be empty");
    }
}
