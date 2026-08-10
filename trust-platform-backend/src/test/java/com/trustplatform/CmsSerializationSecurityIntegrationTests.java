package com.trustplatform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
public class CmsSerializationSecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin@example.com", authorities = {"MANAGE_SETTINGS"})
    public void whenSavingValidJsonMilestones_thenBypassesSanitizerAndSucceeds() throws Exception {
        String milestones = "[{\"date\":\"2015\",\"event\":\"Founding\"}]";

        mockMvc.perform(put("/api/admin/pages/HISTORY_MILESTONES")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(milestones))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(milestones));

        mockMvc.perform(get("/api/admin/pages/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.HISTORY_MILESTONES").value(milestones));
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = {"MANAGE_SETTINGS"})
    public void whenSavingMalformedJsonMilestones_thenReturnsBadRequest() throws Exception {
        String malformed = "[{\"date\":\"2015\""; // Unclosed brackets

        mockMvc.perform(put("/api/admin/pages/HISTORY_MILESTONES")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(malformed))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = {"MANAGE_SETTINGS"})
    public void whenSavingHtmlFields_thenRunsSanitizer() throws Exception {
        String html = "<p>Intro Text</p><script>alert(1)</script>";

        // Script tag should be stripped by HtmlSanitizer
        mockMvc.perform(put("/api/admin/pages/HISTORY_INTRO")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(html))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("<p>Intro Text</p>"));
    }
}
