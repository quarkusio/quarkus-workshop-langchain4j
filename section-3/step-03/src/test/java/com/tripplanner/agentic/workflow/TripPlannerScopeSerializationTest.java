package com.tripplanner.agentic.workflow;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.tripplanner.model.TripPlan;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Exercises the real agentic scope serialization path: TripPlannerSystem (a @SequenceAgent)
 * creates a child workflow via quarkus-flow-langchain4j, which serializes the AgenticScope.
 * If scope serialization loses parameters, WireMock stubs won't match (they require actual
 * parameter values in the LLM prompt), causing the test to fail.
 *
 * @see <a href="https://github.com/quarkusio/quarkus-workshop-langchain4j/pull/376#issuecomment-5091522868">Bug report</a>
 */
@QuarkusTest
@QuarkusTestResource(value = TripPlannerScopeSerializationTest.OpenAiMock.class, restrictToAnnotatedClass = true)
class TripPlannerScopeSerializationTest {

    @Inject
    TripPlannerSystem tripPlannerSystem;

    @Test
    @DisplayName("planTrip preserves String and Integer parameters through scope serialization")
    void planTrip_preserves_all_parameters_through_scope_serialization() {
        TripPlan plan = tripPlannerSystem.planTrip(
                "California Coast",
                "2026-08-15",
                5,
                "family",
                4,
                "$3000",
                "beach and scenic drives");

        assertNotNull(plan, "TripPlan must not be null");

        assertNotNull(plan.vehicle(), "Vehicle recommendation must not be null");
        assertEquals("SUV", plan.vehicle().type());
        assertEquals("Ford Explorer", plan.vehicle().model());

        assertNotNull(plan.itinerary(), "Itinerary must not be null");
        assertEquals(5, plan.itinerary().size(),
                "Itinerary should have 5 days, matching the days=5 parameter");

        assertNotNull(plan.routeOverview(), "Route overview must not be null");
        assertFalse(plan.routeOverview().isBlank());

        assertNotNull(plan.costs(), "Cost estimate must not be null");
        assertNotNull(plan.costs().total());

        assertNotNull(plan.tips(), "Tips must not be null");
        assertFalse(plan.tips().isEmpty());
    }

    public static class OpenAiMock implements QuarkusTestResourceLifecycleManager {

        private WireMockServer wireMock;

        @Override
        public Map<String, String> start() {
            wireMock = new WireMockServer(options()
                    .dynamicPort()
                    .notifier(new ConsoleNotifier(true)));
            wireMock.start();

            stubVehicleAdvisorAgent();
            stubItineraryPlannerAgent();
            stubCostEstimatorAgent();

            return Map.of("quarkus.langchain4j.openai.base-url", wireMock.baseUrl() + "/v1");
        }

        @Override
        public void stop() {
            if (wireMock != null) {
                wireMock.stop();
            }
        }

        private void stubVehicleAdvisorAgent() {
            wireMock.stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                    .withRequestBody(containing("vehicle specialist"))
                    .withRequestBody(containing("California Coast"))
                    .withRequestBody(containing("4"))
                    .withRequestBody(containing("family"))
                    .withRequestBody(containing("$3000"))
                    .atPriority(1)
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(openAiResponse(VEHICLE_JSON))));
        }

        private void stubItineraryPlannerAgent() {
            wireMock.stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                    .withRequestBody(containing("itinerary planner"))
                    .withRequestBody(containing("California Coast"))
                    .withRequestBody(containing("5"))
                    .withRequestBody(containing("2026-08-15"))
                    .withRequestBody(containing("family"))
                    .atPriority(2)
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(openAiResponse(ITINERARY_JSON))));
        }

        private void stubCostEstimatorAgent() {
            wireMock.stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                    .withRequestBody(containing("cost estimation"))
                    .withRequestBody(containing("$3000"))
                    .atPriority(3)
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(openAiResponse(COST_ESTIMATE_JSON))));
        }

        private static final String VEHICLE_JSON =
                "{\"type\":\"SUV\",\"model\":\"Ford Explorer\","
                        + "\"reasoning\":\"Spacious SUV ideal for a family of 4 on California coastal roads\"}";

        private static final String ITINERARY_JSON =
                "{\"routeOverview\":\"California Coast scenic drive from LA to San Francisco\","
                        + "\"itinerary\":["
                        + "{\"day\":1,\"title\":\"LA to Santa Barbara\",\"description\":\"Drive along PCH with beach stops\",\"overnightStop\":\"Santa Barbara\"},"
                        + "{\"day\":2,\"title\":\"Santa Barbara to Big Sur\",\"description\":\"Scenic coastal drive\",\"overnightStop\":\"Big Sur\"},"
                        + "{\"day\":3,\"title\":\"Big Sur to Monterey\",\"description\":\"Explore Monterey Bay\",\"overnightStop\":\"Monterey\"},"
                        + "{\"day\":4,\"title\":\"Monterey to Santa Cruz\",\"description\":\"Beach day and boardwalk\",\"overnightStop\":\"Santa Cruz\"},"
                        + "{\"day\":5,\"title\":\"Santa Cruz to San Francisco\",\"description\":\"Final coastal drive\",\"overnightStop\":\"San Francisco\"}"
                        + "],\"tips\":[\"Book coastal hotels early\",\"PCH can be foggy in the morning\",\"Pack sunscreen\"]}";

        private static final String COST_ESTIMATE_JSON =
                "{\"vehiclePerDay\":\"$95/day\",\"fuel\":\"$180\",\"tolls\":\"$45\","
                        + "\"accommodation\":\"$200/night\",\"food\":\"$80/day\","
                        + "\"activities\":\"$60/day\",\"total\":\"$2575\"}";

        private static String openAiResponse(String jsonContent) {
            String escaped = jsonContent.replace("\"", "\\\"");
            return """
                    {
                      "id": "chatcmpl-test",
                      "object": "chat.completion",
                      "created": 1234567890,
                      "model": "gpt-4o",
                      "choices": [{
                        "index": 0,
                        "message": {
                          "role": "assistant",
                          "content": "%s"
                        },
                        "finish_reason": "stop"
                      }],
                      "usage": {
                        "prompt_tokens": 100,
                        "completion_tokens": 50,
                        "total_tokens": 150
                      }
                    }
                    """.formatted(escaped);
        }
    }
}
