package com.tripplanner.guardrails;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.agentic.tools.RentalPricingTool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.service.tool.ToolExecutor;
import io.quarkiverse.langchain4j.runtime.ToolsRecorder;
import io.quarkiverse.langchain4j.runtime.tool.QuarkusToolExecutor;
import io.quarkiverse.langchain4j.runtime.tool.QuarkusToolExecutorFactory;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class RentalPricingGuardrailTest {

    @Inject
    QuarkusToolExecutorFactory executorFactory;

    @Inject
    ObjectMapper objectMapper;

    CountingPricingTool tool;
    ToolExecutor executor;

    @BeforeEach
    void setUp() {
        tool = new CountingPricingTool();
        var metadata = ToolsRecorder.getMetadata().get(RentalPricingTool.class.getName()).stream()
                .filter(method -> method.toolSpecification().name().equals("estimateRental"))
                .findFirst().orElseThrow();
        assertTrue(metadata.getInputGuardrails().hasGuardrails());

        // Use the original tool metadata so Quarkus applies the real annotation and guardrail.
        executor = executorFactory.create(new QuarkusToolExecutor.Context(
                tool, metadata.invokerClassName(), metadata.methodName(), metadata.argumentMapperClassName(),
                metadata.executionModel(), metadata.returnBehavior(), false, metadata));
    }

    @ParameterizedTest
    @CsvSource({"compact,1,45,45", "estate,5,65,325", "suv,5,80,400", "mpv,30,90,2700"})
    void validArgumentsReachTheCalculation(String category, int days, int dailyRate, int rentalTotal) throws Exception {
        var request = ToolExecutionRequest.builder().id("valid").name("estimateRental")
                .arguments("{\"category\":\"" + category + "\",\"days\":" + days + "}").build();

        var result = executor.executeWithContext(request, InvocationContext.builder().chatMemoryId("pricing-test").build());

        assertFalse(result.isError());
        assertEquals(1, tool.calls);
        var estimate = objectMapper.readTree(result.resultText());
        assertEquals(category, estimate.path("category").asText());
        assertEquals(days, estimate.path("days").asInt());
        assertEquals("EUR", estimate.path("currency").asText());
        assertEquals(dailyRate, estimate.path("dailyRate").asInt());
        assertEquals(rentalTotal, estimate.path("rentalTotal").asInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"category\":\"spaceship\",\"days\":5}",
            "{\"category\":\"SUV\",\"days\":5}",
            "{\"category\":\"suv\",\"days\":0}",
            "{\"category\":\"suv\",\"days\":-1}",
            "{\"category\":\"suv\",\"days\":31}",
            "{\"category\":\"suv\",\"days\":1.5}",
            "{\"category\":\"suv\",\"days\":4294967297}",
            "{\"category\":\"suv\",\"days\":\"5\"}",
            "{\"category\":\"suv\",\"days\":true}",
            "{\"category\":\"suv\",\"days\":null}",
            "{\"category\":\"suv\"}",
            "{\"days\":5}",
            "{\"category\":null,\"days\":5}",
            "{\"category\":42,\"days\":5}",
            "{\"category\":\"suv\",\"days\":5} {}",
            "{}", "[]", "null", "not json", ""
    })
    void invalidArgumentsNeverReachTheCalculation(String arguments) {
        var request = ToolExecutionRequest.builder().id("invalid").name("estimateRental")
                .arguments(arguments).build();

        var result = executor.executeWithContext(request, InvocationContext.builder().chatMemoryId("pricing-test").build());

        assertTrue(result.isError());
        assertTrue(result.resultText().startsWith("Input validation failed:"));
        assertEquals(0, tool.calls, "Rejected arguments must not enter the tool body");
    }

    @Vetoed
    public static class CountingPricingTool extends RentalPricingTool {
        int calls;

        @Override
        public RentalEstimate estimateRental(String category, int days) {
            calls++;
            return super.estimateRental(category, days);
        }
    }
}
