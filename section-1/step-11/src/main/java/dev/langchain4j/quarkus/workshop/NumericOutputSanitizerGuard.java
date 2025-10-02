package dev.langchain4j.quarkus.workshop;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NumericOutputSanitizerGuard implements OutputGuardrail {

    @Inject
    Logger logger;

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        String llmResponse = responseFromLLM.text();

        try {
            double number = Double.parseDouble(llmResponse);
            return successWith(llmResponse, number);
        } catch (NumberFormatException e) {
            // ignore
        }

        logger.debugf("LLM output for expected numeric result: %s", llmResponse);

        String extractedNumber = extractNumber(llmResponse);
        if (extractedNumber != null) {
            logger.infof("Extracted number: %s", extractedNumber);
            try {
                double number = Double.parseDouble(extractedNumber);
                return successWith(extractedNumber, number);
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        return failure("Unable to extract a number from LLM response: " + llmResponse);
    }

    private String extractNumber(String text) {
        int lastDigitPosition = text.length()-1;
        while (lastDigitPosition >= 0) {
            if (Character.isDigit(text.charAt(lastDigitPosition))) {
                break;
            }
            lastDigitPosition--;
        }
        if (lastDigitPosition < 0) {
            return null;
        }
        int numberBegin = lastDigitPosition;
        while (numberBegin >= 0) {
            if (!Character.isDigit(text.charAt(numberBegin)) && text.charAt(numberBegin) != '.') {
                break;
            }
            numberBegin--;
        }
        return text.substring(numberBegin+1, lastDigitPosition+1);
    }
}
