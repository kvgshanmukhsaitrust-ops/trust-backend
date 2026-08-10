package com.trustplatform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
public class TurnstileSecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void whenSubmittingDonationWithoutTurnstileTokenInDevMode_thenSucceedsDueToDevBypass() throws Exception {
        // In dev profile (activeProfile defaults to dev/test), bypass works if key is dummy/missing
        String payload = "{\"amount\": 100, \"donorName\": \"Turnstile Test\", \"donorEmail\": \"turnstile@example.com\"}";

        mockMvc.perform(post("/api/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }
}
