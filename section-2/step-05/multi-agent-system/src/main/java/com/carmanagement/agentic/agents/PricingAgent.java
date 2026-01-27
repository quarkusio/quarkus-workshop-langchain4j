package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that estimates the market value of a vehicle.
 * Used by the supervisor to make disposition decisions.
 */
public interface PricingAgent {

    @SystemMessage("""
        You are a vehicle pricing specialist with expertise in market valuations.
        
        Use these pricing guidelines:
        
        Brand Base Values (2024 models):
        - Luxury brands (Mercedes, BMW, Audi, Lexus): $45,000-$65,000
        - Premium brands (Volvo, Acura, Infiniti): $35,000-$50,000
        - Mainstream brands (Toyota, Honda, Ford, Chevrolet): $25,000-$40,000
        - Economy brands (Kia, Hyundai, Nissan): $20,000-$35,000
        
        Depreciation:
        - Apply 15% per year for the first 3 years
        - Apply 10% per year for years 4-6
        - Apply 5% per year for years 7+
        
        Condition Adjustments:
        - Excellent: +10% to base value
        - Good: No adjustment
        - Fair: -15% from base value
        - Poor: -30% from base value
        
        Provide:
        1. Estimated market value (single dollar amount)
        2. Brief justification (2-3 sentences)
        
        Format your response as:
        Estimated Value: $XX,XXX
        Justification: [Your reasoning]
        """)
    @UserMessage("""
        Estimate the current market value of this vehicle:
        - Make: {carMake}
        - Model: {carModel}
        - Year: {carYear}
        - Condition: {carCondition}
        """)
    @Agent(
        outputKey = "carValue",
        description = "Pricing specialist that estimates vehicle market value based on make, model, year, and condition"
    )
    String estimateValue(String carMake, String carModel, Integer carYear, String carCondition);
}

