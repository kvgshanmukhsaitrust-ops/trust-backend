package com.trustplatform.ai;

import com.trustplatform.analytics.AnalyticsService;
import com.trustplatform.analytics.dto.AnalyticsSummaryResponse;
import com.trustplatform.audit.AuditAction;
import com.trustplatform.common.api.ApiSuccessResponse;
import com.trustplatform.exception.BadRequestException;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final AnalyticsService analyticsService;

    @PostMapping("/impact-report")
    @PreAuthorize("hasAuthority('EXPORT_REPORTS')")
    @AuditAction("GENERATE_AI_REPORT")
    public ResponseEntity<ApiSuccessResponse<String>> generateImpactReport(
            @RequestParam(defaultValue = "monthly") String scope) {

        // Deep integration: fetch real platform metrics dynamically
        AnalyticsSummaryResponse summary = analyticsService.getAnalyticsSummary();
        Map<String, Object> metrics = new HashMap<>();
        
        java.math.BigDecimal avgDonation = java.math.BigDecimal.ZERO;
        long totalDonors = 0L;
        if (summary.getDonorAnalytics() != null) {
            if (summary.getDonorAnalytics().getAverageDonationSize() != null) {
                avgDonation = summary.getDonorAnalytics().getAverageDonationSize();
            }
            totalDonors = summary.getDonorAnalytics().getTotalDonorsCount();
        }
        
        metrics.put("totalAmountCollected", avgDonation.multiply(java.math.BigDecimal.valueOf(totalDonors)));
        metrics.put("successfulDonations", totalDonors);
        metrics.put("approvedVolunteers", summary.getTotalVolunteersCount());
        metrics.put("totalEvents", summary.getCampaignAnalytics() != null ? (long) summary.getCampaignAnalytics().size() : 0L);

        String report = aiService.generateImpactReport(scope, metrics);

        return ResponseEntity.ok(
                ApiSuccessResponse.<String>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("AI Impact Report generated successfully")
                        .data(report)
                        .build()
        );
    }

    @PostMapping("/enhance-story")
    @PreAuthorize("hasAuthority('MANAGE_STORIES')")
    @AuditAction("ENHANCE_STORY_AI")
    public ResponseEntity<ApiSuccessResponse<String>> enhanceStory(
            @RequestBody StoryEnhanceRequest request) {

        String input = request.getContent();
        if (input == null || input.length() > 2000) {
            throw new BadRequestException("Input must be between 1 and 2000 characters");
        }

        String enhanced = aiService.enhanceStory(input, request.getStyleProfile());

        return ResponseEntity.ok(
                ApiSuccessResponse.<String>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Story enhanced successfully via AI")
                        .data(enhanced)
                        .build()
        );
    }

    @PostMapping("/summarize-analytics")
    @PreAuthorize("hasAuthority('VIEW_ANALYTICS')")
    @AuditAction("ANALYZE_METRICS_AI")
    public ResponseEntity<ApiSuccessResponse<String>> summarizeAnalytics() {

        AnalyticsSummaryResponse summary = analyticsService.getAnalyticsSummary();
        Map<String, Object> metrics = new HashMap<>();
        
        long totalVolunteers = summary.getTotalVolunteersCount();
        double pendingRatio = summary.getVolunteerPendingRatio();
        long upcomingEvents = 0L;
        if (summary.getCampaignAnalytics() != null) {
            upcomingEvents = summary.getCampaignAnalytics().stream()
                    .filter(c -> c.getDonationCount() > 0).count();
        }
        
        java.math.BigDecimal avgDonation = java.math.BigDecimal.ZERO;
        long totalDonors = 0L;
        if (summary.getDonorAnalytics() != null) {
            if (summary.getDonorAnalytics().getAverageDonationSize() != null) {
                avgDonation = summary.getDonorAnalytics().getAverageDonationSize();
            }
            totalDonors = summary.getDonorAnalytics().getTotalDonorsCount();
        }
        
        metrics.put("totalApplications", totalVolunteers);
        metrics.put("pendingApplications", (long) (totalVolunteers * pendingRatio));
        metrics.put("upcomingEvents", upcomingEvents);
        metrics.put("totalAmountCollected", avgDonation.multiply(java.math.BigDecimal.valueOf(totalDonors)));

        String summaryText = aiService.summarizeAnalytics(metrics);

        return ResponseEntity.ok(
                ApiSuccessResponse.<String>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("AI Operational Summary generated successfully")
                        .data(summaryText)
                        .build()
        );
    }

    @PostMapping("/match-volunteers")
    @PreAuthorize("hasAuthority('MANAGE_MEMBERS')")
    @AuditAction("MATCH_VOLUNTEERS_AI")
    public ResponseEntity<ApiSuccessResponse<String>> matchVolunteer(
            @RequestBody VolunteerMatchRequest request) {

        Map<String, Object> volunteer = new HashMap<>();
        volunteer.put("skills", request.getVolunteerSkills());
        volunteer.put("experience", request.getVolunteerExperience());

        Map<String, Object> event = new HashMap<>();
        event.put("title", request.getEventTitle());
        event.put("skillsNeeded", request.getEventSkillsNeeded());

        String matchResult = aiService.matchVolunteer(volunteer, event);

        return ResponseEntity.ok(
                ApiSuccessResponse.<String>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Volunteer compatibility analyzed successfully")
                        .data(matchResult)
                        .build()
        );
    }

    // --- Inner Helper Request DTOs ---

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoryEnhanceRequest {
        private String content;
        private String styleProfile;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolunteerMatchRequest {
        private String volunteerSkills;
        private String volunteerExperience;
        private String eventTitle;
        private String eventSkillsNeeded;
    }
}
