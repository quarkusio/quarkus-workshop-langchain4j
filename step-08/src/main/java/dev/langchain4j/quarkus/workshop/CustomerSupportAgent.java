package dev.langchain4j.quarkus.workshop;

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
            If the question is unrelated to car rental, you should politely redirect the customer to the right department.
            
            You will get the location tied to a booking from the database. Get the coordinates for that location,
            and based on the coordinates, get the weather for that specific location. You should provide information
            about specific equipment the car rental booking might need based on the weather, such as snow chains or
            air conditioning.
            
            Today is {current_date}.
            """)
    @ToolBox(BookingRepository.class)
    String chat(String userMessage);
}
