package com.trustplatform.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class TurnstileService {

    @Value("${cloudflare.turnstile.secret-key:}")
    private String secretKey;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean verifyToken(String token, String remoteIp) {
        // Dev/test bypass rule
        boolean isDevOrTest = "dev".equalsIgnoreCase(activeProfile) || "test".equalsIgnoreCase(activeProfile);
        boolean isSecretMissingOrDummy = secretKey == null || secretKey.trim().isEmpty() || "dummy".equalsIgnoreCase(secretKey);

        if (isSecretMissingOrDummy) {
            if (isDevOrTest) {
                log.info("[TurnstileService] Turnstile secret key is not configured in dev/test profile. Bypassing validation.");
                return true;
            } else {
                // Production must fail closed if Turnstile secret key is missing!
                log.error("[TurnstileService] Turnstile secret key is missing in production profile! Validation FAILED (closed status).");
                return false;
            }
        }

        if (token == null || token.trim().isEmpty()) {
            log.warn("[TurnstileService] Missing Turnstile token.");
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("secret", secretKey);
            body.add("response", token);
            if (remoteIp != null) {
                body.add("remoteip", remoteIp);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    "https://challenges.cloudflare.com/turnstile/v0/siteverify",
                    request,
                    Map.class
            );

            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                log.info("[TurnstileService] Turnstile token verified successfully.");
                return true;
            }

            log.warn("[TurnstileService] Turnstile validation failed. Response: {}", response);
            return false;
        } catch (Exception e) {
            log.error("[TurnstileService] Error communicating with Cloudflare verify endpoint", e);
            return false;
        }
    }
}
