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
@Component("openAiProvider")
public class OpenAiProvider implements AiProvider {

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String generateImpactReport(String reportType, Map<String, Object> metrics) {
        log.info("[OpenAiProvider] Generating live impact report via OpenAI...");
        
        BigDecimal totalFunds = getBigDecimalMetric(metrics, "totalAmountCollected", BigDecimal.ZERO);
        long successfulDonations = getLongMetric(metrics, "successfulDonations", 0L);
        long approvedVolunteers = getLongMetric(metrics, "approvedVolunteers", 0L);
        long totalEvents = getLongMetric(metrics, "totalEvents", 0L);

        String prompt = "You are a professional copywriter and operational analyst for KVG Trust NGO. " +
                "Generate a professional, emotionally compelling, metrics-grounded markdown impact report of type '" + reportType + "'. " +
                "Ground your report directly in these actual verified database statistics: " +
                "Total Funds Collected: ₹" + totalFunds + ", Successful Transactions: " + successfulDonations + ", Vetted Volunteers mobilized: " + approvedVolunteers + ", Active campaigns: " + totalEvents + ". " +
                "Write structured sections (Executive Summary, Financial Transparency, and Campaigns outcomes), using bold metrics and clean bullet points. " +
                "Avoid generic AI-generated fluff or repetitive summaries. Cite actual metrics internally.";

        return callOpenAi(prompt);
    }

    @Override
    public String enhanceStory(String content, String styleProfile) {
        log.info("[OpenAiProvider] Enhancing story text via OpenAI...");

        String prompt = "You are an expert copywriter inside an NGO CMS. " +
                "Enhance the following story draft into highly polished, engaging, HTML copy. " +
                "Format the response using semantic HTML paragraphs (<p>) and a blockquote (<blockquote>). " +
                "Do not return complete HTML documents, only content fragments. " +
                "Enhance using style profile: '" + styleProfile + "'. " +
                "Draft text to enhance: \"" + content + "\"";

        return callOpenAi(prompt);
    }

    @Override
    public String summarizeAnalytics(Map<String, Object> metrics) {
        log.info("[OpenAiProvider] Tracing operational insights summary via OpenAI...");

        long totalApps = getLongMetric(metrics, "totalApplications", 0L);
        long pendingApps = getLongMetric(metrics, "pendingApplications", 0L);
        long upcomingEvents = getLongMetric(metrics, "upcomingEvents", 0L);
        BigDecimal totalFunds = getBigDecimalMetric(metrics, "totalAmountCollected", BigDecimal.ZERO);

        String prompt = "You are a director of operational intelligence for KVG Trust. " +
                "Generate a short, high-value, bulleted 'Executive Intelligence Insights Summary' analyzing these real-time metrics: " +
                "Total volunteer applications: " + totalApps + ", Pending applications: " + pendingApps + ", Upcoming events: " + upcomingEvents + ", Total capital collections: ₹" + totalFunds + ". " +
                "Focus on anomalies, capacity utilization bottlenecks, donor velocity, and media coverage relationships. " +
                "Provide exactly 3-4 highly relevant operational insights in bullet points. Be concise and actionable.";

        return callOpenAi(prompt);
    }

    @Override
    public String matchVolunteer(Map<String, Object> volunteer, Map<String, Object> event) {
        log.info("[OpenAiProvider] Formulating smart matching analysis via OpenAI...");

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

        return callOpenAi(prompt);
    }

    private String callOpenAi(String prompt) {
        String key = (apiKey != null && !apiKey.trim().isEmpty()) ? apiKey : System.getenv("OPENAI_API_KEY");
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }

        String url = "https://api.openai.com/v1/chat/completions";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(key);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", "gpt-4o-mini");
            payload.put("messages", List.of(message));
            payload.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("choices")) {
                List choices = (List) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map choice = (Map) choices.get(0);
                    Map responseMessage = (Map) choice.get("message");
                    return (String) responseMessage.get("content");
                }
            }
            throw new RuntimeException("Malformed response payload from OpenAI API");
        } catch (Exception e) {
            log.error("[OpenAiProvider] OpenAI API request execution failed", e);
            throw new RuntimeException("OpenAI API failed: " + e.getMessage(), e);
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
        log.info("[OpenAiProvider] Executing live case summarization via GPT...");
        String prompt = String.format(
                "You are an expert NGO Case Investigator. Summarize this assistance application to help the review committee.\n" +
                "Case Title: %s\n" +
                "Category: %s\n" +
                "Details: %s\n" +
                "Provide a professional bulleted markdown summary containing the core request, urgency level (low/medium/high/critical), and key items to verify.",
                title, category, description
        );
        return callOpenAi(prompt);
    }
}
