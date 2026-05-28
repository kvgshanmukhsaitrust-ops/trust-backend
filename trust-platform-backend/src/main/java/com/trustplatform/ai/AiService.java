package com.trustplatform.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    @Value("${ai.provider:gemini}")
    private String configuredProvider;

    private final GeminiProvider geminiProvider;
    private final OpenAiProvider openAiProvider;
    private final LocalHeuristicProvider localHeuristicProvider;

    /**
     * Resolves the active AI provider based on environment setup, key presence, and dynamic fallbacks.
     */
    public AiProvider resolveProvider() {
        String geminiKey = System.getenv("GEMINI_API_KEY");
        String openAiKey = System.getenv("OPENAI_API_KEY");

        if ("gemini".equalsIgnoreCase(configuredProvider)) {
            if (geminiKey != null && !geminiKey.trim().isEmpty()) {
                log.info("[AiService] Google Gemini resolved as active live AI provider.");
                return geminiProvider;
            }
            log.warn("[AiService] Gemini selected but GEMINI_API_KEY is absent. Inspecting OpenAI fallback...");
            if (openAiKey != null && !openAiKey.trim().isEmpty()) {
                log.info("[AiService] OpenAI resolved as active live fallback provider.");
                return openAiProvider;
            }
        } else if ("openai".equalsIgnoreCase(configuredProvider)) {
            if (openAiKey != null && !openAiKey.trim().isEmpty()) {
                log.info("[AiService] OpenAI resolved as active live AI provider.");
                return openAiProvider;
            }
            log.warn("[AiService] OpenAI selected but OPENAI_API_KEY is absent. Inspecting Gemini fallback...");
            if (geminiKey != null && !geminiKey.trim().isEmpty()) {
                log.info("[AiService] Google Gemini resolved as active live fallback provider.");
                return geminiProvider;
            }
        }

        log.info("[AiService] No live API keys detected. Evolving gracefully to LocalHeuristicProvider safe-mode.");
        return localHeuristicProvider;
    }

    public String generateImpactReport(String reportType, Map<String, Object> metrics) {
        try {
            return resolveProvider().generateImpactReport(reportType, metrics);
        } catch (Exception e) {
            log.error("[AiService] Live AI report generation failed. Gracefully falling back to heuristics...", e);
            return localHeuristicProvider.generateImpactReport(reportType, metrics);
        }
    }

    public String enhanceStory(String content, String styleProfile) {
        try {
            return resolveProvider().enhanceStory(content, styleProfile);
        } catch (Exception e) {
            log.error("[AiService] Live AI story enhancement failed. Gracefully falling back to heuristics...", e);
            return localHeuristicProvider.enhanceStory(content, styleProfile);
        }
    }

    public String summarizeAnalytics(Map<String, Object> metrics) {
        try {
            return resolveProvider().summarizeAnalytics(metrics);
        } catch (Exception e) {
            log.error("[AiService] Live AI analytics summary failed. Gracefully falling back to heuristics...", e);
            return localHeuristicProvider.summarizeAnalytics(metrics);
        }
    }

    public String matchVolunteer(Map<String, Object> volunteer, Map<String, Object> event) {
        try {
            return resolveProvider().matchVolunteer(volunteer, event);
        } catch (Exception e) {
            log.error("[AiService] Live AI volunteer matching failed. Gracefully falling back to heuristics...", e);
            return localHeuristicProvider.matchVolunteer(volunteer, event);
        }
    }
}
