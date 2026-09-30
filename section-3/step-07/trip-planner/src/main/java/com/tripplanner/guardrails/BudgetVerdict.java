package com.tripplanner.guardrails;

/**
 * Structured verdict returned by the LLM-based budget guardrail judge.
 *
 * @param appropriate {@code true} if the vehicle is a reasonable choice for the given budget tier
 * @param reason      a short explanation of the verdict, used for logging and UI display
 */
public record BudgetVerdict(boolean appropriate, String reason) {}
