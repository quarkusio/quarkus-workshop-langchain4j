package com.tripplanner.agentic.agents;

import dev.langchain4j.agentic.declarative.McpClientAgent;
import dev.langchain4j.agentic.declarative.McpClientSupplier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class McpAgentTest {

    @Test
    void weatherAgentDeclaresCorrectMcpTool() {
        assertTrue(WeatherAgent.class.isInterface());

        Method fetchWeather = Arrays.stream(WeatherAgent.class.getMethods())
                .filter(m -> m.isAnnotationPresent(McpClientAgent.class))
                .findFirst().orElseThrow();

        McpClientAgent ann = fetchWeather.getAnnotation(McpClientAgent.class);
        assertEquals("getWeatherForecast", ann.toolName());
        assertEquals("weather", ann.outputKey());

        assertTrue(Arrays.stream(WeatherAgent.class.getMethods())
                .anyMatch(m -> m.isAnnotationPresent(McpClientSupplier.class)));
    }

    @Test
    void pointsOfInterestAgentDeclaresCorrectMcpTool() {
        assertTrue(PointsOfInterestAgent.class.isInterface());

        Method fetchPoi = Arrays.stream(PointsOfInterestAgent.class.getMethods())
                .filter(m -> m.isAnnotationPresent(McpClientAgent.class))
                .findFirst().orElseThrow();

        McpClientAgent ann = fetchPoi.getAnnotation(McpClientAgent.class);
        assertEquals("getPointsOfInterest", ann.toolName());
        assertEquals("pointsOfInterest", ann.outputKey());

        assertTrue(Arrays.stream(PointsOfInterestAgent.class.getMethods())
                .anyMatch(m -> m.isAnnotationPresent(McpClientSupplier.class)));
    }
}
