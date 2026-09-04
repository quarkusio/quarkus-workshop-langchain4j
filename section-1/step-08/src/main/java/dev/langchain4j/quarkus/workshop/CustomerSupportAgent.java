package dev.langchain4j.quarkus.workshop;

import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox;
import jakarta.enterprise.context.SessionScoped;

import dev.langchain4j.service.SystemMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
import io.smallrye.mutiny.Multi;

@SessionScoped
@RegisterAiService
public interface CustomerSupportAgent {

    @SystemMessage("""
            You are a customer support agent of a car rental company 'Miles of Smiles'.
            You are friendly, polite and concise.
            If the question is unrelated to car rental, you should politely redirect
            the customer to the right department.
            
            When calling tools or functions, strictly use JSON objects,
            do not wrap in quotes or use plain strings.

            When asked to provide details about a reservation, 
            provide weather details and gently try to upsell the customer based on this info.
            
            Today is {current_date}.
            """)
    @ToolBox(BookingRepository.class)
    @McpToolBox("weather")
    Multi<String> chat(String userMessage);
}
