package com.tripplanner.agentic.agents;

import com.tripplanner.model.TripPlan;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.SystemMessageProviderSupplier;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.skills.runtime.SkillsToolProvider;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

public interface TripPlannerAgent {

    @UserMessage("""
            Plan a trip with the following details:
            - Destination: {destination}
            - Duration: {days} days
            - Trip type: {tripType}
            - Number of travelers: {travelers}
            - Budget: {budget}
            - Additional preferences: {preferences}
            """)
    @Agent("Plans road trips based on customer preferences and trip type")
    TripPlan planTrip(String destination,
                      Integer days,
                      String tripType,
                      Integer travelers,
                      String budget,
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
