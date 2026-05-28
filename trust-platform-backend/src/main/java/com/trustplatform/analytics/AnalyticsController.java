package com.trustplatform.analytics;

import com.trustplatform.common.api.ApiSuccessResponse;
import com.trustplatform.analytics.dto.AnalyticsSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ANALYTICS')")
    public ResponseEntity<ApiSuccessResponse<AnalyticsSummaryResponse>> getAnalyticsSummary() {
        AnalyticsSummaryResponse summary = analyticsService.getAnalyticsSummary();
        return ResponseEntity.ok(
                ApiSuccessResponse.<AnalyticsSummaryResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Lightweight DTO aggregation metrics fetched successfully")
                        .data(summary)
                        .build()
        );
    }
}
