package com.tripplanner.agentic.tools;

import com.tripplanner.guardrails.RentalEstimateInputGuardrail;
import dev.langchain4j.agent.tool.Tool;
import io.quarkiverse.langchain4j.guardrails.ToolInputGuardrails;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.Map;

@ApplicationScoped
public class RentalPricingTool {

    // Fictional workshop rates in whole euros per day, not live rental prices.
    public static final Map<String, Integer> DAILY_RATES = Map.of(
            "compact", 45, "estate", 65, "suv", 80, "mpv", 90);

    private static final Logger LOG = Logger.getLogger(RentalPricingTool.class);

    @Tool("Calculate a rental estimate in EUR for the given vehicle category and number of rental days.")
    @ToolInputGuardrails(RentalEstimateInputGuardrail.class)
    public RentalEstimate estimateRental(String category, int days) {
        int dailyRate = DAILY_RATES.get(category);
        int rentalTotal = dailyRate * days;
        LOG.infof("Rental calculation executed: category=%s, days=%d, total=%d EUR", category, days, rentalTotal);
        return new RentalEstimate(category, days, "EUR", dailyRate, rentalTotal);
    }

    public record RentalEstimate(String category, int days, String currency, int dailyRate, int rentalTotal) {}
}
