package com.trustplatform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "rate-limit.donation.capacity=5" })
@AutoConfigureMockMvc
public class RateLimitingIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void whenExceedingDonationRateLimit_thenReturns429() throws Exception {
        String payload = "{\"amount\": 500, \"donorName\": \"Limit Test\", \"donorEmail\": \"limit@example.com\"}";
        
        // Allowed 5 donation creations per 60 seconds
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/donations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated());
        }
        
        // 6th attempt should fail with 429
        mockMvc.perform(post("/api/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isTooManyRequests());
    }
}
