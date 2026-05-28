package com.trustplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustplatform.ai.AiController;
import com.trustplatform.ai.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AiLayerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AiService aiService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "regular@user.org", authorities = {"READ_CONTENT"})
    public void whenUnauthorizedUserRequestsAiReport_thenReturns403Forbidden() throws Exception {
        // regular user lacks EXPORT_REPORTS authority
        mockMvc.perform(post("/api/admin/ai/impact-report")
                        .param("scope", "monthly")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "director@trust.org", authorities = {"EXPORT_REPORTS", "VIEW_ANALYTICS"})
    public void whenAuthorizedUserGeneratesReport_thenReturns200AndValidHeuristicFallback() throws Exception {
        // Fallback Heuristics verification under absent API keys
        mockMvc.perform(post("/api/admin/ai/impact-report")
                        .param("scope", "monthly")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("KVG Trust — Monthly Synthesis Report")))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("Financial Integrity & Transparency Indicators")));
    }

    @Test
    @WithMockUser(username = "writer@trust.org", authorities = {"MANAGE_STORIES"})
    public void whenStoryEnhanced_thenReturnsHtmlProposalsCorrectly() throws Exception {
        AiController.StoryEnhanceRequest request = new AiController.StoryEnhanceRequest();
        request.setContent("<p>We distributed food kits in the local neighborhood.</p>");
        request.setStyleProfile("immersive");

        mockMvc.perform(post("/api/admin/ai/enhance-story")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("<blockquote")))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("resilience and collaborative solidarity")));
    }

    @Test
    @WithMockUser(username = "director@trust.org", authorities = {"VIEW_ANALYTICS"})
    public void whenAnalyticsSummarized_thenReturnsBulletedInsights() throws Exception {
        mockMvc.perform(post("/api/admin/ai/summarize-analytics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("✨ Director's Executive Insights Summary")))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("Media Density Integration")));
    }

    @Test
    @WithMockUser(username = "manager@trust.org", authorities = {"MANAGE_MEMBERS"})
    public void whenVolunteerMatched_thenReturnsCompatibilityScoreAndBulletPoints() throws Exception {
        AiController.VolunteerMatchRequest request = new AiController.VolunteerMatchRequest();
        request.setVolunteerSkills("Nursing, Pediatric care, English");
        request.setVolunteerExperience("2 years first aid responder at local clinics.");
        request.setEventTitle("Free Medical Camp 2026");
        request.setEventSkillsNeeded("Medical assistance, Patient coordination");

        mockMvc.perform(post("/api/admin/ai/match-volunteers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("score")))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("Clinical Alignment")));
    }
}
