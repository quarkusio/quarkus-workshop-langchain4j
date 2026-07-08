package com.tripplanner.agentic.agents;

import com.tripplanner.model.ItineraryResult;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.SystemMessageProviderSupplier;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.skills.runtime.SkillsToolProvider;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

public interface ItineraryPlannerAgent {

    @UserMessage("""
            You are an expert trip itinerary planner.
            Create a detailed day-by-day itinerary and a route overview for the trip.
            Include a title, description, and overnight stop for each day.

            - Destination: {destination}
            - Duration: {days} days
            - Trip type: {tripType}
            - Additional preferences: {preferences}
            """)
    @Agent(description = "Creates a detailed day-by-day itinerary and route overview",
           outputKey = "itineraryResult")
    ItineraryResult planItinerary(String destination,
                                  Integer days,
                                  String tripType,
                                  String preferences);

    @SystemMessageProviderSupplier
    static String systemMessageProvider(Object memoryId) {
        Instance<SkillsToolProvider> skillsToolProvider = CDI.current().select(SkillsToolProvider.class);
        if (skillsToolProvider.isResolvable()) {
            return """
                    You have access to the following skills:
                    %s
                    """.formatted(skillsToolProvider.get().getSkills().formatAvailableSkills());
        }
        return "";
    }
}
