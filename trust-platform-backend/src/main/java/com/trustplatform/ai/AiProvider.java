package com.trustplatform.ai;

import java.util.Map;

public interface AiProvider {

    /**
     * Generates a professional, emotionally compelling, metrics-grounded markdown report
     * based on active NGO statistics.
     */
    String generateImpactReport(String reportType, Map<String, Object> platformMetrics);

    /**
     * Enhances a drafted story structure, returning styled copy optimized for CMS storytelling.
     */
    String enhanceStory(String content, String styleProfile);

    /**
     * Generates a structural bullet-point operational analysis of campaign anomalies,
     * volunteer saturation points, and donor momentum indexes.
     */
    String summarizeAnalytics(Map<String, Object> platformMetrics);

    /**
     * Computes volunteer matching rationales and compatibility scores.
     */
    String matchVolunteer(Map<String, Object> volunteerProfile, Map<String, Object> eventParameters);
}
