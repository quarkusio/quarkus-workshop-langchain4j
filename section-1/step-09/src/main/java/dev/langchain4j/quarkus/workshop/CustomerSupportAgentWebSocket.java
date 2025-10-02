package dev.langchain4j.quarkus.workshop;

import dev.langchain4j.guardrail.InputGuardrailException;
import io.quarkus.logging.Log;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/customer-support-agent")
public class CustomerSupportAgentWebSocket {

    private final CustomerSupportAgent customerSupportAgent;

    public CustomerSupportAgentWebSocket(CustomerSupportAgent customerSupportAgent) {
        this.customerSupportAgent = customerSupportAgent;
    }

    @OnOpen
    public String onOpen() {
        return "Welcome to Miles of Smiles! How can I help you today?";
    }

    @OnTextMessage
    public String onTextMessage(String message) {
        try {
            return customerSupportAgent.chat(message);
        } catch (InputGuardrailException e) {
            Log.errorf(e, "Error calling the LLM: %s", e.getMessage());
            return "Sorry, I am unable to process your request at the moment. It's not something I'm allowed to do.";
        } catch (Exception e) {
            Log.errorf(e, "Error calling the LLM: %s", e.getMessage());
            return "I ran into some problems. Please try again.";
        }
    }
}
