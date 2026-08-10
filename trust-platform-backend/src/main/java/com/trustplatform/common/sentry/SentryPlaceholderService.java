package com.trustplatform.common.sentry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Slf4j
@Service
public class SentryPlaceholderService {

    // Patterns for PII scrubbing: PAN card (India), Email address, Aadhaar card (India)
    private static final Pattern PAN_PATTERN = Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("\\b\\d{4}[ -]?\\d{4}[ -]?\\d{4}\\b");

    public void captureException(Throwable throwable) {
        String className = throwable.getClass().getName();
        String message = throwable.getMessage();
        
        String scrubbedMessage = message != null ? scrubPII(message) : "No message available";
        
        log.info("[SENTRY-MOCK] Exception captured successfully. Type: {}, Message: (Scrubbed) [{}]", 
                className, scrubbedMessage);
        
        // In a real Sentry deployment, this method would forward the exception to the Sentry SDK:
        // io.sentry.Sentry.captureException(throwable);
    }

    public String scrubPII(String input) {
        if (input == null) return null;
        String scrubbed = PAN_PATTERN.matcher(input).replaceAll("[REDACTED_PAN]");
        scrubbed = EMAIL_PATTERN.matcher(scrubbed).replaceAll("[REDACTED_EMAIL]");
        scrubbed = AADHAAR_PATTERN.matcher(scrubbed).replaceAll("[REDACTED_AADHAAR]");
        return scrubbed;
    }
}
