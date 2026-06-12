package com.carmanagement.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Simple service for managing human input requests in the Human-in-the-Loop pattern.
 * Stores pending approval requests and completes them when humans provide input via UI.
 */
@ApplicationScoped
public class HumanInputService {

    private final Map<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
    private final Map<String, String> pendingPrompts = new ConcurrentHashMap<>();

    /**
     * Request human input and return a CompletableFuture that completes when input is provided.
     * 
     * @param requestId Unique identifier for this request (e.g., "car-123")
     * @param prompt The prompt/question to show to the human
     * @return CompletableFuture that completes with the human's response
     */
    public CompletableFuture<String> requestInput(String requestId, String prompt) {
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        pendingPrompts.put(requestId, prompt);
        return future;
    }

    /**
     * Provide human input to complete a pending request.
     * 
     * @param requestId The request ID
     * @param input The human's input/decision
     */
    public void provideInput(String requestId, String input) {
        CompletableFuture<String> future = pendingRequests.remove(requestId);
        pendingPrompts.remove(requestId);
        if (future != null) {
            future.complete(input);
        }
    }

    /**
     * Get all pending requests (for UI display).
     * 
     * @return Map of requestId to prompt
     */
    public Map<String, String> getPendingRequests() {
        return Map.copyOf(pendingPrompts);
    }

    /**
     * Check if a request is pending.
     */
    public boolean hasPendingRequest(String requestId) {
        return pendingRequests.containsKey(requestId);
    }

    /**
     * Cancel a pending request.
     */
    public void cancelRequest(String requestId) {
        CompletableFuture<String> future = pendingRequests.remove(requestId);
        pendingPrompts.remove(requestId);
        if (future != null) {
            future.cancel(true);
        }
    }
}


