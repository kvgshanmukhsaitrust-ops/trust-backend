package com.trustplatform.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Component("geminiProvider")
public class GeminiProvider implements AiProvider {

    @Value("${ai.gemini.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String generateImpactReport(String reportType, Map<String, Object> metrics) {
        log.info("[GeminiProvider] Generating live impact report via Google Gemini...");
        
        BigDecimal totalFunds = getBigDecimalMetric(metrics, "totalAmountCollected", BigDecimal.ZERO);
        long successfulDonations = getLongMetric(metrics, "successfulDonations", 0L);
        long approvedVolunteers = getLongMetric(metrics, "approvedVolunteers", 0L);
        long totalEvents = getLongMetric(metrics, "totalEvents", 0L);

        String prompt = "You are a professional, highly skilled copywriter and operational analyst for an NGO called KVG Trust. " +
                "Generate a professional, emotionally compelling, metrics-grounded markdown impact report of type '" + reportType + "'. " +
                "Strictly ground your analysis and figures in these actual verified database statistics: " +
                "1. Total Funds Collected: ₹" + totalFunds + " " +
                "2. Successful Transactions: " + successfulDonations + " " +
                "3. Vetted & Approved Volunteers mobilized: " + approvedVolunteers + " " +
                "4. Active campaigns and events: " + totalEvents + ". " +
                "Write structured sections (Executive Summary, Financial Transparency, and Campaigns outcomes), using bold metrics, clean bullet points, and high-fidelity GFM markdown. " +
                "Avoid generic AI-generated fluff or repetitive summaries. Cite actual metrics internally. Do not hallucinate statistics.";

        return callGemini(prompt);
    }

    @Override
    public String enhanceStory(String content, String styleProfile) {
        log.info("[GeminiProvider] Enhancing story text via Google Gemini...");

        String prompt = "You are an expert copywriter inside a premium NGO CMS. " +
                "Enhance the following story draft draft into highly polished, engaging, HTML copy. " +
                "Format the response using semantic HTML paragraphs (<p>) and an inspiring, beautiful blockquote (<blockquote>). " +
                "Do not return complete HTML documents (<html>/<body>), only content fragments. " +
                "Enhance the text using this style profile: '" + styleProfile + "'. " +
                "Draft text to enhance: \"" + content + "\"";

        return callGemini(prompt);
    }

    @Override
    public String summarizeAnalytics(Map<String, Object> metrics) {
        log.info("[GeminiProvider] Tracing operational insights summary via Google Gemini...");

        long totalApps = getLongMetric(metrics, "totalApplications", 0L);
        long pendingApps = getLongMetric(metrics, "pendingApplications", 0L);
        long upcomingEvents = getLongMetric(metrics, "upcomingEvents", 0L);
        BigDecimal totalFunds = getBigDecimalMetric(metrics, "totalAmountCollected", BigDecimal.ZERO);

        String prompt = "You are a director of operational intelligence for KVG Trust NGO. " +
                "Generate a short, high-value, bulleted 'Executive Intelligence Insights Summary' analyzing these real-time metrics: " +
                "Total volunteer applications: " + totalApps + ", Pending applications: " + pendingApps + ", Upcoming events: " + upcomingEvents + ", Total capital collections: ₹" + totalFunds + ". " +
                "Focus on anomalies, capacity utilization bottlenecks, donor velocity, and media coverage relationships. " +
                "Provide exactly 3-4 highly relevant operational insights in bullet points. Be concise and actionable.";

        return callGemini(prompt);
    }

    @Override
    public String matchVolunteer(Map<String, Object> volunteer, Map<String, Object> event) {
        log.info("[GeminiProvider] Formulating smart matching analysis via Google Gemini...");

        String skills = (String) volunteer.getOrDefault("skills", "");
        String experience = (String) volunteer.getOrDefault("experience", "");
        String eventTitle = (String) event.getOrDefault("title", "Initiative");
        String eventSkills = (String) event.getOrDefault("skillsNeeded", "");

        String prompt = "You are a volunteer coordinator for KVG Trust. " +
                "Evaluate the match between this volunteer and this community event initiative. " +
                "Volunteer Skills: '" + skills + "', Experience: '" + experience + "'. " +
                "Event Title: '" + eventTitle + "', Skills Required: '" + eventSkills + "'. " +
                "Return a raw, structural JSON block in exactly this format with NO markdown wrapper blocks (no ```json): " +
                "{\n" +
                "  \"score\": <integer percentage between 30 and 99>,\n" +
                "  \"reasoning\": \"<2-3 concise bullet points explaining alignment or gaps>\"\n" +
                "}";

        return callGemini(prompt);
    }

    private String callGemini(String prompt) {
        String key = (apiKey != null && !apiKey.trim().isEmpty()) ? apiKey : System.getenv("GEMINI_API_KEY");
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + key;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Construct payload using standard Maps
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);

            Map<String, Object> partsContainer = new HashMap<>();
            partsContainer.put("parts", List.of(textPart));

            Map<String, Object> contentsContainer = new HashMap<>();
            contentsContainer.put("contents", List.of(partsContainer));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(contentsContainer, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("candidates")) {
                List candidates = (List) responseBody.get("candidates");
                if (!candidates.isEmpty()) {
                    Map candidate = (Map) candidates.get(0);
                    Map content = (Map) candidate.get("content");
                    List parts = (List) content.get("parts");
                    if (!parts.isEmpty()) {
                        Map part = (Map) parts.get(0);
                        return (String) part.get("text");
                    }
                }
            }
            throw new RuntimeException("Malformed response payload from Gemini API");
        } catch (Exception e) {
            log.error("[GeminiProvider] Google Gemini API request execution failed", e);
            throw new RuntimeException("Gemini API failed: " + e.getMessage(), e);
        }
    }

    private BigDecimal getBigDecimalMetric(Map<String, Object> metrics, String key, BigDecimal defaultValue) {
        if (metrics == null) return defaultValue;
        Object val = metrics.get(key);
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        if (val instanceof Number) {
            return BigDecimal.valueOf(((Number) val).doubleValue());
        }
        return defaultValue;
    }

    private long getLongMetric(Map<String, Object> metrics, String key, long defaultValue) {
        if (metrics == null) return defaultValue;
        Object val = metrics.get(key);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return defaultValue;
    }

    @Override
    public String summarizeCase(String title, String description, String category) {
        log.info("[GeminiProvider] Executing live case summarization via Gemini...");
        String prompt = String.format(
                "You are an expert NGO Case Investigator. Summarize this assistance application to help the review committee.\n" +
                "Case Title: %s\n" +
                "Category: %s\n" +
                "Details: %s\n" +
                "Provide a professional bulleted markdown summary containing the core request, urgency level (low/medium/high/critical), and key items to verify.",
                title, category, description
        );
        return callGemini(prompt);
    }
}
