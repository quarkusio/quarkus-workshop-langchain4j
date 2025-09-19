package dev.langchain4j.quarkus.workshop;

import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox;
import jakarta.enterprise.context.SessionScoped;

import dev.langchain4j.service.SystemMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;

@SessionScoped
@RegisterAiService
public interface CustomerSupportAgent {

    @SystemMessage("""
            You are a customer support agent of a car rental company 'Miles of Smiles'.
            You are friendly, polite and concise.
            If the question is unrelated to car rental, you should politely redirect 
            the customer to the right department.
            
            You will get the location and start dates for a booking from the booking table 
            in the database.
            Figure out the coordinates for that location,
            and based on the coordinates and the date, 
            call a tool to get the weather for that specific location on the given date.
            You should provide information about specific equipment 
            the car rental booking might need based on the weather, 
            such as snow chains or air conditioning.
            
            Today is {current_date}.
            """)
    @ToolBox(BookingRepository.class)
    @McpToolBox("weather")
    String chat(String userMessage);
}
