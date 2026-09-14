package com.tripplanner.chat;

import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TripPlannerTools {

    @Tool("Get information about available vehicle types and their features")
    public String getVehicleInfo(String vehicleType) {
        // Simple placeholder - Step 04 can enhance this if needed
        return switch (vehicleType.toLowerCase()) {
            case "suv" -> "SUVs offer spacious interiors, suitable for 5-7 passengers, good for families and long trips.";
            case "sedan" -> "Sedans are economical, comfortable for 4-5 passengers, fuel-efficient for road trips.";
            case "convertible" -> "Convertibles provide an open-air experience, best for 2-4 passengers, ideal for coastal routes.";
            case "van" -> "Vans offer maximum space for 7-9 passengers, perfect for large families or groups.";
            default -> "We offer SUVs, sedans, convertibles, and vans. What type interests you?";
        };
    }

    @Tool("Get typical budget ranges for different trip types")
    public String getBudgetInfo(String tripType, int days) {
        String base = switch (tripType.toLowerCase()) {
            case "economy" -> "€50-70 per day for vehicle, modest accommodation, and basic meals";
            case "moderate" -> "€100-150 per day for comfortable vehicle, mid-range hotels, and dining";
            case "comfortable" -> "€200-300 per day for premium vehicle, quality hotels, and activities";
            case "luxury" -> "€500+ per day for luxury vehicle, high-end accommodation, and experiences";
            default -> "€100-300 per day depending on comfort level";
        };
        return String.format("For a %d-day trip: %s. Total estimate: €%d-€%d",
                days, base, days * 50, days * 500);
    }
}
