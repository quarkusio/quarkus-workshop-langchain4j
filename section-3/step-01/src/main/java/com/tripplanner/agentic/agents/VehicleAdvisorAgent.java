package com.tripplanner.agentic.agents;

import com.tripplanner.model.BudgetReview;
import com.tripplanner.model.TripPlan;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.SystemMessageProviderSupplier;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.skills.runtime.SkillsToolProvider;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

public interface VehicleAdvisorAgent {

    @UserMessage("""
            You are a vehicle specialist for road trips.
            Based on the trip details below, recommend the most suitable vehicle.
            Consider the destination terrain, trip type, number of travelers, and budget.

            - Destination: {destination}
            - Trip type: {tripType}
            - Number of travelers: {travelers}
            - Budget: {budget}
            - Additional preferences: {preferences}

            Budget review feedback from a previous iteration: {budgetReview}
            If the budget review suggests cost reductions, adjust your recommendation accordingly.
            """)
    @Agent(description = "Recommends the best vehicle for the trip based on destination, travelers, and budget",
           outputKey = "vehicle")
    TripPlan.VehicleRecommendation recommendVehicle(String destination,
                                                    String tripType,
                                                    Integer travelers,
                                                    String budget,
                                                    String preferences,
                                                    BudgetReview budgetReview);

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
