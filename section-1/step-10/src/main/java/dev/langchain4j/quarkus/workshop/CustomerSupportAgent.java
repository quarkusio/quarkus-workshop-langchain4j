package dev.langchain4j.quarkus.workshop;

import jakarta.enterprise.context.SessionScoped;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import dev.langchain4j.service.SystemMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
import io.quarkiverse.langchain4j.guardrails.InputGuardrails;

@SessionScoped
@RegisterAiService
public interface CustomerSupportAgent {

    @SystemMessage("""
            You are a customer support agent of a car rental company 'Miles of Smiles'.
            You are friendly, polite and concise.
            If the question is unrelated to car rental, you should politely redirect the customer to the right department.
            
            Today is {current_date}.
            """)
    @InputGuardrails(PromptInjectionGuard.class)
    @ToolBox(BookingRepository.class)
    @Timeout(5000)
    @Retry(maxRetries = 3, delay = 100)
    @Fallback(CustomerSupportAgentFallback.class)
    String chat(String userMessage);

    public static class CustomerSupportAgentFallback implements FallbackHandler<String> {

        private static final String EMPTY_RESPONSE = "Failed to get a response from the AI Model. Are you sure it's up and running, and configured correctly?";
        @Override
        public String handle(ExecutionContext context) {
            return EMPTY_RESPONSE;
        }
    
    }
}
